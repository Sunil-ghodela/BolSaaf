# BolSaaf Phase Progress Tracker

**Last Updated**: 2026-04-15 (release-ready: AAB signed, legal URLs hosted, brand theme unified)

## ✅ COMPLETED (April 15)

### Release artifact
- Signed **AAB** at `app/build/outputs/bundle/release/app-release.aab` (17.0 MB, `bolsaaf-release.jks`).
- `versionCode = 2`, `versionName = 1.0.1`, `targetSdk = 34`, `minSdk = 24`. Ready to upload to Play Console (Internal testing → Production).
- Play Store listing fields locked:
  - Privacy Policy URL → `https://shadowselfwork.com/voice/privacy`
  - Terms of Service URL → `https://shadowselfwork.com/voice/terms`
  - Support email → `support@shadowselfwork.com` (stub in the policies)
  - Category → Music & Audio (alt: Video Players & Editors); Content rating → Everyone.

### Legal pages moved under `/voice/`
- **Server**: nginx `selfshadowwork` site gained `location = /voice/privacy` and `location = /voice/terms` blocks aliasing the same `/var/www/static-legal/{privacy,terms}.html` files. Backup `selfshadowwork.bak.20260415_061616`. `nginx -t && systemctl reload nginx` clean.
- **App**: Settings dialog `onPrivacyPolicy` / `onTermsOfService` callbacks in `MainActivity.kt` now open the `/voice/` URLs. Old bare `/privacy` + `/terms` still resolve (un-touched) so existing AABs in the wild keep working.

### Brand theme unified across all screens
- **Palette source-of-truth**: `ui/theme/BolSaafPalette.kt` rewritten with the screenshot reference colors:
  - Brand stops: `BrandRed #E94E5B → BrandPurple #A24CB7 → BrandBlue #3D7DDB` (the "Ghar baithe banao" gradient).
  - Make Reel CTA stops: `MakeReelOrange #FF6B35 → MakeReelRed #FF3D5B`.
  - Canvas: warm off-white `#FAFAFA`, white cards, near-black text `#1A1A1F`, gray text `#6B7280`.
  - All legacy names (`ThemeRed`, `ThemeBlue`, `AccentPurple`, `CtaOrangeRedGradient`, `PrimaryGradient`, `SubtitleBluePurple`, …) preserved as aliases so existing call sites compile unchanged.
- **New `BrandGradient` helper**: `BrandGradient.{Brand, BrandLinear, MakeReel, BrandSoft}` — screens should pull from here instead of inlining `Brush.horizontalGradient(listOf(Color(0xFF…)))`.
- **`MD3Theme.kt`** light tokens repointed to brand stops (`primary = BrandRed`, `secondary = BrandPurple`, `tertiary = BrandBlue`, `background = #FAFAFA`, `outline = #E5E7EB`). Dark scheme retuned to lifted brand stops on `#121214` for future use.
- **Light-only by default**: `BolSaafTheme(darkTheme = false)` — pass `true` explicitly to opt into dark. Matches the screenshot reference.
- **Cleanup**: deleted duplicate `ui/theme/Theme.kt` and `ui/theme/Type.kt` (older blue/red `BolSaafTheme` overload + bare `Typography`). MD3 versions are now the only ones.
- **First migration**: `HomeScreen.MakeReelBanner` now uses `BrandGradient.MakeReel` instead of three hardcoded color hexes. Other hardcoded gradient sites (per-card accents in `HomeScreen`, comparison/profile screens) left as a follow-up — they read from the alias names so they already render the new brand colors.

### Today's shipped checklist (carried over from morning session)

