# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## ⏩ Start here — current state (read first)

> Last updated: 2026-04-15.

**Where we are**: v1.0.3 (versionCode 4) **queued for Play Store first review** (India, full rollout). Package `com.bolsaaf` is registered, signing keystore verified, AAB at `app/build/outputs/bundle/release/app-release.aab`. Review window: 1–7 days. Don't bump versionCode or re-upload unless the user explicitly asks — the current submission is the canonical one.

**First thing to read** (gitignored — lives only on this machine): `BOLSAAF_PROJECT_INFO.md`. Single source of truth for every external identifier — package name, AdMob App ID, upload cert fingerprints, server IP, admin emails, every `/voice/` endpoint, Play Console listing values. If you need *any* ID or credential, it's there before you go hunting.

**Other reference docs, in order of relevance**:
- `PHASE_PROGRESS_TRACKER.md` — narrative log of everything built, dated. Read the latest "COMPLETED" section to know what just landed.
- `PRODUCTION_CHECKLIST.md` — pre-flight gates for new uploads (code, server, Play listing, rollback plan).
- `VOICE_API_CONTRACT_PHASE2_PLUS.md` — API contract; the app's `VoiceApiPhase2Client.kt` and the server must stay consistent with this.

**Server access**: `ssh root@77.237.234.45` (shadowselfwork.com). Django backend at `/var/www/simplelms/backend/`. Voice app at `apps/voice/`. Admin dashboard at `https://shadowselfwork.com/voice/admin/` (log in with `ss.sunil9255@gmail.com`). Restart after any server edit: `systemctl restart voice-gunicorn`.

**What exists that you might not expect**:
- Real auth backend (`VoiceUser` model, `/voice/auth/{register,login,logout,me,consume,password-reset}/`).
- Self-serve account deletion at `/voice/account-deletion/` (Play requirement).
- Dedicated `VoiceAdminSite` at `/voice/admin/` with bulk actions (Reset free quota, Grant Pro 1 month / 1 year, Revoke Pro).
- AdMob SDK wired (App ID `ca-app-pub-9194903827759003~6689280046`) but **no ad units placed yet**.
- Brand theme unified — red → purple → blue gradient everywhere via `BrandGradient` helper. App is light-only by design.
- `/voice/privacy` + `/voice/terms` hosted as static HTML at `/var/www/static-legal/`.

**Signing** — upload key SHA-256 is `80:D4:24:C4:25:91:7C:A9:C5:45:B0:26:71:A8:87:9B:F6:AD:9F:CD:AB:50:F9:41:8E:7E:89:FC:70:7E:00:00`. Keystore at `keystore/bolsaaf-release.jks`, passwords in `.env` (both gitignored). Off-machine backup at `~/bolsaaf-signing-backup-20260415.tar.gz`. **Never commit `.env` or `keystore/`.**

**User**: Vaibhav / "Sunil" (commit author). Prefers terse Hinglish communication ("bhai", "dost"). Ships fast, wants working code over polish. Delegates decisions ("tu decide kar") and expects me to exercise judgment + flag trade-offs. Already built and shipped a lot today — read the tracker before suggesting big new directions.

## Project

BolSaaf is an Android voice-cleaner app (Kotlin + Jetpack Compose) that wraps Xiph **RNNoise** on-device via JNI/NDK and also talks to a server-side **Voice Cleaning API v2.2** (`https://shadowselfwork.com/voice/`) for heavier pipelines (Demucs extract, background mix, Reel mode, video processing). The app's current product direction is "reel creation engine", not just noise cleaning — see `VOICE_API_CONTRACT_PHASE2_PLUS.md` and `PHASE_PROGRESS_TRACKER.md` for the locked intent.

## Build, run, test

Single Gradle module `:app`. Android SDK **35** / min 24 / target 35, Kotlin + Compose (compiler ext 1.5.1), NDK + CMake 3.22.1 for the native RNNoise lib. Native libs built with `-Wl,-z,max-page-size=16384` for 16 KB page size compatibility.

```bash
./gradlew :app:assembleDebug               # build debug APK
./gradlew :app:installDebug                # install to connected device
./gradlew :app:testDebugUnitTest           # JVM unit tests (what CI runs)
./gradlew :app:testDebugUnitTest --tests "com.bolsaaf.audio.PcmResampleTest"   # single class
./gradlew :app:testDebugUnitTest --tests "com.bolsaaf.audio.PcmResampleTest.someMethod"  # single test
./gradlew :app:connectedDebugAndroidTest   # instrumented tests (needs device)
./gradlew :app:assembleRelease             # signed release (reads keystore from .env)
```

Release signing reads `SIGNING_KEYSTORE`, `SIGNING_ALIAS`, `SIGNING_STORE_PASSWORD`, `SIGNING_KEY_PASSWORD` from the repo-root `.env` (see `app/build.gradle.kts`). `.env` is checked in; do not commit real production secrets there.

### Python side (benchmarks / tuning)

CI (`.github/workflows/ci.yml`) also runs offline adaptive benchmarks after the Android unit tests. To reproduce locally (needs `ffmpeg` for some scripts, plain Python 3.11 for the two below):

