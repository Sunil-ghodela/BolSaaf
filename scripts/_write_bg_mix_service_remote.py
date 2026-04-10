from pathlib import Path

p = Path("/var/www/simplelms/backend/apps/voice/services/background_mix.py")
p.write_text(
    """import os
import shutil
import subprocess
from pathlib import Path

from django.conf import settings

from apps.voice.models import AudioFile


BG_MAP = {
    "rain": "rain.mp3",
    "cafe": "cafe.mp3",
    "ocean": "ocean.mp3",
    "forest": "forest.mp3",
    "street": "street.mp3",
}


def run_add_background_job(job_id: int, bg: str, bg_volume: float = 0.15) -> None:
    job = AudioFile.objects.get(id=job_id)
    job.status = "processing"
    job.error_message = ""
    job.save(update_fields=["status", "error_message", "updated_at"])

    input_path = Path(job.original_file.path)
    media_root = Path(settings.MEDIA_ROOT)
    bg_file = media_root / "backgrounds" / BG_MAP.get(bg, "")
    if not bg_file.exists():
        job.status = "failed"
        job.error_message = f"Background file not found: {bg}"
        job.save(update_fields=["status", "error_message", "updated_at"])
        return

    cleaned_dir = media_root / "cleaned"
    os.makedirs(cleaned_dir, exist_ok=True)
    output_name = f"mix_{job.id}_{input_path.stem}_{bg}.wav"
    output_path = cleaned_dir / output_name

    # ffmpeg mix:
    # - normalize sample rate/channels
    # - loop bg if shorter
    # - voice full volume + bg at requested volume
    cmd = [
        "ffmpeg",
        "-y",
        "-i",
        str(input_path),
        "-stream_loop",
        "-1",
        "-i",
        str(bg_file),
        "-filter_complex",
        f"[0:a]aresample=48000,pan=mono|c0=c0[a0];"
        f"[1:a]aresample=48000,pan=mono|c0=c0,volume={max(0.0, min(bg_volume, 1.0))}[a1];"
        f"[a0][a1]amix=inputs=2:duration=first:dropout_transition=2[m]",
        "-map",
        "[m]",
        "-c:a",
        "pcm_s16le",
        str(output_path),
    ]

    try:
        subprocess.run(cmd, check=True, capture_output=True, text=True)
        with open(output_path, "rb") as f:
            job.cleaned_file.save(output_name, f, save=False)
        job.status = "completed"
        try:
            import soundfile as sf
            audio, sr = sf.read(str(output_path))
            job.duration = len(audio) / sr
        except Exception:
            pass
        try:
            job.cleaned_size = output_path.stat().st_size
        except Exception:
            pass
        job.save()
    except Exception as e:
        job.status = "failed"
        job.error_message = f"Add background failed: {e}"
        job.save(update_fields=["status", "error_message", "updated_at"])
    finally:
        # keep output file; cleanup temp files if any in future
        pass
"""
)
print("written", p)
