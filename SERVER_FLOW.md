# BolSaaf Server Flow

**Purpose:** single-page map of how a request from the Android app lands on a pipeline on the production host. Use this before touching the backend so you don't have to re-SSH and re-discover. Last verified: 2026-04-14 (post gunicorn + Celery migration).

---

## 1. Host

| Field | Value |
|---|---|
| SSH | `ssh root@77.237.234.45` |
| OS | Ubuntu 22.04.4 LTS |
| RAM / CPU / Disk | 5.8 GiB / load ~0.6 / `/` 286 GB free, no swap |
| Uptime | ~87 days (Jan 2026) |

---

## 2. Reverse proxy — nginx

- Listen: `:80` (redirect → 443) + `:443` (TLS via Certbot, cert is `retreatdesk.com/fullchain.pem` — shared across vhosts on this box).
- Vhost file: `/etc/nginx/sites-enabled/selfshadowwork` (duplicate `.bak` and conflicting IP entries removed 2026-04-14; `nginx -t` now clean).
- Relevant vhost: `server_name shadowselfwork.com www.shadowselfwork.com;`
  - `location /voice/` → `proxy_pass http://unix:/run/voice-gunicorn.sock:/voice/;` with `proxy_read_timeout 300s`, `proxy_connect_timeout 300s`, `client_max_body_size 100M`.
  - `location ^~ /media/` → `alias /var/www/simplelms/backend/media/;` (serves cleaned output files directly from disk, bypasses Django).
  - `location /` → `proxy_pass http://localhost:3000;` (a Next.js app for the public site; irrelevant to the mobile API).
- Other vhost: `admin.shadowselfwork.com` → proxies `/` to `:8000`. Same Django app, different hostname.

---

## 3. App server — gunicorn (systemd) + Celery worker (systemd)

As of 2026-04-14 the voice API runs under three systemd units, all `enabled`:

| Unit | Purpose | Path |
|---|---|---|
| `voice-gunicorn.socket` | Unix socket listener (root:www-data 0660) | `/run/voice-gunicorn.sock` |
| `voice-gunicorn.service` | WSGI app (3 sync workers, 300s timeout) | `config.wsgi:application` |
| `voice-celery.service` | Celery worker (2 child processes, Redis broker) | `config` app, `voice.*` tasks |

- Project root: `/var/www/simplelms/backend`
- Virtualenv: `/var/www/simplelms/backend/.venv`
- Settings module: `config.settings`
- Celery app: `config.celery:app` (autodiscovers `apps.voice.tasks`)
- Logs:
  - `/var/log/voice-gunicorn.access.log` + `/var/log/voice-gunicorn.error.log`
  - `/var/log/voice-celery.log`