```bash
python3 scripts/generate_test_signals.py       # writes synthetic 48k WAVs to testdata/wav/
python3 scripts/benchmark_batch_analyze.py     # runs adaptive_audio_analyze over testdata/, writes data/benchmark_adaptive_batch.json
```

`scripts/` also contains one-off `_patch_*` / `deploy_*` scripts that mutate the **remote server** (`shadowselfwork.com`) — they are not part of local dev; don't run them unless explicitly asked.

## Architecture

### On-device audio pipeline

`app/src/main/cpp/` builds `librnnoise-lib.so` from upstream Xiph RNNoise sources (`rnnoise/src/*.c`) plus a JNI shim (`rnnoise_jni.cpp`). The Kotlin wrapper layer in `app/src/main/java/com/bolsaaf/audio/` is the heart of the on-device engine and is where most logic lives:

- `RNNoise.kt` / `RNNoiseBridge.kt` — JNI surface. **RNNoise requires 480-sample frames at 48 kHz mono** — every caller must respect this.
- `PcmFormat.kt`, `PcmResample.kt` — format + resampling helpers. Live mic path reads the actual `AudioRecord.sampleRate` (API 23+), accumulates PCM, resamples to 48 kHz mono, and feeds RNNoise in fixed 480-sample chunks. File-clean path **zero-pads tail frames** through RNNoise rather than bypassing them. Do not regress this — it was the Phase 1 hardening fix (see `PHASE_PROGRESS_TRACKER.md`).
- `AudioRecorder.kt`, `AudioInputStage.kt`, `AudioProcessor.kt` — capture + processing orchestration.
- `AdaptiveAudioAnalyzer.kt`, `CleaningPreset.kt`, `FeedbackAdaptiveMemory.kt`, `ProcessingQualityGuard.kt` — adaptive mode: analyze input (RMS/peak/zero-fraction/confidence) → pick preset → run post-processing guard (retry → dry-mix → loudness floor). This mirrors the server `adaptive` / `quality_guard` metadata documented in the API contract; keep the two in sync.
- `WavPreview.kt` — WAV I/O.

Unit tests for the above live under `app/src/test/java/com/bolsaaf/audio/` and are the fast feedback loop — prefer adding JVM tests here (no device needed) over instrumented tests when the code doesn't touch Android framework classes.

### Server API clients

Two clients, one per API generation:

- `VoiceCleaningApi.kt` — v2 synchronous-ish `POST /voice/clean/` path. Handles relative `cleaned_url` resolution against the site origin, and falls back to polling `GET /voice/status/{job_id}/` if the clean response omits the URL. Max upload 5 MB; modes: `basic | standard | studio | pro`.
- `VoiceApiPhase2Client.kt` — v2.2+ async job pattern for `extract_voice`, `add_background`, `reel`, `video/process`. All of these return `{status: "accepted", job_id, job_type}` and must be polled via `GET /voice/status/{job_id}/`. Status payload now carries optional `adaptive` + `quality_guard` sections and temporary compat keys (`state`, `mode`, `processing_time`) — the contract in `VOICE_API_CONTRACT_PHASE2_PLUS.md` is authoritative; treat the markdown file and these two client classes as one unit that must stay consistent.

### UI

Single-activity Compose app. Entry point is `MainActivity.kt`; screens under `ui/screens/` (`HomeScreen`, `LiveScreen`, `ComparisonScreen`, `ComparisonPlayerScreen`, `HistoryScreen`, `ProfileScreen`). The Material 3 design system (tokens in `ui/theme/`, motion primitives in `ui/animation/`, reusable animated components in `ui/components/MD3Components.kt`) was added April 2026 — when touching screens, prefer MD3 tokens/components over ad-hoc colors and animations. See `MD3_DESIGN_SYSTEM.md` for the token reference.

### Where the product direction lives

These docs encode decisions that aren't visible in code and are worth reading before substantial changes:

- `VOICE_API_CONTRACT_PHASE2_PLUS.md` — endpoint/request/response shapes, validation rules, rollout strategy (keep legacy `state`/`mode`/`processing_time` keys until app adoption is complete).
- `Voice Cleaning API - Mobile Integratio.md` — v2 mobile integration guide (clean flow, `cleaned_url` resolution rules, error shapes).
- `PHASE_PROGRESS_TRACKER.md` — phase-by-phase status and locked product intent ("reel creation engine", −16 LUFS target, presets: Podcast / Rain / Cafe / Viral).
- `FEATURE_ROADMAP_PHASE3_VIRAL.md`, `MD3_DESIGN_SYSTEM.md`, `IMPLEMENTATION_SUMMARY.md` — roadmap + design + recent-work summaries.

## Conventions worth respecting

- RNNoise frame size / sample rate assumptions live in `PcmFormat.kt` constants — don't hardcode 480 / 48000 elsewhere.
- The Kotlin adaptive pipeline intentionally mirrors the server's adaptive pipeline shape so telemetry and presets align; when adding a field to one side, update the other.
- Keep backward-compatible JSON keys when parsing `/voice/status/` responses (both `status`+`processing_mode` and `state`+`mode`). The server will drop legacy keys only after app adoption stabilizes.
- CI signal is `./gradlew :app:testDebugUnitTest` + the two Python benchmark scripts; before pushing, at minimum run the unit tests.
