"""Download audio from URL (yt-dlp) then run existing Demucs extract job."""
import io
import logging
import os
import shlex
import shutil
import struct
import subprocess
import tempfile
import threading
import wave
from pathlib import Path

from django.core.files import File

from apps.voice.models import AudioFile
from apps.voice.services.demucs_extract import run_extract_voice_job

logger = logging.getLogger(__name__)

# Max parallel URL→download→Demucs pipelines (default 2). Third request waits in queue.
_PARALLEL = max(1, min(32, int(os.environ.get("URL_EXTRACT_MAX_PARALLEL", "2"))))
_url_extract_semaphore = threading.Semaphore(_PARALLEL)

ALLOWED_HOST_FRAGMENTS = (
    "youtube.com",
    "youtu.be",
    "instagram.com",
    "tiktok.com",
    "vm.tiktok.com",
    "www.tiktok.com",
    "m.youtube.com",
)


def is_allowed_media_url(url: str) -> bool:
    u = (url or "").strip().lower()
    if not u.startswith("https://"):
        return False
    if any(h in u for h in ALLOWED_HOST_FRAGMENTS):
        return True
    # Direct media links (works when CDN is not bot-blocked)
    for ext in (".mp3", ".wav", ".m4a", ".aac", ".ogg", ".flac"):
        if u.split("?", 1)[0].endswith(ext):
            return True
    return False


def minimal_silence_wav_bytes(duration_sec: float = 0.3, sample_rate: int = 48000) -> bytes:
    n_samples = int(duration_sec * sample_rate)
    frames = b"".join(struct.pack("<h", 0) for _ in range(n_samples))
    buf = io.BytesIO()
    with wave.open(buf, "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(sample_rate)
        w.writeframes(frames)
    return buf.getvalue()


def run_extract_from_url_job(job_id: int, source_url: str, mode: str) -> None:
    """Waits for a free slot if URL_EXTRACT_MAX_PARALLEL jobs are already running."""
    _url_extract_semaphore.acquire()
    try:
        _run_extract_from_url_job_locked(job_id, source_url, mode)
    finally:
        _url_extract_semaphore.release()


def _is_youtube_url(url: str) -> bool:
    u = (url or "").lower()
    return "youtube.com" in u or "youtu.be" in u


def _youtube_player_client_retries() -> list[list[str]]:
    """Extra yt-dlp args per attempt after the default (no override) try."""
    raw = os.environ.get("URL_EXTRACT_YOUTUBE_CLIENTS", "").strip()
    if raw:
        clients = [c.strip() for c in raw.split(",") if c.strip()]
    else:
        # Order: mobile-ish clients first; YouTube often blocks plain `web` on DC IPs.
        clients = ["android", "ios", "mweb", "web_safari", "web", "tv_simply"]
    return [["--extractor-args", f"youtube:player_client={c}"] for c in clients]


def _run_extract_from_url_job_locked(job_id: int, source_url: str, mode: str) -> None:
    job = AudioFile.objects.get(id=job_id)
    tmpdir = tempfile.mkdtemp(prefix=f"ytdl_job{job_id}_")
    try:
        outtmpl = str(Path(tmpdir) / "src.%(ext)s")
        cookies = os.environ.get("YT_DLP_COOKIES", "").strip()
        extra_global: list[str] = []
        raw_extra = os.environ.get("YT_DLP_EXTRA_ARGS", "").strip()
        if raw_extra:
            try:
                extra_global = shlex.split(raw_extra)
            except ValueError as e:
                logger.warning("YT_DLP_EXTRA_ARGS parse failed job=%s: %s", job_id, e)

        attempt_suffixes: list[list[str]] = [[]]
        if _is_youtube_url(source_url):
            attempt_suffixes.extend(_youtube_player_client_retries())

        timeout_sec = max(60, min(3600, int(os.environ.get("URL_EXTRACT_YTDLP_TIMEOUT", "600"))))
        proc = None
        last_err_text = ""

        for idx, suffix in enumerate(attempt_suffixes):
            cmd: list[str] = ["yt-dlp"]
            if cookies and os.path.isfile(cookies):
                cmd.extend(["--cookies", cookies])
                if idx == 0:
                    logger.info("yt-dlp using cookies file for job=%s", job_id)
            cmd.extend(
                [
                    "-x",
                    "--audio-format",
                    "wav",
                    "--no-playlist",
                    "--format-sort",
                    "+size,+br",
                    "-o",
                    outtmpl,
                    "--no-warnings",
                ]
            )
            cmd.extend(extra_global)
            cmd.extend(suffix)
            cmd.append(source_url)
            label = "default" if not suffix else " ".join(suffix)
            logger.info(
                "yt-dlp attempt %s/%s job=%s mode=%s url=%s",
                idx + 1,
                len(attempt_suffixes),
                job_id,
                label,
                source_url[:80],
            )
            proc = subprocess.run(cmd, capture_output=True, text=True, timeout=timeout_sec)
            if proc.returncode == 0:
                break
            last_err_text = (proc.stderr or proc.stdout or "").strip()
            logger.warning(
                "yt-dlp attempt %s failed job=%s rc=%s err=%s",
                idx + 1,
                job_id,
                proc.returncode,
                last_err_text[:500],
            )
            for p in Path(tmpdir).glob("*"):
                try:
                    if p.is_file():
                        p.unlink()
                    elif p.is_dir():
                        shutil.rmtree(p, ignore_errors=True)
                except OSError:
                    pass

        if proc is None or proc.returncode != 0:
            raise RuntimeError(
                f"yt-dlp exit {getattr(proc, 'returncode', -1)} after {len(attempt_suffixes)} attempt(s): "
                f"{last_err_text[:1500]}"
            )
        candidates = sorted(Path(tmpdir).glob("src.*"), key=lambda p: p.stat().st_mtime, reverse=True)
        if not candidates:
            raise RuntimeError("yt-dlp produced no audio file")
        src_path = candidates[0]
        with open(src_path, "rb") as fh:
            job.original_file.save(f"url_source_{job_id}{src_path.suffix.lower()}", File(fh), save=True)
        job.refresh_from_db()
        run_extract_voice_job(job_id, mode)
    except Exception as e:
        logger.exception("extract_from_url failed job=%s", job_id)
        try:
            job = AudioFile.objects.get(id=job_id)
            job.status = "failed"
            job.error_message = (f"URL extract failed: {e}")[:2000]
            job.save(update_fields=["status", "error_message", "updated_at"])
        except Exception:
            pass
    finally:
        shutil.rmtree(tmpdir, ignore_errors=True)
