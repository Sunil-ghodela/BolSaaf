# API: Extract voice from URL (`/voice/extract_from_url/`)

Used by the **Extract** tab in the Android app (`VoiceApiPhase2Client.extractVoiceFromUrl`).

## Request

- **Method:** `POST`
- **URL:** `https://<host>/voice/extract_from_url/`
- **Content-Type:** `application/json; charset=utf-8`
- **Body (JSON):**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `source_url` | string | yes | HTTPS link (YouTube, Instagram Reels, TikTok, etc.) |
| `mode` | string | no | Same semantics as file-based extract (`standard`, `studio`, `pro`, …) |

Example:

```json
{"source_url": "https://www.youtube.com/watch?v=dQw4w9WgXcQ", "mode": "studio"}
```

## Success response

Same envelope as other async jobs (align with `extract_voice/`):

```json
{
  "job_id": 123,
  "job_type": "extract_from_url",
  "message": "Job accepted"
}
```

## Status / output

Poll existing **`GET /voice/status/<job_id>/`** until `status` is `completed` or `failed`.

On success, reuse the same fields as `extract_voice`:

- `output_audio_url` or `cleaned_url` — WAV (or agreed format) download URL.

## Server implementation notes

1. **Validate** URL host against an allowlist (YouTube, youtu.be, Instagram, TikTok, …).
2. **Download audio** in a worker (recommended: `yt-dlp` with `-f bestaudio` / `-x --audio-format wav`), writing to a temp file under a per-job directory.
3. **Run** the same Demucs / denoise pipeline as `POST /voice/extract_voice/` on that WAV.
4. **Publish** output to the same storage + job model as existing extract jobs so `status/` and media URLs stay consistent.

Legal / ToS: only enable hosts you are licensed to use; rate-limit and log abuse.

## YouTube from datacenter IPs

Many VPS IPs get **“Sign in to confirm you’re not a bot”** from YouTube. Mitigations:

- **`yt-dlp --cookies`** — **Haan, kaam karti hai** jab tum **browser se export** ki hui Netscape-format cookies file server par rakho, aur env **`YT_DLP_COOKIES`** us file ka **absolute path** ho (readable by the Django process user). Phir server code automatically `yt-dlp --cookies <path>` lagata hai.
- Cookies **regularly refresh** karni pad sakti hain (YouTube session expire).
- Alternative: residential proxy / separate worker IP.

**Direct `https://…/*.mp3` (or wav/m4a/…)** URLs are also allowed and often work without cookies.

## Server env (optional)

| Variable | Default | Meaning |
|----------|---------|---------|
| `YT_DLP_COOKIES` | *(empty)* | Path to `cookies.txt` for `yt-dlp --cookies` |
| `URL_EXTRACT_MAX_PARALLEL` | `2` | Max concurrent URL-extract pipelines (download + Demucs) |
| `URL_EXTRACT_YTDLP_TIMEOUT` | `600` | `yt-dlp` subprocess timeout (seconds) |
| `URL_EXTRACT_YOUTUBE_CLIENTS` | *(see code)* | Comma-separated `player_client` values; server retries YouTube with each after the default attempt |
| `YT_DLP_EXTRA_ARGS` | *(empty)* | Extra arguments for every attempt (shell-style, e.g. `--verbose`) |

Example (before starting Django / `runserver`):

```bash
export YT_DLP_COOKIES=/var/www/simplelms/backend/secrets/youtube_cookies.txt
export URL_EXTRACT_MAX_PARALLEL=2
# Optional: override client retry order (empty = built-in default list in `url_extract.py`)
# export URL_EXTRACT_YOUTUBE_CLIENTS=android,ios,mweb,web_safari
```

Keep **`yt-dlp` updated** on the worker (`pip3 install -U yt-dlp`); YouTube changes often break older versions.

## Deployed server (2026-04-11)

- Host: `77.237.234.45` (BolSaaf `simplelms` Django `runserver 0.0.0.0:8000`).
- Added: `apps/voice/services/url_extract.py`, `ExtractFromUrlView` in `views.py`, route `voice/extract_from_url/`.
- System package: `pip3 install yt-dlp`.

## Deploy checklist

- [ ] Add Django (or FastAPI) route `extract_from_url/`
- [ ] Install `yt-dlp` (or equivalent) on the worker host
- [ ] Wire Celery (or existing job runner) so the HTTP handler returns quickly with `job_id`
- [ ] Confirm `status/` returns `output_audio_url` when completed
