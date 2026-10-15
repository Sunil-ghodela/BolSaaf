# ReelVoice — Handoff (updated 2026-10-15)

## Objective

Make ReelVoice production-ready: real background recording, truthful background modes with **real server assets**, closed mobile-to-server E2E gap, and server-validated billing. Preserve current UI direction.

## Repository and current state

- Repository: `BolSaaf` (GitHub `Sunil-ghodela/BolSaaf`), branch `main`, package `com.reelvoice`
- Android app builds with `./gradlew :app:assembleDebug` (needs `local.properties` → `sdk.dir=$HOME/Library/Android/sdk`)
- Smoke-test emulator: `emulator-5554` was used live on 2026-10-15
- Pre-existing local changes (`app/build.gradle.kts` version bump vc8→10 / 1.1.4, gradle wrapper 8.7→8.13, untracked `upload_certificate.pem`) — **preserved, not discarded**. Certificate stays uncommitted on purpose.

## ✅ DONE — P0 #1: Foreground background recording (2026-10-15)

- **`app/src/main/java/com/reelvoice/RecordingService.kt`** (new): foreground service owning `AudioRecord` + `AudioProcessor`. Mic-type FGS, persistent notification with Stop action, `START_REDELIVER_INTENT` + consumed-intent guard (no re-record over finished WAVs), result reporting via shared `StateFlow`/`SharedFlow` (companion), idempotent resource release on every exit path.
- **`MainActivity.kt`**: `startRecording()` starts the service; `stopRecording()` stops via service; `onDestroy()` no longer kills recording; completion handled in `handleRecordingFinished()` from service results (recreation-safe, replay guarded by `lastHandledTimestamp`); `POST_NOTIFICATIONS` requested on API 33+.
- **Manifest**: `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MICROPHONE`, `POST_NOTIFICATIONS` permissions; `.RecordingService` declared with `foregroundServiceType="microphone"`.
- **Emulator-verified**: service ran foreground (`types=0x80` microphone), survived HOME backgrounding, stop from UI flushed 5.1 MB `original_`+`cleaned_` WAVs, no crash, clean service exit.

## ✅ DONE — P0 #2: Real background assets on server (2026-10-15)

Four mode backgrounds now exist **as real server assets** — no fallback needed for preferred IDs:

- `bhakti.mp3` (temple bells + tanpura-like drone), `cricket.mp3` (stadium crowd), `confession.mp3` (soft intimate ambience), `study.mp3` (lo-fi focus chords) — all 60 s loops, synthesized with ffmpeg (script: `scripts/gen_background_loops.sh`), plus 20 s previews each.
- Server `BG_MAP` updated in **both** `services/background_mix.py` and `services/reel_mode.py`; catalog view `/voice/backgrounds/` now lists 9 backgrounds with per-mode default volumes (bhakti 0.20, cricket 0.25, confession 0.12, study 0.18).
- `/add_background/` validation now derives allowed `bg` ids from `BG_MAP` (unknown IDs can no longer be silently accepted) — was a hardcoded 5-id set.
- **E2E proof on prod**: `add_background` jobs for `bhakti` (job 118) and `cricket` (job 119) went pending→completed, mixed output verified audible via volumedetect.
- Client `applyModePreset()` now adopts the server's `default_volume` when the preferred background exists (`MainActivity.kt`). Honest fallback labeling remains as a safety net only.
- Server patches applied via `scripts/patch_server_backgrounds.py` + `scripts/patch_server_bg_validation.py`; originals backed up on VPS at `/tmp/bak_background_mix.py`, `/tmp/bak_reel_mode.py`, `/tmp/bak_views.py`. `voice-gunicorn` + `voice-celery` restarted after patches.

## ⚠️ CRITICAL FINDING — API domain has no DNS (2026-10-15)

- `shadowselfwork.com` is **not registered** (Verisign RDAP 404; NXDOMAIN from 8.8.8.8/1.1.1.1 and on the VPS itself). All three Kotlin clients hardcode `https://shadowselfwork.com/voice/` (`VoiceApiPhase2Client.kt`, `VoiceCleaningApi.kt`, `AuthApi.kt`). **Every network feature of any shipped build is failing** unless something else resolves the name.
- The server stack itself is healthy at `root@77.237.234.45` (nginx vhost `selfshadowwork`, `voice-gunicorn` unix socket, Celery worker).
- **Migration prep done on server:** new vhost `/etc/nginx/sites-enabled/voice.tranquilwaters.in` mirrors the voice config (status/upload/media splits, rate-limit zones); `ALLOWED_HOSTS` in `/var/www/simplelms/backend/.env` now includes `voice.tranquilwaters.in`. Health + backgrounds return 200 against that Host locally.
- **Needed to finish (user decision):**
  1. Add DNS A record `voice.tranquilwaters.in → 77.237.234.45` at the `tranquilwaters.in` DNS provider (apex already resolves fine), OR re-register `shadowselfwork.com` at a registrar.
  2. Then run `certbot --nginx -d voice.tranquilwaters.in` (needs DNS live first).
  3. Then swap the three client `baseUrl`s to `https://voice.tranquilwaters.in/voice/`, rebuild, smoke test.

## 🔲 P0 #3: Mobile-to-server E2E (pending)

Test plan: `MOBILE_E2E_TEST_PLAN.md`. Verified so far: health, backgrounds, add_background (server-side curl). Still to verify from the device: quick clean, extract voice, reel V1/V2 polling, video processing, live recording + upload, network failure/retry. Also confirm the emulator/device can resolve the final API host (DNS must exist first — see above).

## 🔲 Billing (before production revenue)

`MainActivity.validatePurchaseWithServer()` is still the local stub setting `isProMember = true`. Implement `BILLING_CONTRACT.md` server validation + `/voice/auth/me/` refresh. Never unlock Pro client-only.

## 🔲 Other pending

- Cancellation + recovery for stuck async jobs; session expiry/quota/offline states.
- Instrumentation/UI tests: recording, upload, polling, playback.
- Accessibility labels; mode-unavailable states (mostly satisfied by real assets now).
- Reel V2 three-output preview/share/download on a real device.
- Backend hygiene: dead view code review, `ALLOWED_HOSTS` final pass before `DEBUG=False` (CORS lists in `.env` may also need the new domain).
- `status/119`-style job rows from testing exist in prod DB — harmless; delete if desired.

## Acceptance criteria status

- [x] Recording survives backgrounding; stops cleanly from UI/notification action.
- [x] No use-after-release/leak paths in service lifecycle (idempotent release, join-before-release kept from `AudioRecorder`).
- [x] Every visible mode maps to a real server background asset; unknown IDs rejected server-side.
- [x] BG mix affects real mixed output (server-verified at selected volume).
- [ ] Full device E2E evidence in client + server logs (blocked on DNS fix).
- [ ] Billing cannot unlock Pro without server confirmation (stub still present).
- [x] `./gradlew :app:assembleDebug` passes.

## Server quick-reference

- SSH: `ssh root@77.237.234.45`; voice backend: `/var/www/simplelms/backend`; nginx vhosts `selfshadowwork` (legacy domain) + `voice.tranquilwaters.in` (new); services `voice-gunicorn`, `voice-celery`; logs `/var/log/voice-gunicorn.{access,error}.log`, `/var/log/voice-celery.log`.
- Full map: `SERVER_FLOW.md` (mostly still accurate; domain caveat above).
