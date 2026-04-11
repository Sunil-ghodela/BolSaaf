# BolSaaf Phase Progress Tracker

Last updated: 2026-04-11

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
| **3e** | Use feedback trend (`artifacts/smoothness`) to auto-adjust extract defaults weekly. |

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

1. **Reel server finalization:** clean/extract + background + loudness (−16 LUFS) + **video export** in one stable reel pipeline.
2. **UX status clarity:** stage-wise progress (`Analyzing → Cleaning → Mixing → Finalizing`) on Home processing dialog + upload path.
3. **Adaptive tuning pass:** calibrate thresholds using daily 5 real takes (`data/BENCHMARK_REEL_5TAKE_TEMPLATE.md` + dated logs).
4. **Quality robustness:** keep retry/dry-mix/floor; VAD-assisted decisions + optional dry/wet blend % tuning.
5. **Extract / URL (when prioritized):** server `yt-dlp` + cookies (`YT_DLP_COOKIES`); restore app chip or tab; doc: `data/API_EXTRACT_FROM_URL.md`, deploy script `scripts/deploy_voice_url_extract/url_extract_service.py`.




## CI tooling

- Added GitHub Actions workflow: `.github/workflows/ci.yml`
  - installs ffmpeg
  - runs Android unit tests (`:app:testDebugUnitTest`)
  - runs adaptive benchmark scripts and uploads artifact (`data/benchmark_adaptive_batch.json`)

## Recent validation (device)

- Latest runs (`122242`, `122304`) confirm adaptive mode selection (`studio`, `STRONG`, `preGain=4.0`) is firing correctly.
- Outputs remain very quiet for ultra-low-input captures; this is now handled by retry + dry-mix + loudness floor, but thresholds still need tuning from real recordings.

- Remote reel smoke: `POST /voice/reel/` with full fields accepted + completed (`job_id=24`, `job_type=reel`, `cleaned_url` returned).
- Feedback API live and receiving real entries from app:
  - total entries observed: 6
  - split: `good=4`, `artifacts=2`
  - mode concentration: extract-heavy feedback (`extract_voice=4`, `reel=2`)
  - tracker files maintained by script: `data/FEEDBACK_RESULT_TRACKER.md`, `data/feedback_summary_history.jsonl`
