# BolSaaf Feature Roadmap: Phase 3 - Viral Reel Engine

Date: 2026-04-13  
Priority: Core product differentiator  
Primary Goal: One-tap reel-ready output for voice creators

---

## Mission

Build a reel engine where user flow is:

1. Upload audio/video
2. Tap one CTA
3. Receive 3 share-ready versions with clear stage progress

Output variants (v1):
- `clean_only`
- `with_bg`
- `viral_boosted`

---

## Scope Lock (V1)

### In Scope (must ship)
- Single backend job orchestration for reel creation
- Stage-wise progress model (`analyzing`, `cleaning`, `extracting`, `mixing`, `encoding`, `completed`)
- 3-output generation in one job
- Loudness normalization target ~`-16 LUFS`
- Reel video export path parity for selected output(s)
- Android stage progress UI + 3-output result screen + share actions
- DB persistence for outputs, job meta, expiry (30 days)

### Out of Scope (defer)
- Whisper-based filler word removal
- Advanced de-reverb / de-ess chain
- Batch (3-5 files) queue UX
- Auto captions pipeline
- Large preset catalog expansion

---

## Architecture (V1)

### Backend flow

Upload -> ReelJob created -> Analyze -> Process variants -> Encode video -> Save outputs -> Completed

Detailed sequence:
1. Analyze source (duration, RMS/LUFS/noise/silence)
2. Create base clean stream (existing clean/reel chain)
3. Optional Demucs extract (using DB-driven dry blend)
4. Generate variants:
   - `clean_only`: clean chain only
   - `with_bg`: clean + bg mix + loudnorm
   - `viral_boosted`: extract/clean + bg preset + loudnorm + optional trim
5. Encode video output(s) where requested
6. Store URLs, metrics, and stage timings

### Reuse existing assets
- `apps/voice/services/reel_mode.py`
- `apps/voice/services/video_process.py`
- `apps/voice/services/extract_feedback_tuning.py`
- Existing `AudioFile` + `VoiceFeedback`

---

## Delivery Plan

### Phase 3A (Week 1) - Foundation

#### Backend
- [ ] Add Reel V2 create/status contract (`/voice/reel/create`, `/voice/reel/{id}/status`)
- [ ] Introduce normalized stage state machine and progress accounting
- [ ] Add 3-output generator and per-output metadata
- [ ] Persist expiry (`expires_at = created + 30 days`)

#### Android
- [ ] Home CTA: "Make Reel Ready"
- [ ] Processing dialog with stage timeline + overall progress
- [ ] Result card for 3 variants with play/share/download actions

#### Acceptance criteria
- [ ] 3-minute input returns all 3 outputs under target budget (<= 60s baseline target)
- [ ] Status endpoint reports deterministic stage progress
- [ ] At least 1 reel smoke test fully passes end-to-end daily

### Phase 3B (Week 2) - Smart Suggestions (minimal)

#### Backend
- [ ] Add fast analyzer summary (`rms`, `noise_floor`, `silence_ratio`, `voice_clarity_score`)
- [ ] Suggest one preset id (`podcast`, `street_vlog`, `motivational_reel`, `asmr_calm`)

#### Android
- [ ] Show top 2 suggestions only (severity + impact)
- [ ] One-tap apply suggested preset

#### Acceptance criteria
- [ ] Analyzer result available within 5-10s for typical clip
- [ ] Suggestion payload safely optional for old clients

### Phase 3C (Week 3+) - Quality polish
- [ ] Compare screen (before/after waveform + loudness/noise/duration summary)
- [ ] Quick share templates (IG/WA/YT/TikTok)
- [ ] Optional Voice Polish toggles (pause trim first)

---

## API Surface (finalized for build)

See: `data/API_REEL_V2.md`

Required endpoints:
- `POST /voice/reel/create`
- `GET /voice/reel/{reel_job_id}/status`
- `GET /voice/reel/{reel_job_id}/outputs/{variant}/download` (or pre-signed URL contract)

Backward compatibility:
- Keep existing `/voice/reel/` until clients migrate.

---

## Data Model (V1)

### ReelJob
- `id`
- `source_audio_file_id` (nullable if direct upload)
- `source_type` (`audio`, `video`)
- `requested_variants` (json array)
- `preset_id`
- `target_lufs`
- `status` (`pending`, `processing`, `completed`, `failed`)
- `current_stage`
- `overall_progress` (0..100)
- `stage_progress` (json map)
- `analysis_summary` (json)
- `error_message`
- `created_at`, `updated_at`, `expires_at`

### ReelOutput
- `id`
- `reel_job_id`
- `variant` (`clean_only`, `with_bg`, `viral_boosted`)
- `audio_url`
- `video_url` (nullable)
- `duration_sec`
- `loudness_lufs`
- `size_bytes`
- `created_at`

### ReelFeedback (optional in v1.1)
- `reel_job_id`, `variant`, `rating`, `issue`, `notes`

---

## Non-Functional Targets

- Processing success rate: >= 95% on known-good inputs
- P95 status polling freshness: <= 2 seconds
- Failures must return stage and reason (`error_code` + `message`)
- Storage TTL cleanup job for expired outputs

---

## Risks and Mitigations

1. Video encoding latency spikes
   - Mitigation: stage timeouts + retry + graceful fallback to audio-only outputs

2. Demucs artifact variability
   - Mitigation: keep DB-driven `vocals_dry_ratio` active and tracked in job metadata

3. Scope blow-up from polish features
   - Mitigation: enforce V1 scope lock above, defer filler/whisper and batch queue

---

## Ownership Checklist

### Backend owner
- [ ] Reel V2 endpoints + models + migrations
- [ ] Stage machine + 3-output generator
- [ ] Monitoring hooks (duration/failure/stage)

### Android owner
- [ ] CTA + file pick + progress timeline
- [ ] 3-output results + share flow
- [ ] Polling contract alignment with V2 status

### QA owner
- [ ] Daily 5-take benchmark (`data/BENCHMARK_REEL_5TAKE_TEMPLATE.md`)
- [ ] Cross-device app validation + server smoke scripts
- [ ] Regression checks for clean/background/reel/video job types

---

## Definition of Done (Phase 3 V1)

- User can create reel from audio/video with one tap
- User receives 3 output variants reliably
- User sees transparent stage progress while processing
- User can preview and share selected variant
- System records enough metrics/feedback to improve defaults weekly

