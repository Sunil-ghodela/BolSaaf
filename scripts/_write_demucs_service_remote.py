from pathlib import Path

p = Path("/var/www/simplelms/backend/apps/voice/services/demucs_extract.py")
p.write_text(
    """import os
import shutil
import subprocess
from pathlib import Path

from django.conf import settings

from apps.voice.models import AudioFile


def _resolve_vocals_file(base_out_dir: Path, input_stem: str) -> Path:
    # Demucs typically writes: <out>/<model>/<stem>/vocals.wav
    if not base_out_dir.exists():
        raise FileNotFoundError(f"Demucs output directory not found: {base_out_dir}")
    candidates = list(base_out_dir.rglob(f"{input_stem}/vocals.wav"))
    if candidates:
        return candidates[0]
    # fallback: first vocals.wav anywhere
    any_vocals = list(base_out_dir.rglob("vocals.wav"))
    if any_vocals:
        return any_vocals[0]
    raise FileNotFoundError("vocals.wav not produced by Demucs")


def run_extract_voice_job(job_id: int, mode: str = "studio") -> None:
    job = AudioFile.objects.get(id=job_id)
    job.status = "processing"
    job.error_message = ""
    job.save(update_fields=["status", "error_message", "updated_at"])

    input_path = Path(job.original_file.path)
    stem = input_path.stem
    work_dir = Path(settings.MEDIA_ROOT) / "tmp_demucs" / f"job_{job_id}"
    out_dir = work_dir / "out"
    os.makedirs(work_dir, exist_ok=True)
    os.makedirs(out_dir, exist_ok=True)

    demucs_cmd = [
        "demucs",
        "--two-stems=vocals",
        "--out",
        str(out_dir),
        str(input_path),
    ]
    # For pro mode, allow heavier model in future. For now same model path.
    if mode == "pro":
        demucs_cmd.extend(["-n", "htdemucs"])

    try:
        subprocess.run(demucs_cmd, check=True, capture_output=True, text=True)
        vocals_path = _resolve_vocals_file(out_dir, stem)
        cleaned_name = f"extract_{job.id}_{stem}_vocals.wav"
        media_cleaned_dir = Path(settings.MEDIA_ROOT) / "cleaned"
        os.makedirs(media_cleaned_dir, exist_ok=True)
        final_out = media_cleaned_dir / cleaned_name
        shutil.copy2(vocals_path, final_out)

        with open(final_out, "rb") as f:
            job.cleaned_file.save(cleaned_name, f, save=False)
        job.status = "completed"
        job.processing_mode = mode
        try:
            import soundfile as sf
            audio, sr = sf.read(str(final_out))
            job.duration = len(audio) / sr
        except Exception:
            pass
        try:
            job.cleaned_size = final_out.stat().st_size
        except Exception:
            pass
        job.save()
    except Exception as e:
        job.status = "failed"
        job.error_message = f"Extract voice failed: {e}"
        job.save(update_fields=["status", "error_message", "updated_at"])
    finally:
        shutil.rmtree(work_dir, ignore_errors=True)
"""
)
print("written", p)
