# BolSaaf Voice API Contract (Phase 2+)

This document defines implementation-ready API contracts for Phase 2+ reel-first delivery:
- Extract Voice (Demucs)
- Add Background
- Reel Mode (primary product path)
- Video Processing
- Standardized Job Status
- Adaptive + quality guard metadata (new)

Base URL:
`https://shadowselfwork.com/voice/`

---

## 1) Common Patterns

### 1.1 Async Job Pattern
Heavy operations must return a `job_id` and be polled via status endpoint.

Immediate response (accepted):
```json
{
  "status": "accepted",
  "job_id": 101,
  "job_type": "extract_voice",
  "message": "Job queued"
}
```

### 1.2 Standard Job Status Response (v2+)
`GET /voice/status/{job_id}/`

```json
{
  "job_id": 101,
  "status": "processing",
  "job_type": "extract_voice",
  "progress": 42,
  "current_step": "demucs_separation",
  "processing_mode": "studio",
  "input_type": "audio",
  "output_audio_url": null,
  "output_video_url": null,
  "error_message": null,
  "metrics": {
    "duration_sec": null,
    "processing_time_sec": null
  },
  "created_at": "2026-04-08T10:30:00Z",
  "updated_at": "2026-04-08T10:30:05Z",

  "state": "processing",
  "mode": "studio",
  "processing_time": null
}
```

Notes:
- `status`: `pending | processing | completed | failed`
- Keep compatibility keys (`state`, `mode`, `processing_time`) temporarily.
- New clients should use `status` + `processing_mode`.

### 1.3 Common Error Format
```json
{
  "status": "error",
  "error_code": "INVALID_INPUT",
  "message": "File size exceeds 20MB"
}
```

### 1.4 Optional Adaptive / Quality Metadata (v2.2+)
Status responses may include adaptive decisions and guard actions used during processing:

```json
{
  "adaptive": {
    "rms_dbfs": -62.4,
    "peak_dbfs": -12.1,
    "near_zero_fraction": 0.72,
    "zero_fraction": 0.31,
    "confidence": 0.86,
    "preset": {
      "pre_gain": 4.0,
      "denoise_level": "STRONG",
      "compressor_strength": "MEDIUM",
      "dry_mix": 0.10,
      "mode": "studio"
    }
  },
  "quality_guard": {
    "issues": ["output_very_quiet"],
    "retry_applied": true,
    "retry_mode": "standard",
    "dry_mix_applied": true,
    "loudness_floor_applied": true
  }
}
```

Notes:
- These keys are optional and should not break older clients.
- Clients should still rely on `status` and output URLs as source of truth.

---

## 2) Endpoint: Extract Voice

### `POST /voice/extract_voice/`
Extracts vocals (Demucs stem) from input audio/video.

Request:
- Content-Type: `multipart/form-data`
- Fields:
  - `file` (required): audio or video
  - `mode` (optional): `studio | pro` (default `studio`)
  - `output_format` (optional): `wav | mp3` (default `wav`)

Response (202):
```json
{
  "status": "accepted",
  "job_id": 201,
  "job_type": "extract_voice",
  "message": "Voice extraction started"
}
```

Status completed payload should include:
- `output_audio_url` pointing to vocals output.

---

## 3) Endpoint: Add Background

### `POST /voice/add_background/`
Mixes voice with selected background ambience.

Request:
- Content-Type: `multipart/form-data`
- Fields:
  - `file` (required): cleaned or raw voice audio/video
  - `bg` (required): one of `rain | cafe | ocean | forest | street`
  - `bg_volume` (optional): `0.0 - 1.0`, recommended `0.10 - 0.20` (default `0.15`)
  - `voice_gain` (optional): `0.5 - 2.0` (default `1.0`)
  - `mode` (optional): `standard | studio | pro` (default `standard`)

Response (202):
```json
{
  "status": "accepted",
  "job_id": 301,
  "job_type": "add_background",
  "message": "Background mix started"
}
```

---

## 4) Endpoint: Reel Mode

### `POST /voice/reel/`
One-click pipeline (reel-first):
adaptive analyze -> clean/extract -> background mix -> loudness normalize -> export.