| #   | Task                                                                                          |
| --- | --------------------------------------------------------------------------------------------- |
| 8   | Profile **Plan modal** (Free vs Pro, usage + renewal, BEST VALUE / CURRENT badges, Upgrade CTA) |
| 11  | Auto-dismiss success banner (3s)                                                              |
| 12  | Redesigned Quick Clean / Add Vibe / Video Clean cards (gradient icons, per-type accent)        |
| 13  | Real video thumbnails in Recent Cleans                                                        |
| 14  | Polish pass on Recent Cleans card                                                             |
| 15  | Make Reel moved to slim banner below quick cards                                              |
| 16  | Record Live audio/video bottom sheet (`RecordFormatSheet.kt`)                                 |
| 17  | Live tab: Audio/Video format toggle + CameraX                                                 |
| 18  | Privacy/Terms hosted under `/voice/`, URLs wired, versionCode bumped, signed AAB built        |
| 19  | Add Vibe flow fixed (bottom sheet first, then file)                                           |
| 20  | Brand theme refresh (red→purple→blue + orange→red), MD3Theme aligned, dup themes deleted     |

### Outside-the-code follow-ups (not for Claude)
1. Play Console: create app listing → fill descriptions → upload AAB → screenshots / feature graphic / icon.
2. Decide billing: Pro CTA currently shows a toast; wire Google Play Billing after first release if you want revenue.
3. Test the AAB via `bundletool` or Internal testing track before promoting to Production.

### Where things live (April 15 deltas)
- Brand colors + gradients: `app/src/main/java/com/bolsaaf/ui/theme/BolSaafPalette.kt`
- MD3 light/dark schemes: `app/src/main/java/com/bolsaaf/ui/theme/MD3Theme.kt`
- Plan modal: `ui/screens/ProfileScreen.kt::PlanDialog` (line ~586) + `PlanCard` helper.
- Record-format bottom sheet: `ui/screens/RecordFormatSheet.kt`, video record overlay: `ui/components/VideoRecordOverlay.kt`.
- Legal URLs in app: `MainActivity.kt:539` (privacy) / `:543` (terms).
- Server: `/etc/nginx/sites-enabled/selfshadowwork` (lines added next to existing `/privacy` + `/terms` blocks).

---

## ✅ COMPLETED (April 14 — evening)

### Free quota (20 min/month)
- **`MainActivity` companion object**: `FREE_QUOTA_MINUTES = 20`, `QUOTA_WARN_THRESHOLD = 3`.
- **Persistence**: `SharedPreferences("bolsaaf_quota")` with `period` (yyyyMM) + `left` keys. Auto-resets at the start of each calendar month (`loadFreeMinutes()` called in `onCreate`).
- **Decrement chokepoint**: single call to `consumeQuotaSeconds(dur)` inside `addAudioPair(...)` — rounds up to nearest minute via `ceil(seconds/60)`, clamps ≥ 0. Covers every successful path (HomeScreen cloud clean, video flow, reel flow, FastLib audio + video) without scattering calls.
- **Thresholds**: toast warning when crossing to ≤3 min ("3 min free left. Upgrade to Pro for unlimited."); hard alert at 0 ("Free quota used up. Upgrade to Pro to continue.").
- **Home top-right chip** (`GlassmorphicHeader`): redesigned from plain "N min free" text into a compact pill with **circular progress ring** + `N / 20 min`. Flips to red background + red accent when `freeMinutesLeft ≤ 3`.

### FastLib Lab/Dev screen polish
- **Phase progress card** during `fastLibIsCleaning`: circular spinner (or filled ring during download %), title flips by stage — `Uploading…` → `Cleaning Audio…` → `Downloading 45%`. 3-step horizontal progress row (Upload → Clean → Download) fills in sequence; the active step shows partial fill (0.6 for processing, real `progressPct/100` for download).
- **MainActivity stage state**: added `fastLibStage: String?` (uploading/processing/downloading) + `fastLibProgress: Int`. `startFastLibTestCleaning` now transitions them and reuses `downloadFromUrl(..., onProgress)` for byte-granular download %.
- **Outputs list**: replaced the single `Output Ready: filename` line with a `LazyColumn` of per-job output cards. State: `fastLibOutputs = mutableStateListOf<String>()`, newest-first, capped at 20 entries.
- **Per-row card** (`OutputRow`): 84×60 thumbnail tile + type chip (AUDIO cyan / VIDEO cyan) + filename + human-readable size (`humanSize()` helper, KB/MB/GB); per-row `Play / Share / Save` buttons.
- **Video thumbnail**: `VideoThumbnail` composable uses `MediaMetadataRetriever.getFrameAtTime(1_000_000, OPTION_CLOSEST_SYNC)` on `Dispatchers.IO` (via `LaunchedEffect`), renders as `Image(bitmap=..., ContentScale.Crop)`. Play icon overlaid in a circle. Audio rows fall back to MusicNote icon on colored tile.

