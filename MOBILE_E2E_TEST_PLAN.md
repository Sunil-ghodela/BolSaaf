# Mobile → Server End-to-End Verification (Task #18)

Run this once per release to prove that the Android client successfully drives every `/voice/` flow on production. Until 2026-04-14 the server log showed zero `/voice/clean/` hits for days even though the app reported "not working" — we don't yet know if that was due to the server-side bugs (now fixed) or a client-side regression. This plan closes the loop.

## Prerequisites

- A connected Android device (USB + USB debugging on) OR an emulator.
- `adb` on PATH.
- Debug APK: `./gradlew :app:assembleDebug` → `app/build/outputs/apk/debug/app-debug.apk`.
- A test audio file on the device (any voice memo works).
- A second terminal with SSH to the production host.

## Setup

```bash
# Terminal 1 — install the fresh debug build
adb devices
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Terminal 1 — clear + tail logcat filtered to our tags
adb logcat -c
adb logcat -v time VoiceCleaningApi:V VoiceApiPhase2Client:V AudioRecorder:V AudioProcessor:V MainActivity:V *:E
```

```bash
# Terminal 2 — watch the server as requests come in
ssh root@77.237.234.45 'tail -F /var/log/voice-gunicorn.access.log /var/log/voice-celery.log'
```

## Tests

For each test, correlate the client logcat timestamp with the matching `/voice/access.log` entry. Timestamps within a few seconds of each other = request reached the server.

### T1. Health check on app launch

- **Action:** Open the app.
- **Expect logcat:** `VoiceCleaningApi: health modes=[basic, standard, studio, pro] ffmpeg=true deepfilter=true cuda=false` (or similar).
- **Expect server log:** `GET /voice/health/ HTTP/1.1" 200`.
- **Pass if:** client receives the v2.2.0 payload with `demucs: true`.

### T2. Quick clean (sync path, `/voice/clean/`)

- **Action:** Home screen → "Quick" mode → pick a short voice memo → Clean.
- **Expect logcat:** `VoiceCleaningApi: poll status=completed` OR (more likely) an immediate 200 with `cleaned_url`.
- **Expect server log:** `POST /voice/clean/ HTTP/1.1" 200` followed by a `GET /voice/cleaned_output/<id>/ HTTP/1.1" 200`.
- **Pass if:** The comparison screen opens and both "Original" and "Cleaned" play without error.
- **Why this matters:** Prior to 2026-04-14 the `/voice/cleaned_output/<id>/` route 404'd and the download route 500'd (pk/job_id mismatch). Both are now fixed; this test guards against regression.

### T3. Reel / extract / background mix (async path, Celery-backed)

- **Action:** Home screen → "Reel ★" (or Extract / BG as applicable) → pick a file → submit.
- **Expect logcat:** `VoiceApiPhase2Client: job accepted id=<N>` then periodic `status=processing` then `status=completed`.
- **Expect server logs (both):**
  - `voice-gunicorn.access.log`: `POST /voice/reel/ HTTP/1.1" 202` + repeating `GET /voice/status/<N>/ HTTP/1.1" 200`.
  - `voice-celery.log`: `Task voice.reel[<uuid>] received` then a few seconds later `Task voice.reel[<uuid>] succeeded in <T>s`.
- **Pass if:** job transitions through `pending → processing → completed` and output plays.
- **Why this matters:** This is the path that was silently no-op'ing before (#19 scaffold-stub fix) and had no queue backbone (#17 Celery migration).

### T4. Live recording (on-device RNNoise)

- **Action:** Live screen → grant mic permission → record ~20s of speech with background noise → stop.
- **Expect logcat:** no `use-after-release` crashes; `AudioRecorder` logs a clean stop.
- **Pass if:** Playback of the cleaned file sounds denoised (AGC/NS off + RNNoise on).
- **Edge check:** If logcat emits `stopRecording: record thread still alive after 10s join`, flag a regression — the stop timeout was just bumped from 5s to 10s.

### T5. Failure path

- **Action:** Kill wifi mid-upload.
- **Pass if:** UI surfaces a readable error (not a silent hang).

## Rollback trigger

If T2 or T3 fails with the server reachable, capture:
```bash
adb logcat -d > /tmp/logcat_$(date +%s).txt
ssh root@77.237.234.45 'tail -200 /var/log/voice-gunicorn.error.log /var/log/voice-celery.log' > /tmp/server_$(date +%s).txt
```
Emergency rollback instructions live at the bottom of `SERVER_FLOW.md` §9.

## What "passes" means for closing #18

- T1–T5 all green
- Server logs show **matching** timestamps for every client event
- If any async pipeline stays `pending` > 2 minutes with Celery idle, that's a dispatch bug — re-open #17.