Request:
- Content-Type: `multipart/form-data`
- Fields:
  - `file` (required)
  - `bg` (optional): `rain | cafe | ocean | forest | street`
  - `bg_volume` (optional, default `0.15`)
  - `mode` (optional): `standard | studio | pro` (default `standard`)
  - `target_lufs` (optional, default `-16`)
  - `preset_id` (optional): `podcast | rain_reel | cafe_talk | viral_reel`
  - `adaptive` (optional bool, default `true`): allow server to auto-select processing profile
  - `output_format` (optional): `wav | mp3 | m4a` (default `wav`)

Response (202):
```json
{
  "status": "accepted",
  "job_id": 401,
  "job_type": "reel",
  "message": "Reel mode job started"
}
```

---

## 5) Endpoint: Video Process

### `POST /voice/video/process/`
Extracts audio from video, processes per selected mode, remuxes output.

Request:
- Content-Type: `multipart/form-data`
- Fields:
  - `file` (required): video (`mp4`, `mov`, `mkv`)
  - `job_type` (optional): `video_clean | video_extract_voice | video_reel` (default `video_reel`)
  - `mode` (optional): `standard | studio | pro` (default `standard`)
  - `bg` (optional for reel)
  - `bg_volume` (optional)
  - `target_lufs` (optional, default `-16`)
  - `preset_id` (optional): `podcast | rain_reel | cafe_talk | viral_reel`
  - `adaptive` (optional bool, default `true`)

Response (202):
```json
{
  "status": "accepted",
  "job_id": 501,
  "job_type": "video_reel",
  "message": "Video processing started"
}
```

Status completed payload should include:
- `output_video_url`
- optionally `output_audio_url`

---

## 6) Endpoint: Background Catalog

### `GET /voice/backgrounds/`
Returns list of available background tracks and metadata.

Response (200):
```json
{
  "status": "ok",
  "backgrounds": [
    {
      "id": "rain",
      "label": "Rain",
      "preview_url": "/media/backgrounds/previews/rain_20s.mp3",
      "default_volume": 0.15
    },
    {
      "id": "cafe",
      "label": "Cafe",
      "preview_url": "/media/backgrounds/previews/cafe_20s.mp3",
      "default_volume": 0.15
    }
  ]
}
```

---

## 7) Health Endpoint Extensions

### `GET /voice/health/`
Extend payload with feature capability flags:

```json
{
  "status": "ok",
  "service": "BolSaaf Voice Cleaning API",
  "version": "2.2.0",
  "system_requirements": {
    "ffmpeg": true,
    "deepfilternet": true,
    "demucs": true,
    "cuda": false
  },
  "available_modes": ["basic", "standard", "studio", "pro"],
  "capabilities": {
    "adaptive_runtime": true,
    "quality_retry": true,
    "dry_mix_guard": true,
    "loudness_floor_guard": true
  },
  "available_job_types": [
    "clean",
    "extract_voice",
    "add_background",
    "reel",
    "video_clean",
    "video_extract_voice",
    "video_reel"
  ]
}
```

---

## 8) Validation Rules (Recommended)

- Max audio upload size: `20MB` (for reel/video-heavy use cases)
- Max video size: `100MB` (or async queue only)
- Audio formats: `wav/mp3/m4a/aac/ogg/flac`
- Video formats: `mp4/mov/mkv`
- `bg_volume` hard clamp: `0.0..1.0`
- `target_lufs` clamp: `-24..-12`

---

## 9) Kotlin Client Mapping

Suggested intents in app (current):
- `Reel ★` -> `POST /voice/reel/` (primary path)
- `Quick` -> `POST /voice/clean/`
- `Extract` -> `POST /voice/extract_voice/`
- `BG` -> `POST /voice/add_background/`
- `Video` -> `POST /voice/video/process/`

Client behavior now includes:
- adaptive analysis preview + confidence
- runtime mode selection for cloud clean when adaptive enabled
- quality fallback chain (`retry` -> `dry mix` -> `loudness floor`)

All async endpoints:
1. create job
2. poll `GET /voice/status/{job_id}/`
3. download `output_audio_url` or `output_video_url`

---

## 10) Rollout Strategy

1. Ship backend reel pipeline full chain (including `bg` + loudness + optional video export) with compatibility keys intact.
2. Extend status payload with optional `adaptive` and `quality_guard` sections.
3. Keep fallback parsing for old keys (`state`, `mode`) for at least one release.
4. Remove legacy keys only after app adoption is complete and telemetry is stable.

