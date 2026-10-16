# ReelVoice — Handoff (updated 2026-10-16, coordinated status)

## Latest coordinated status

- Deep bhai's latest Android changes are present on `main` in commit `613ef12`.
- The Lab/DEV tab has been removed from the Android UI.
- The refreshed iOS-style adaptive app icon has been added to the Android launcher.
- These UI/branding changes are complete; do not re-introduce the Lab/DEV surface while adding the next features.
- Subscription is **not production-ready yet**: the client-side validation stub remains and must be replaced with server-confirmed entitlements.
- Video entry points exist in the app, but the complete device E2E flow and failure/retry behavior are still pending.

## Next coordinated flow

0. **Contract first:** `BILLING_CONTRACT.md` rev 2 is on branch `feature/reelvoice-product-plan` and open for approval. It defines the Free / Pro Monthly / Pro Annual matrix, audio-video entitlements, server validation, restore, expiry, and cancellation, with **no hardcoded pricing**. Billing implementation starts only after this contract is approved.
1. Keep the Android UI cleanup limited to the remaining design polish and consistent audio/video feature presentation.
2. Finish the billing contract and server validation before exposing paid video/background limits.
3. Complete Quick Clean, Extract Voice, Reel V1/V2, Video Processing, and retry/offline E2E checks.
4. Audit the iOS build/review result and reuse the approved new logo asset for the follow-up update.
5. Prepare the ReelVoice landing page with matching audio/video positioning, pricing, and store/deep links.

The landing page and iOS update should consume the same feature and entitlement names defined by the billing contract; avoid maintaining separate pricing or capability definitions.

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

## ⚠️ CRITICAL FINDING — RESOLVED: API domain migrated (2026-10-15 evening)

- `shadowselfwork.com` is **not registered** (Verisign RDAP 404; NXDOMAIN from 8.8.8.8/1.1.1.1 and on the VPS itself). All three Kotlin clients had hardcoded `https://shadowselfwork.com/voice/` — every network feature of any shipped build was failing.
- **Fixed by migrating to `voice.tranquilwaters.in`:** user added DNS A record → `77.237.234.45`; certbot issued Let's Encrypt cert (live); nginx vhost `/etc/nginx/sites-enabled/voice.tranquilwaters.in` serves `/voice/` + `/media/` with the same rate-limit zones; `ALLOWED_HOSTS` in `/var/www/simplelms/backend/.env` includes the new host.
- **All 3 client baseUrls now `https://voice.tranquilwaters.in/voice/`** (`VoiceApiPhase2Client.kt`, `VoiceCleaningApi.kt`, `AuthApi.kt`) — commit `e5bd72d`, pushed to `main`.
- Legacy `shadowselfwork` nginx vhost left in place (harmless); can be removed later. Old domain is dead — do not reference it anywhere.

## ✅ DONE — P0 #3: First mobile-to-server E2E round (2026-10-15)

Verified end-to-end from the emulator against the NEW domain:

- **Health + backgrounds**: app fetched `availableModes=[basic, standard, studio, pro]` and all 9 backgrounds (incl. bhakti/cricket/confession/study) — server access logs show 200s from Dalvik UA.
- **Live recording → upload → add_background**: recorded on-device (service foreground), picked the file via Add-Vibe flow with Bhakti background, `POST /voice/add_background/` → job 120 → `pending → completed` → output downloaded via `/voice/cleaned_output/120/` → playback verified (AudioTrack: full 5.0 s = 240,000 frames @48k).
- **Crash fixed during testing**: `AudioRecorder` OOM'd on longer recordings (`ByteArrayOutputStream` buffering whole session in RAM, 33 MB alloc fail at ~5.5 min mark in emulator test). PCM now streams to temp files and WAV is written streamed (`writeWavFromPcmTemp`) — commit `e5bd72d`.
- Test artifacts cleaned: 44-byte broken WAVs from the pre-fix crash deleted from emulator.

Still to verify (next session): Quick Clean, Extract Voice, Reel V1/V2 polling, Video Processing, network failure/retry path — per `MOBILE_E2E_TEST_PLAN.md`.

## 🔲 P0 #3 remaining: Mobile-to-server E2E (next session)

Test plan: `MOBILE_E2E_TEST_PLAN.md`. Verified so far: health, backgrounds, add_background (server-side curl **and full emulator flow** incl. playback). Still to verify from the device: quick clean, extract voice, reel V1/V2 polling, video processing, live recording + upload to other flows, network failure/retry. Note: emulator `sdcard/Download` picked files fine; Play-files picker showed "No items" — real device testing will differ.

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
- [x] Full emulator E2E evidence: add_background flow incl. recording, upload, polling, download, playback (server logs + AudioTrack evidence).
- [ ] Remaining E2E flows (quick clean, extract, reel V1/V2, video) from device.
- [ ] Billing cannot unlock Pro without server confirmation (stub still present).
- [x] `./gradlew :app:assembleDebug` passes.
- [x] API domain reachable over HTTPS (`voice.tranquilwaters.in`), cert valid.

## Emulator test-session notes (for anyone resuming)

- `emulator-5554` (API 35 arm64) was used; app installed via `adb install -r app/build/outputs/apk/debug/app-debug.apk`.
- `POST_NOTIFICATIONS` + `RECORD_AUDIO` granted via `adb shell pm grant`.
- UI coordinates shift between screens — use `uiautomator dump` + text search, not hardcoded taps.
- File picker: system Files app → Show roots → Downloads → pick WAV; in-app "BROWSE FILES IN OTHER APPS" opens the same Files UI.
- Fresh installed app starts at quota 20/20; each session decrements only after successful flows.

## Server quick-reference

- SSH: `ssh root@77.237.234.45`; voice backend: `/var/www/simplelms/backend`; nginx vhosts `selfshadowwork` (legacy domain) + `voice.tranquilwaters.in` (new); services `voice-gunicorn`, `voice-celery`; logs `/var/log/voice-gunicorn.{access,error}.log`, `/var/log/voice-celery.log`.
- Full map: `SERVER_FLOW.md` (mostly still accurate; domain caveat above).
