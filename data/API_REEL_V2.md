# API: Reel V2 Contract (`/voice/reel/create`)

Version: draft-v1 (2026-04-13)

This contract defines the one-tap Reel V2 flow that returns three output variants from one job.

---

## Base URL

`https://shadowselfwork.com/voice/`

---

## 1) Create Reel Job

`POST /voice/reel/create`

Content-Type: `application/json`

### Request

```json
{
  "input_file_id": "uuid-or-int",
  "source_type": "audio",
  "requested_variants": ["clean_only", "with_bg", "viral_boosted"],
  "preset_id": "podcast",
  "background_preset": "cafe",
  "target_lufs": -16,
  "include_video": true
}
```

### Response (202 Accepted)

```json
{
  "status": "accepted",
  "reel_job_id": "uuid-or-int",
  "estimated_time_sec": 45,
  "current_stage": "analyzing",
  "overall_progress": 0,
  "stages": [
    {"stage": "analyzing", "progress": 0},
    {"stage": "cleaning", "progress": 0},
    {"stage": "extracting", "progress": 0},
    {"stage": "mixing", "progress": 0},
    {"stage": "encoding", "progress": 0}
  ]
}
```

### Validation errors (400)

- invalid `requested_variants`
- unsupported `source_type`
- file not found
- target LUFS out of allowed range

---

## 2) Reel Job Status

`GET /voice/reel/{reel_job_id}/status`

### Response (processing)

```json
{
  "reel_job_id": "uuid-or-int",
  "status": "processing",
  "current_stage": "mixing",
  "overall_progress": 63,
  "stage_progress": {
    "analyzing": 100,
    "cleaning": 100,
    "extracting": 100,
    "mixing": 45,
    "encoding": 0
  },
  "analysis_summary": {
    "duration_sec": 44.2,
    "rms_dbfs": -23.4,
    "noise_floor_db": -46.0,
    "silence_ratio": 0.11,
    "suggested_preset": "podcast"
  },
  "outputs": {
    "clean_only": null,
    "with_bg": null,
    "viral_boosted": null
  },
  "created_at": "2026-04-13T09:20:00Z",
  "updated_at": "2026-04-13T09:20:22Z",
  "expires_at": "2026-05-13T09:20:00Z"
}
```

### Response (completed)

```json
{
  "reel_job_id": "uuid-or-int",
  "status": "completed",
  "current_stage": "completed",
  "overall_progress": 100,
  "stage_progress": {
    "analyzing": 100,
    "cleaning": 100,
    "extracting": 100,
    "mixing": 100,
    "encoding": 100
  },
  "outputs": {
    "clean_only": {
      "audio_url": "/voice/reel/123/outputs/clean_only/audio",
      "video_url": "/voice/reel/123/outputs/clean_only/video",
      "duration_sec": 44.0,
      "loudness_lufs": -16.2
    },
    "with_bg": {
      "audio_url": "/voice/reel/123/outputs/with_bg/audio",
      "video_url": "/voice/reel/123/outputs/with_bg/video",
      "duration_sec": 44.0,
      "loudness_lufs": -16.0
    },
    "viral_boosted": {
      "audio_url": "/voice/reel/123/outputs/viral_boosted/audio",
      "video_url": "/voice/reel/123/outputs/viral_boosted/video",
      "duration_sec": 43.4,
      "loudness_lufs": -16.1
    }
  },
  "created_at": "2026-04-13T09:20:00Z",
  "updated_at": "2026-04-13T09:20:49Z",
  "expires_at": "2026-05-13T09:20:00Z"
}
```

### Failed response

```json
{
  "reel_job_id": "uuid-or-int",
  "status": "failed",
  "current_stage": "encoding",
  "overall_progress": 82,
  "error_code": "ffmpeg_encode_failed",
  "error_message": "Video encode timeout",
  "updated_at": "2026-04-13T09:21:05Z"
}
```

---

## 3) Output Download (optional if not using direct urls)

- `GET /voice/reel/{reel_job_id}/outputs/{variant}/audio`
- `GET /voice/reel/{reel_job_id}/outputs/{variant}/video`

Where `variant` in: `clean_only`, `with_bg`, `viral_boosted`

---

## 4) Backward compatibility

- Keep existing `POST /voice/reel/` active.
- Old clients should continue polling existing `/voice/status/{job_id}/`.
- New clients should migrate to Reel V2 status for multi-output metadata.

---

## 5) Android Polling Guidance

- Poll every 1.5–2.0 seconds
- Timeout at 180 seconds (or product-defined)
- Render per-stage progress from `stage_progress`
- Show only ready variants (non-null URLs)