- History: prior to 2026-04-14 this was `python manage.py runserver 0.0.0.0:8000 --noreload` started via `nohup`, with async jobs dispatched as `threading.Thread(daemon=True)`. That was replaced with gunicorn (task #16) + Celery (task #17) to fix concurrency/ durability problems. The old runserver unit `voice-runserver.service` no longer exists; if you need to roll back, see §9.

### Installed heavy deps (verified in .venv)

- `demucs` 4.0.1
- `deepfilternet` 0.5.6 (+ `DeepFilterLib` 0.5.6)
- `torch` 2.2.2, `torchaudio` 2.2.2 (CPU; `cuda=false` in health)
- `soundfile`, `numpy`
- `ffmpeg` 4.4.2 at `/usr/bin/ffmpeg`
- Native RNNoise accessed via Python wrapper in `apps/voice/services/rnnoise.py`

---

## 4. Django app layout — `apps/voice/`

```
apps/voice/
├── urls.py                ← routes under /voice/
├── views.py               ← DRF APIViews (921 lines; has duplicate classes — task #19)
├── auth_views.py          ← /voice/auth/{register,login,logout,profile}
├── models.py              ← AudioFile, VoiceFeedback, ExtractVoiceTuningState, ReelJob, ReelOutput, VoiceUser
├── serializers.py
├── admin.py
├── migrations/
├── management/            ← custom manage.py commands
└── services/
    ├── studio_pipeline.py       ← Phase-5 orchestrator (ffmpeg pre → denoise → DSP → polish)
    ├── rnnoise.py               ← RNNoise wrapper (fast path)
    ├── deepfilter.py            ← DeepFilterNet wrapper (studio/pro path)
    ├── dsp_enhance.py           ← EQ/compressor/saturation/limiter/normaliser
    ├── ffmpeg_polish.py         ← -16 LUFS loudnorm + export
    ├── ffmpeg_utils.py          ← shared ffmpeg helpers
    ├── demucs_extract.py        ← Demucs stem separation (extract_voice job)
    ├── background_mix.py        ← add_background job
    ├── reel_mode.py             ← reel job (v1 chain)
    ├── reel_v2.py               ← reel job (v2, multi-variant, stage-progress)
    ├── video_process.py         ← video_reel job
    ├── url_extract.py           ← extract_from_url (semaphore-limited parallelism)
    ├── extract_feedback_tuning.py  ← adaptive tuning from user feedback
    ├── processor.py
    └── utils.py
```

---

## 5. URL table — `apps/voice/urls.py`

All paths below are served at `https://shadowselfwork.com/voice/…`.

| Method | Path | View | Notes |
|---|---|---|---|
| POST | `/clean/` | `CleanAudioView` | Sync-ish: runs pipeline in-process, returns `cleaned_url` + `job_id`. Used by `VoiceCleaningApi.kt`. Max 5 MB. |
| GET | `/status/<job_id>/` | `JobStatusView` | Returns both new keys (`status`, `processing_mode`) **and** legacy (`state`, `mode`, `processing_time`). Infers `job_type` from filename prefix. |
| GET | `/health/` | `HealthCheckView` | Reports `version: "2.0.0"` (stale; contract says 2.2.0; missing `demucs` capability key — task #6). |
| GET | `/backgrounds/` | `BackgroundCatalogView` | Lists `/media/backgrounds/` files. |
| GET | `/download/<job_id>/` | `CleanedFileDownloadView` | Serves cleaned file as attachment. |
| POST | `/extract_voice/` | `ExtractVoiceView` | Async; Demucs. |
| POST | `/extract_from_url/` | `ExtractFromUrlView` | Async; semaphore-limited. |
| POST | `/add_background/` | `AddBackgroundView` | Async; mixes voice + bg. |
| POST | `/reel/` | `ReelModeView` | Async; v1 reel chain. |
| POST | `/reel/create/` | `ReelCreateV2View` | Async; v2 multi-variant. |
| GET | `/reel/<reel_job_id>/status/` | `ReelStatusV2View` | Stage-progress per `STAGES = ["analyzing","cleaning","extracting","mixing","encoding"]`. |
| POST | `/video/process/` | `VideoProcessView` | Async; extracts audio, processes, remuxes. |
| POST | `/feedback/` | `VoiceFeedbackView` | User feedback (feeds `extract_feedback_tuning`). |
| POST | `/auth/{register,login,logout}/` | `auth_views` | Email-password, `VoiceUser` model. |
| GET  | `/auth/profile/` | `UserProfileView` | |

---

## 6. Job dispatch — Celery + Redis

Every async endpoint dispatches to a Celery task (Redis broker at `localhost:6379/0`, no result backend — status is tracked via the `AudioFile` / `ReelJob` models):

```python
# views.py
voice_tasks.extract_voice_task.delay(job.id, mode)
return Response({"status": "accepted", "job_id": job.id, "job_type": "extract_voice"}, status=202)
```

Tasks are thin wrappers in `apps/voice/tasks.py`:

| Task | Wraps | Bound from |
|---|---|---|
| `voice.extract_voice` | `services.demucs_extract.run_extract_voice_job` | `POST /voice/extract_voice/` |
| `voice.add_background` | `services.background_mix.run_add_background_job` | `POST /voice/add_background/` |
| `voice.reel` | `services.reel_mode.run_reel_job` | `POST /voice/reel/` |
| `voice.video_reel` | `services.video_process.run_video_reel_job` | `POST /voice/video/process/` |
| `voice.reel_v2` | `services.reel_v2.run_reel_v2_job` | `POST /voice/reel/create/` |
| `voice.extract_from_url` | `services.url_extract.run_extract_from_url_job` | `POST /voice/extract_from_url/` |

Celery settings in `config/settings.py` (bottom of file):
- `CELERY_BROKER_URL = "redis://localhost:6379/0"`
- `CELERY_TASK_ACKS_LATE = True` + `CELERY_WORKER_PREFETCH_MULTIPLIER = 1` (so a worker crash re-queues the in-flight job instead of losing it).
- `CELERY_RESULT_BACKEND = None`.

Observability:
- `celery -A config inspect registered` → list registered tasks.
- `celery -A config inspect active` → in-flight jobs per worker.
- `tail -F /var/log/voice-celery.log` → per-task trace incl. exceptions.

The feedback-view `_tune` closure (views.py ~line 697) intentionally stays on `threading.Thread` — it's a tiny local function, not a heavy job, and Celery would add deployment overhead without a clear win.

---

## 7. Data model (sync paths — who writes what)

- `AudioFile` — primary job row; one row per `POST /voice/<endpoint>/`. Fields: `original_file`, `cleaned_file`, `status` (pending/processing/completed/failed), `duration`, `processing_time`, `processing_mode`, `error_message`, `created_at`, `updated_at`, `original_size`, `cleaned_size`. Deleting the row also deletes both files from disk (see `models.py:delete()`).
- `ReelJob` + `ReelOutput` — v2 reel flow. `ReelJob` tracks `current_stage`, `overall_progress`, `stage_progress` (JSON per-stage %). Each variant result is a `ReelOutput` row pointing at an `AudioFile` for audio/video.
- `VoiceFeedback` — mobile feedback submissions; drives `extract_feedback_tuning`.
- `ExtractVoiceTuningState` — singleton (pk=1) holding `vocals_dry_ratio` adapted from aggregated feedback.
- `VoiceUser` — email+password (SHA-256 hash) auth for the mobile `auth_views`.

---

## 8. Storage

- Upload dir: `/var/www/simplelms/backend/media/uploads/`
- Cleaned dir: `/var/www/simplelms/backend/media/cleaned/` (file name prefix tells `JobStatusView` the `job_type`: `clean_*`, `extract_*`, `mix_*`, `reel_*`, `video_*`).
- Backgrounds: `/var/www/simplelms/backend/media/backgrounds/` (~34 MB: `cafe.mp3`, `forest.mp3`, `ocean.mp3`, `rain.mp3`, `street.mp3` + `previews/` for the 20s snippets the catalog endpoint returns).
- Nginx serves `/media/*` directly; Django only generates URLs.
- DB: **Postgres 14** at `localhost:5432`, database `simplelms`, user `simplelms`. Engine `django.db.backends.postgresql_psycopg2`; psycopg2-binary 2.9.9 in the voice venv. Setting is gated by `USE_SQLITE` env toggle (defaults to False). A stale `db.sqlite3` file in the project root is a leftover from dev bootstrapping and is **not in use** — safe to ignore.

---

## 9. Common operations

```bash
# SSH in
ssh root@77.237.234.45

# tail voice API logs
tail -F /var/log/voice-gunicorn.access.log
tail -F /var/log/voice-gunicorn.error.log
tail -F /var/log/voice-celery.log

# filter real voice traffic
grep -E "/voice/(clean|reel|extract_voice|add_background|video)" \
  /var/log/voice-gunicorn.access.log

# restart / reload
systemctl reload voice-gunicorn.service       # graceful (HUP) — picks up code changes
systemctl restart voice-gunicorn.service      # hard restart
systemctl restart voice-celery.service        # restart queue worker (drops in-flight but acks_late → re-queued)

# Celery introspection
cd /var/www/simplelms/backend && .venv/bin/celery -A config inspect registered
cd /var/www/simplelms/backend && .venv/bin/celery -A config inspect active

# run a migration
cd /var/www/simplelms/backend && .venv/bin/python manage.py migrate

# open a Django shell
cd /var/www/simplelms/backend && .venv/bin/python manage.py shell

# Rollback to runserver (EMERGENCY only)
# 1. Revert nginx:
#    sed -i 's|proxy_pass http://unix:/run/voice-gunicorn.sock:/voice/;|proxy_pass http://127.0.0.1:8000/voice/;|' /etc/nginx/sites-enabled/selfshadowwork
#    nginx -t && systemctl reload nginx
# 2. Stop gunicorn + celery:
#    systemctl stop voice-gunicorn.service voice-celery.service
# 3. Start runserver via systemd-run:
#    systemd-run --unit=voice-runserver --working-directory=/var/www/simplelms/backend \
#      /var/www/simplelms/backend/.venv/bin/python manage.py runserver 0.0.0.0:8000 --noreload
# 4. Revert views.py from /root/voice_backups_20260414/views.py.pre_celery if needed.

# inspect a stuck job
.venv/bin/python manage.py shell -c \
  "from apps.voice.models import AudioFile; \
   import django.utils.timezone as tz; \
   print(AudioFile.objects.filter(status__in=['pending','processing']).values_list('id','status','created_at')[:20])"

# nginx
nginx -t && systemctl reload nginx
tail -F /var/log/nginx/error.log

# backgrounds
ls /var/www/simplelms/backend/media/backgrounds/
```

---

## 10. Request lifecycle

**Sync path — `POST /voice/clean/`** (small files, ≤5 MB):

```
Android (VoiceCleaningApi.kt)
  └── multipart POST https://shadowselfwork.com/voice/clean/
        └── nginx :443 (TLS, client_max_body_size 100M)
              └── proxy_pass unix:/run/voice-gunicorn.sock
                    └── gunicorn worker → CleanAudioView.post()
                          ├── save upload → AudioFile.original_file
                          ├── StudioPipeline(mode).run()
                          │     ├── ffmpeg preprocess (to 48k mono wav)
                          │     ├── RNNoise | DeepFilterNet (mode-dependent)
                          │     ├── DSP (EQ/compressor/saturation/limiter/norm)
                          │     └── ffmpeg polish (-16 LUFS loudnorm + encode)
                          ├── save cleaned → AudioFile.cleaned_file
                          └── 200 {cleaned_url, job_id, duration, processing_time, mode}
```

Android resolves `cleaned_url` against `siteOrigin` (`https://shadowselfwork.com`) and downloads via nginx `/media/` alias.

**Async path — `POST /voice/{extract_voice,add_background,reel,reel/create,video/process,extract_from_url}/`**:

```
Android → nginx → gunicorn worker → View.post()
    ├── save upload → AudioFile.original_file  (status=pending)
    ├── voice_tasks.<task>.delay(job.id, …)    # pushes to Redis broker
    └── return 202 {accepted, job_id, job_type}

                                  Redis broker (0)
                                       │
                                       ▼
                             voice-celery.service (2 workers)
                                       │
                                       ▼
                          services.run_<pipeline>_job(job_id, …)
                             (pulls AudioFile, runs pipeline,
                              writes cleaned_file, status=completed
                              or error_message + status=failed)

Android polls GET /voice/status/<job_id>/ (or /voice/reel/<id>/status/ for v2)
until status: completed and output_audio_url / output_video_url is set.
```

Because `CELERY_TASK_ACKS_LATE=True`, a worker crash during execution re-queues the job instead of losing it. Because `CELERY_WORKER_PREFETCH_MULTIPLIER=1`, workers pull at most one heavy job at a time — important for CPU-bound Demucs.

---

## 11. Known debt

| Area | Item | Status |
|---|---|---|
| backend | runserver → gunicorn+systemd | ✅ done 2026-04-14 (#16) |
| backend | threading.Thread → Celery+Redis | ✅ done 2026-04-14 (#17) |
| backend | health endpoint v2.2 + `demucs` key | ✅ done 2026-04-14 (#6) |
| backend | dedupe + fix scaffold ExtractVoice/AddBackground | ✅ done 2026-04-14 (#19) |
| nginx | conflicting `server_name` warnings | ✅ done 2026-04-14 (#5) |
| mobile | end-to-end verify client→server flow | pending (#18) |
| backend | ~~SQLite → Postgres~~ — already on Postgres, was never SQLite in production | resolved |
| backend | move dead first-block ReelMode/VideoProcess/BackgroundCatalog in views.py — harmless but confusing | new, not yet ticketed |
| backend | ALLOWED_HOSTS in `config/settings.py` is missing `shadowselfwork.com` / `www.shadowselfwork.com` (works today because DEBUG=True bypasses the check; flip DEBUG to False and it'll 400) | new, not yet ticketed |