### FastLib limits (client-side guardrail + info card)
- **`MainActivity` companion object**: `FASTLIB_MAX_VIDEO_BYTES = 50 MB`, `FASTLIB_MAX_AUDIO_BYTES = 25 MB` (matches nginx `voice_upload` location's `client_max_body_size`).
- **Early reject**: new `rejectIfOversized(uri, isVideo)` (uses `OpenableColumns.SIZE`) runs in the `pickFast{Audio,Video}Launcher` callbacks. Oversized pick → Toast `"File too large: 73.4 MB. Max 50 MB for video."` and input is not set, no wasted upload.
- **Info card on screen**: "LAB LIMITS" card (cyan) lists Audio / Video size caps + supported formats + engine info (`DeepFilterNet3 · CPU only · ~1s per 5-10s of audio`). Rate limits intentionally NOT shown in UI per product call — still enforced server-side via DRF `ScopedRateThrottle` (`voice_fastlib_clean` 15/min, `voice_fastlib_video` 5/min) with the friendly-429 toast from the error map if tripped.
- **Upload box subtitles** now show size in the hint: `MP3, WAV, AAC · ≤25 MB`.

### Where things live
- Quota: `MainActivity.loadFreeMinutes()`, `consumeQuotaSeconds()`, `quotaPrefs()`; chip in `HomeScreen.GlassmorphicHeader`.
- FastLib progress + list: `MainActivity.startFastLibTestCleaning` (stage transitions), `FastLibTestScreen` (UI); thumbnail via `VideoThumbnail` composable inside `FastLibTestScreen.kt`.
- FastLib limits: `MainActivity.rejectIfOversized`/`uriContentSize`; `FastLibTestScreen.LimitsInfoCard` + `LimitLine`.

### Pending for next session
- **Task 8**: Full Plan modal in Profile screen (Free vs Pro, current usage + renewal, Upgrade CTA stub). `ProfileScreen` already takes `freeQuotaMinutes` / `freeMinutesLeft` / `onUpgrade` props — just needs the modal/dialog itself built out. `FREE_QUOTA_MINUTES` constant is already wired.

---

**Earlier on April 14** (morning):

## ✅ COMPLETED (April 14 — morning)

### Video flow UX
- **Download progress in Processing dialog**: `downloadFromUrl()` now reports byte progress (128 KB granularity); polling loop sets `phase2StageName` = `processing` → `downloading`; dialog title flips `Cleaning Audio Track…` → `Downloading Video… 45%`. Fixes the "78-second silent spinner" on slow 4G that users reported as "app stuck".
- **Video items in Recent Cleans**: `MainActivity.playAudioFile` detects `.mp4/.mov/.mkv` and routes to `openVideoExternal()` (FileProvider + `ACTION_VIEW` chooser — MediaPlayer without a Surface can't play video). Recent card shows a `VideoPreviewBox` (cyan badge, filename, "Open" CTA) instead of waveform + side-by-side audio boxes.
- **Recent Cleans card overhaul**: full redesign — 56dp circular gradient play button with pulse animation, type chip (AUDIO/VIDEO/LIVE) + time + duration on one line, top-right `×` for delete, body adapts (waveform + "compare with original" for audio; full-width "Open in video player" CTA for video), compact icon-only footer row (Share / Save / Feedback).

### Error handling
- **User-visible rate-limit / size errors**: new `friendlyHttpError(code, body)` helper in `audio/VoiceApiPhase2Client.kt` maps `429` → "Too many requests. Please wait a minute and try again.", `413` → "File too large — max 50 MB for video, 10 MB for audio.", `401/403` → "Not allowed. Please sign in again.", `502/503/504` → "Server is busy — please try again in a moment.". Applied to every non-2xx throw site in `VoiceApiPhase2Client`, `VoiceCleaningApi`, and `MainActivity.downloadFromUrl`.

### Backend (shadowselfwork.com)
- **Rate limiting (defense-in-depth)**:
  - **Nginx** `/etc/nginx/nginx.conf`: 3 `limit_req_zone`s — `voice_status` 120r/m, `voice_general` 30r/m, `voice_upload` 10r/m. `/etc/nginx/sites-enabled/selfshadowwork` split `/voice/` into status vs upload locations with zone-specific burst and `client_max_body_size` (1M for status, 50M for upload).
  - **DRF** scoped throttles on all 15 voice views via `ScopedRateThrottle`: `voice_clean` 20/min, `voice_video` 5/min, `voice_reel` 5/min, `voice_status` 120/min, `voice_health` 60/min, `voice_feedback` 30/min, `voice_extract`/`voice_addbg` 10/min, `voice_fastlib_clean` 15/min, `voice_fastlib_video` 5/min.
  - **Django**: `DATA_UPLOAD_MAX_MEMORY_SIZE = 60 MB`, `FILE_UPLOAD_MAX_MEMORY_SIZE = 5 MB`.
  - Backups: `selfshadowwork.bak.20260414_ratelimit`, `settings.py.bak.20260414_ratelimit`.
  - Verified with hammer test: 120 passes then `429`s kick in (with burst=30).
- **Real `fast-music-remover` integration** (replacing the "FastLib" stub that just wrapped `StudioPipeline`):
  - Docker installed + enabled, `ghcr.io/omeryusufyagci/fast-music-remover:latest` running as container `fast-music-remover` on `127.0.0.1:8088` (restart=unless-stopped).
  - `apps/voice/tasks.py` new helper `_run_fast_music_remover(input_path, output_basename)` — POSTs multipart file, reads `media_url` from JSON, streams result back into `MEDIA_ROOT/cleaned/`. Both `fastlib_clean_task` and `fastlib_video_task` rewritten to use it; `processing_mode` set to `"fastlib"` so the status payload distinguishes from `studio`.
  - Verified end-to-end: job 107, 1 MB MP3 → 8.86s → 5.3 MB WAV downloadable at `/voice/cleaned_output/107/`.
  - Backup: `tasks.py.bak.20260414_fastlib`.

### Where things live
- Download progress: `MainActivity.downloadFromUrl` + `processingDialogTitle`/`Subtitle`.
- Video routing: `MainActivity.playAudioFile` + `openVideoExternal`; `HomeScreen.ComparisonCard` (`isVideo` branch + `VideoPreviewBox`).
- Error map: `app/src/main/java/com/bolsaaf/audio/VoiceApiPhase2Client.kt` bottom.
- Backend FastLib: `/opt/fast-music-remover-tmp/` (reference clone), container `fast-music-remover`, tasks at `/var/www/simplelms/backend/apps/voice/tasks.py`.

---

**Previous**: 2026-04-13 (UI/UX fixes + Settings + Login/Logout)

## ✅ COMPLETED UI/UX FIXES (April 13)

### Home Screen
- **Make Reel Banner**: Converted button to banner-style gradient CTA with sparkles and arrow indicator
- **Recent Clean Cards**: Removed blue strip, added proper spacing (8dp vertical), border, and scale/fade animations
- **Video Support**: Fixed video picker to support video/* and audio/* MIME types

### Settings
- **Settings Dialog**: Created full Settings dialog with About, Clear Cache, Privacy Policy, Terms of Service options
- **Quick Action Card**: Settings card now functional with onClick handler

### User Profile
- **Email Login/Logout**: Implemented LoginDialog with email/password fields
- **User State**: Added isLoggedIn, userEmail, userDisplayName, userHandle state in MainActivity
- **Profile UI**: Shows logged-in user email with logout button, or login prompt when not logged in

**Status**: All UI/UX fixes completed and wired to MainActivity

## 🎨 LATEST: Material Design 3 Design System (April 13)

**Status**: ✅ COMPLETE - Foundation Phase
- Comprehensive MD3 color scheme (light & dark)
- Typography system (15 scales)
- Shape tokens (5 sizes)
- Motion system (3 timing strategies)
- 9 reusable animated components
- Build: SUCCESS | Device: 93b0c2c0

**Next**: HomeScreen redesign (Priority 1)

---

# BolSaaf Phase Progress Tracker (Previous)

## Product direction (locked intent)

- **Positioning:** shift from “noise cleaner only” to **Reel creation engine** (clean → optional extract → BG → loudness → video export on server).
- **Current phase:** build is strong; focus is **consistency, experience, quality**, and **server reel finalization** (incl. −16 LUFS target, presets: Podcast / Rain / Cafe / Viral — product names; implementation mostly backend + tuning).
- **Benchmark:** compulsory discipline — use `data/BENCHMARK_REEL_5TAKE_TEMPLATE.md` (5 real takes + table + listening notes).

## Overall completion (~85%)

| Pillar | Status |
|--------|--------|
| Core engine | Done |
| Studio engine | Very strong (reel chain service + endpoints validated) |
| Smart engine | Stronger (adaptive + feedback-driven extract tuning) |
| UX | Good → **refreshed** (light yellow + red/blue brand, 4-tab nav, flatter cards/nav) |
| Advanced | Growing → retry + dry-mix + loudness floor + feedback telemetry |

**Total roadmap completion (weighted): ~84%**

**Sample matrix (2026-04-09):** `AUDIO_SAMPLE_CHECK_20260409_MATRIX.md`; JSON under `data/audio_sample_matrix_20260409.json` when exported.

---

## Phase 1 — Core Engine (100% Complete)

- RNNoise cleaning path
- **2026-04-11 — Voice-clean pipeline hardening (Android):** live mic path now **accumulates PCM**, reads **actual `AudioRecord.sampleRate` (API 23+)**, **resamples to 48 kHz mono** in fixed RNNoise-sized chunks (`PcmResample` + 480-sample frames); file clean tail frames are **zero-padded** through RNNoise instead of bypassing. Helpers: `PcmResample.kt`, `PcmFormat.kt`. Follow-ups: loudness (−16 LUFS), Demucs dry-mix tuning (server/product).
- DSP path
- Django API + cloud integration
- Mobile app integration
- Health/status flow active

---

## Phase 2 — Studio Engine (~92% Complete)

### 2.1 Extract Voice (Demucs) — **100%**
- Endpoint live: `POST /voice/extract_voice/`
- Async job accepted -> processed -> output URL
- Status returns `job_type=extract_voice` + `output_audio_url`

### 2.2 Background Environment — **90%**
- Endpoint live: `POST /voice/add_background/`
- 5 backgrounds uploaded (`rain/cafe/ocean/forest/street`)
- ffmpeg mixing with `bg_volume`
- Status returns `job_type=add_background` + output URL
- Remaining: preview assets tuning and quality balancing preset-by-preset

### 2.3 Video Support — **70%**
- Endpoint live: `POST /voice/video/process/`
- Pipeline working: extract audio -> process -> remux -> output video URL
- Remaining: production hardening, bg integration in video reel path, timeout/retry

### 2.4 Reel Mode — **80% → finalize next**
- Endpoint live: `POST /voice/reel/`
- **Target pipeline (lock):** input → adaptive hint → clean *or* extract (Demucs) → **background mix** → **loudness (~−16 LUFS)** → **export video** (server).
- App: **Make Reel** default flow; orange **“Make Reel — recommended”** CTA; reel job sends **background + volume** again (server must honor when ready).
- Server chain now patched: clean/extract option + bg mix + loudnorm in reel service; request fields (`bg`, `bg_volume`, `extract_voice`, `preset_id`, `adaptive`) wired.
- Remaining: finalize video branch with full reel-audio chain parity + production hardening.

---

## Phase 3 — Smart Processing (~62% Complete)

### Done (v2 adaptive automation)

- **`AdaptiveAudioAnalyzer` (v2):** final combined logic using RMS / peak / zero / near-zero / crest + confidence; emits runtime `AdaptivePreset(preGain, denoiseLevel, compressorStrength, dryMix, mode)`.
- **Auto execution:** cloud clean now auto-selects mode from adaptive preset (with availability fallback); local fallback maps adaptive preset to cleaning preset.
- **Python mirror:** `scripts/adaptive_audio_analyze.py` (single WAV → JSON).
- **Batch:** `scripts/benchmark_batch_analyze.py` → `data/benchmark_adaptive_batch.json`.
- **Synthetic fixtures:** `scripts/generate_test_signals.py` → `testdata/wav/*.wav`.
- **Unit tests:** `AdaptiveAudioAnalyzerTest`, `ProcessingQualityGuardTest`.

### Plan (next)

| Milestone | Goal |
|-----------|------|
| **3b** | **Done (chip):** After file pick, Home + “File ready” dialog show **Suggested preset** row (levels check) with **Apply**; uses capped WAV export (~30s) for analysis. |
| **3c** | **Done (runtime):** adaptive preset is applied automatically for each processed audio; still needs threshold tuning from real clips. |
| **3d** | Server-side adaptive mode pick using same metrics on upload. |
| **3e** | **Live (2026-04-13):** server `ExtractVoiceTuningState` + `extract_feedback_tuning.py` — rolling stats from `VoiceFeedback` (`extract_voice`, `reel`, `reel_mode`) update **`vocals_dry_ratio`** (original mixed into Demucs vocals via ffmpeg `amix`, EMA-smoothed). Applied in `demucs_extract.run_extract_voice_job` and `reel_mode._maybe_extract_voice`. Recompute on feedback POST (async) and throttled at extract start. Deploy: `scripts/deploy_extract_voice_tuning.sh` + `deploy_extract_voice_tuning_remote.py`. |

---

## Phase 4 — User Experience (~82% Complete)

- **Primary story:** **Make Reel ★** first chip; default `ProcessingFlow` = **REEL**; full-width **Make Reel — recommended** (gradient CTA) + **Hero “Clean Audio / Video”** card (orange→red CTA, white card shell).
- **Home flow chips (4):** **Reel ★ / Quick / BG / Video** — horizontal-scroll `ModeSelector`; **Extract** removed from chip row (dedicated Extract tab also removed; server `extract_voice` / `extract_from_url` deferred in product until YouTube/cookies path is stable).
- **Navigation:** **Home · Live · History · Profile** (4 tabs). Bottom bar is **solid** (no full-width gradient) for readability; brand gradient kept on small accents (e.g. active chip, top strip on recent-clean cards).
- **Theme (2026-04-11):** `BolSaafPalette` + light `MaterialTheme` — **#FFFBEB** canvas, white cards, brand **#E34C52 → #537FE7**; header matches refs (**rounded-square logo**, **“Audio & Video Studio”**, red **min free** pill). **Upload / Batch / Settings** quick cards and **stats row** are flat white + border (no card gradients). **Recent cleans** `ComparisonCard`: more padding + **thin horizontal red→blue strip** on top (no gradient ring).
- Output card includes **Feedback** (Home/History/Live).
- Feedback dialog: **“Voice clear hui?” Yes/No**; issue chips when No.
- **Reel + BG mix:** background picker + volume for **Reel** and **BG**; reel API `bg` + `bg_volume`.
- Phase-2 async: submit → poll → download; **Video** = MP4, share MIME correct.
- **Remaining:** async stage-wise progress UI; Live parity polish; reel **video** preview when server exports; optional restore **Extract** (file or URL) behind stable `yt-dlp`/cookies.

---

## Phase 5 — Advanced Features (~58% Complete)

### Done (v1 + v2 guards)

- **`ProcessingQualityGuard`** on single-chunk cloud clean; on fail → automatic retry with milder API mode (`pro→studio→standard→basic`).
- **Dry-mix guard live:** applies adaptive `dryMix` when hollow risk is detected.
- **Audibility floor live:** if still `output_very_quiet`, apply post-gain loudness floor (adaptive target).
- **Extract post-tuning (server / when used):** compare original vs cleaned and mild dry-mix/loudness fix when guard issues — **Android app no longer runs file `extract_voice` flow in UI** (tab/chip removed); pipeline still on API for future re-enable.
- **Extract mode softening:** when extract is used, milder cloud mode preference remains a server/client contract option.
- **Feedback telemetry pipeline live:** app → `POST /voice/feedback/` with auto metadata + quick human label.

### Plan (next)

| Milestone | Goal |
|-----------|------|
| **5b+** | Dry/wet blend when guard still fails after retry. |
| **5c** | Loudness: target integrated loudness (e.g. −16 LUFS) — likely ffmpeg or server; document EBU R128 later. |
| **5d** | VAD: gate aggressive processing to speech frames (WebRTC VAD or energy+VAD hybrid). |

---

## Next Priority (Execution Order)

### Phase 3 Viral Engine Sprint (Execution Lock)

- **Source roadmap:** `FEATURE_ROADMAP_PHASE3_VIRAL.md` (rewritten as v1 execution spec, scope lock + DoD).
- **API contract locked:** `data/API_REEL_V2.md` (`/voice/reel/create` + `/voice/reel/{id}/status` + 3 variant outputs).
- **V1 scope (must ship):** one-tap reel job, stage-wise progress, 3 outputs (`clean_only`, `with_bg`, `viral_boosted`), -16 LUFS target, 30-day expiry metadata.
- **Defer:** whisper filler removal, advanced enhancer chain, full batch queue UX, auto-captions.
- **Execution sequence:**
  1. Backend: Reel V2 endpoint + stage machine + ReelJob/ReelOutput persistence
  2. Android: CTA + stage timeline + 3-output results + share
  3. QA: daily 5-take benchmark + reel smoke + failure metrics

1. **Reel V2 backend:** implement `POST /voice/reel/create` + `GET /voice/reel/{id}/status` with stage machine and 3 variant outputs (see `data/API_REEL_V2.md`).
2. **Android Reel V2 UX:** one-tap CTA + stage timeline + multi-output result/share screen.
3. **Video parity:** ensure each requested variant can export MP4 (9:16 path where applicable) with stable retries/timeouts.
4. **Adaptive tuning pass:** calibrate thresholds using daily 5 real takes (`data/BENCHMARK_REEL_5TAKE_TEMPLATE.md` + dated logs).
5. **Quality robustness:** continue retry/dry-mix/floor + VAD/dry-wet iteration after V2 ship.




## CI tooling

- Added GitHub Actions workflow: `.github/workflows/ci.yml`
  - installs ffmpeg
  - runs Android unit tests (`:app:testDebugUnitTest`)
  - runs adaptive benchmark scripts and uploads artifact (`data/benchmark_adaptive_batch.json`)

## Recent validation (server, 2026-04-13)

- **Phase 3A scaffold live:** added server-side `ReelJob` + `ReelOutput` models and Reel V2 endpoints:
  - `POST /voice/reel/create/`
  - `GET /voice/reel/{reel_job_id}/status/`
  - worker: `apps/voice/services/reel_v2.py`
  - deploy script (repo): `scripts/deploy_reel_v2_scaffold_remote.py`
- **Worker upgrade (2026-04-13, hotfix):** Reel V2 worker now calls real `run_reel_job` / `run_video_reel_job` per variant with timeout guards and stage transitions.
- **Smoke test:** Reel V2 job accepted and completed with stage progress + analysis summary payload (`job_id=3`, variants requested: `clean_only`, `with_bg`).
- **Stability note:** one syntax regression in `reel_v2.py` caused temporary `502`; fixed and service recovered (`/voice/health/` back to `ok`).
- **Hardening pass (2026-04-13):**
  - duplicate-safe `requested_variants` normalization (order preserved, de-dup)
  - per-variant failure isolation in worker (`variant_errors` in `analysis_summary`; partial success allowed)
  - timeout guards for audio/video variant processing
  - `error_code` support in Reel V2 status for failed jobs
  - TTL utility command added and validated: `manage.py cleanup_reel_expired`
- **Deploy + restart:** `bash scripts/deploy_extract_voice_tuning.sh root@77.237.234.45` synced `extract_feedback_tuning.py` + `demucs_extract.py`; `deploy_extract_voice_tuning_remote.py` re-run (migrations up-to-date, patches idempotent). **Django `manage.py runserver 0.0.0.0:8000`** restarted under `nohup` for `/var/www/simplelms/backend`; **`GET /voice/health/`** verified **ok** after bounce.
- **Demucs ↔ DB:** `ExtractVoiceTuningState` (singleton) stores `vocals_dry_ratio`; feedback drives blend strength for extract + reel Demucs paths.
- SSH `root@77.237.234.45`: `https://shadowselfwork.com/voice/health/` returns **ok** (ffmpeg + DeepFilterNet; job types include reel / video_reel / extract_from_url).
- **Loudness:** `reel_mode.py` + `video_process.py` use **2-pass `loudnorm`** default **−16 LUFS**; `ffmpeg` lists **EBU R128 loudnorm** filter.
- **yt-dlp:** system `2026.03.17` present; venv has package installed.
- **Ops:** created `/var/www/simplelms/backend/secrets/` (`700`); appended **commented** `# YT_DLP_COOKIES=...` hint to `.env` (uncomment after uploading Netscape `youtube_cookies.txt`).
- **Note:** `curl http://127.0.0.1:8000/voice/health/` returns **400** (Host / `ALLOWED_HOSTS`); use public hostname or correct `Host` header for local checks.

## Recent validation (device)

- Latest runs (`122242`, `122304`) confirm adaptive mode selection (`studio`, `STRONG`, `preGain=4.0`) is firing correctly.
- Outputs remain very quiet for ultra-low-input captures; this is now handled by retry + dry-mix + loudness floor, but thresholds still need tuning from real recordings.

- Remote reel smoke: `POST /voice/reel/` with full fields accepted + completed (`job_id=24`, `job_type=reel`, `cleaned_url` returned).
- **Android Reel V2 wiring (2026-04-13):**
  - app Reel flow switched to `POST /voice/reel/create/` + polling `GET /voice/reel/{reel_job_id}/status/`
  - processing dialog now shows live Reel stage + progress percent from backend status
  - Home UI now renders multi-output Reel actions for `viral_boosted`, `with_bg`, `clean_only` (play/share/save)
- **Build + install validation (2026-04-13):**
  - compile: `./gradlew :app:compileDebugKotlin` ✅
  - release build: `./gradlew :app:assembleRelease` ✅ (`app/build/outputs/apk/release/app-release.apk`, ~14 MB)
  - device install: release APK installed on `Redmi_Note_7_Pro` (after uninstall due to debug/release signature mismatch)
- Feedback API live and receiving real entries from app (refreshed **2026-04-13** via `scripts/feedback_report.py`):
  - total entries: **13**; clear_voice yes/no: **3 / 7**; by result: `good=5`, `artifacts=6`, `quiet=2`
  - modes: `clean=5`, `extract_voice=4`, `add_background=2`, `reel=2`
  - latest same-day samples include `add_background` (quiet / artifacts + smoothness notes)
  - local adaptive bias: `FeedbackAdaptiveMemory.record` + `applyFromFeedbackHistory` in `MainActivity` (noise frustration score boosts cloud mode / denoise)
  - tracker files: `data/FEEDBACK_RESULT_TRACKER.md`, `data/feedback_summary_history.jsonl`
