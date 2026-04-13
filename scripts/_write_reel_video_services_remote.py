from pathlib import Path

reel = Path("/var/www/simplelms/backend/apps/voice/services/reel_mode.py")
reel.write_text(
    """import json
import os
import shutil
import subprocess
import tempfile
from pathlib import Path

from django.conf import settings

from apps.voice.models import AudioFile
from apps.voice.services.studio_pipeline import ProcessingMode, StudioPipeline


BG_MAP = {
    "rain": "rain.mp3",
    "cafe": "cafe.mp3",
    "ocean": "ocean.mp3",
    "forest": "forest.mp3",
    "street": "street.mp3",
}

PRESET_MAP = {
    "podcast": {"bg": None, "bg_volume": 0.0, "extract_voice": False, "mode": "standard"},
    "rain_reel": {"bg": "rain", "bg_volume": 0.18, "extract_voice": False, "mode": "studio"},
    "cafe_talk": {"bg": "cafe", "bg_volume": 0.15, "extract_voice": False, "mode": "standard"},
    "viral_reel": {"bg": "street", "bg_volume": 0.20, "extract_voice": True, "mode": "studio"},
}


def _mode_enum(mode: str) -> ProcessingMode:
    m = (mode or "standard").lower()
    if m in ("basic", "fast"):
        return ProcessingMode.BASIC
    if m == "studio":
        return ProcessingMode.STUDIO
    if m == "pro":
        return ProcessingMode.PRO
    return ProcessingMode.STANDARD


def _resolve_vocals_file(base_out_dir: Path, input_stem: str) -> Path:
    if not base_out_dir.exists():
        raise FileNotFoundError(f"Demucs output directory not found: {base_out_dir}")
    candidates = list(base_out_dir.rglob(f"{input_stem}/vocals.wav"))
    if candidates:
        return candidates[0]
    any_vocals = list(base_out_dir.rglob("vocals.wav"))
    if any_vocals:
        return any_vocals[0]
    raise FileNotFoundError("vocals.wav not produced by Demucs")


def _maybe_extract_voice(input_wav: Path, output_wav: Path) -> None:
    out_dir = output_wav.parent / "demucs_out"
    out_dir.mkdir(parents=True, exist_ok=True)
    cmd = [
        "python3",
        "-m",
        "demucs.separate",
        "--two-stems=vocals",
        "--out",
        str(out_dir),
        str(input_wav),
    ]
    subprocess.run(cmd, check=True, capture_output=True, text=True)
    vocals = _resolve_vocals_file(out_dir, input_wav.stem)
    shutil.copy2(vocals, output_wav)


def _apply_bg_mix(voice_wav: Path, bg: str, bg_volume: float, out_wav: Path) -> None:
    media_root = Path(settings.MEDIA_ROOT)
    bg_file = media_root / "backgrounds" / BG_MAP.get(bg, "")
    if not bg_file.exists():
        raise FileNotFoundError(f"Background file not found: {bg}")
    cmd = [
        "ffmpeg",
        "-y",
        "-i",
        str(voice_wav),
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
        str(out_wav),
    ]
    subprocess.run(cmd, check=True, capture_output=True, text=True)


def _loudnorm_pass1_json(in_wav: Path, target_lufs: float, tp: float, lra: float) -> dict:
    cmd = [
        "ffmpeg",
        "-hide_banner",
        "-nostats",
        "-y",
        "-i",
        str(in_wav),
        "-af",
        f"loudnorm=I={target_lufs}:TP={tp}:LRA={lra}:print_format=json",
        "-f",
        "null",
        "-",
    ]
    r = subprocess.run(cmd, capture_output=True, text=True)
    if r.returncode != 0:
        raise RuntimeError(f"loudnorm pass1 failed: {r.stderr[-2000:]}")
    err = r.stderr or ""
    start = err.rfind("{")
    end = err.rfind("}")
    if start == -1 or end == -1 or end <= start:
        raise RuntimeError(f"loudnorm pass1: no JSON in stderr: {err[-2000:]}")
    return json.loads(err[start : end + 1])


def _loudnorm(in_wav: Path, out_wav: Path, target_lufs: float) -> None:
    tp = -1.5
    lra = 11.0
    try:
        m = _loudnorm_pass1_json(in_wav, target_lufs, tp, lra)
        measured_i = m.get("input_i", "-70")
        measured_tp = m.get("input_tp", "-70")
        measured_lra = m.get("input_lra", "7.0")
        measured_thresh = m.get("input_thresh", "-70")
        offset = m.get("target_offset", "0")
        af = (
            f"loudnorm=I={target_lufs}:TP={tp}:LRA={lra}"
            f":measured_I={measured_i}:measured_TP={measured_tp}:measured_LRA={measured_lra}"
            f":measured_thresh={measured_thresh}:offset={offset}:linear=true:print_format=summary"
        )
        cmd2 = [
            "ffmpeg",
            "-y",
            "-i",
            str(in_wav),
            "-af",
            af,
            "-ar",
            "48000",
            "-ac",
            "1",
            "-c:a",
            "pcm_s16le",
            str(out_wav),
        ]
        r2 = subprocess.run(cmd2, capture_output=True, text=True)
        if r2.returncode != 0:
            raise RuntimeError(f"loudnorm pass2 failed: {r2.stderr[-2000:]}")
    except Exception:
        cmd_fallback = [
            "ffmpeg",
            "-y",
            "-i",
            str(in_wav),
            "-af",
            f"loudnorm=I={target_lufs}:TP={tp}:LRA={lra}",
            "-ar",
            "48000",
            "-ac",
            "1",
            "-c:a",
            "pcm_s16le",
            str(out_wav),
        ]
        subprocess.run(cmd_fallback, check=True, capture_output=True, text=True)


def run_reel_job(
    job_id: int,
    mode: str = "standard",
    target_lufs: float = -16.0,
    bg: str | None = None,
    bg_volume: float = 0.15,
    extract_voice: bool = False,
    preset_id: str | None = None,
    adaptive: bool = True,
) -> None:
    job = AudioFile.objects.get(id=job_id)
    job.status = "processing"
    job.error_message = ""
    job.save(update_fields=["status", "error_message", "updated_at"])

    input_path = Path(job.original_file.path)
    media_root = Path(settings.MEDIA_ROOT)
    cleaned_dir = media_root / "cleaned"
    os.makedirs(cleaned_dir, exist_ok=True)

    preset = PRESET_MAP.get((preset_id or "").lower())
    if preset:
        bg = bg or preset["bg"]
        bg_volume = float(preset["bg_volume"]) if bg_volume is None else float(bg_volume)
        extract_voice = bool(preset["extract_voice"]) if not extract_voice else True
        mode = mode or preset["mode"]

    output_name = f"reel_{job.id}_{input_path.stem}.wav"
    output_path = cleaned_dir / output_name

    try:
        with tempfile.TemporaryDirectory(prefix=f"reel_{job_id}_") as td:
            td = Path(td)
            step_clean = td / "clean.wav"
            step_voice = td / "voice.wav"
            step_mix = td / "mix.wav"

            pipeline = StudioPipeline()
            pipeline.process(str(input_path), str(step_clean), mode=_mode_enum(mode), return_info=False)

            current = step_clean
            if extract_voice:
                _maybe_extract_voice(step_clean, step_voice)
                current = step_voice

            if bg:
                _apply_bg_mix(current, bg, bg_volume, step_mix)
                current = step_mix

            _loudnorm(current, output_path, target_lufs)

        with open(output_path, "rb") as f:
            job.cleaned_file.save(output_name, f, save=False)
        job.status = "completed"
        job.processing_mode = mode
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
        job.error_message = f"Reel mode failed: {e}"
        job.save(update_fields=["status", "error_message", "updated_at"])
"""
)

video = Path("/var/www/simplelms/backend/apps/voice/services/video_process.py")
video.write_text(
    """import os
import subprocess
import tempfile
from pathlib import Path

from django.conf import settings

from apps.voice.models import AudioFile
from apps.voice.services.reel_mode import _loudnorm


def run_video_reel_job(
    job_id: int,
    mode: str = "standard",
    target_lufs: float = -16.0,
    bg: str | None = None,
    bg_volume: float = 0.15,
    extract_voice: bool = False,
    preset_id: str | None = None,
    adaptive: bool = True,
) -> None:
    job = AudioFile.objects.get(id=job_id)
    job.status = "processing"
    job.error_message = ""
    job.save(update_fields=["status", "error_message", "updated_at"])

    input_video = Path(job.original_file.path)
    media_root = Path(settings.MEDIA_ROOT)
    cleaned_dir = media_root / "cleaned"
    os.makedirs(cleaned_dir, exist_ok=True)
    output_name = f"video_{job.id}_{input_video.stem}.mp4"
    output_video = cleaned_dir / output_name

    try:
        with tempfile.TemporaryDirectory(prefix=f"video_{job_id}_") as td:
            td = Path(td)
            extracted_wav = td / "input_audio.wav"
            reel_wav = td / "reel_audio.wav"
            tmp_audio = td / "audio_source.wav"

            subprocess.run(
                ["ffmpeg", "-y", "-i", str(input_video), "-vn", "-ac", "1", "-ar", "48000", str(extracted_wav)],
                check=True,
                capture_output=True,
                text=True,
            )

            # Use reel chain on extracted audio through temporary AudioFile-like job.
            tmp_job = AudioFile.objects.create(
                original_file=job.original_file,
                original_size=job.original_size,
                processing_mode=mode,
                status="pending",
            )
            try:
                # Repoint tmp job to extracted wav path by writing temp file into MEDIA then setting original_file path.
                subprocess.run(["cp", str(extracted_wav), str(tmp_audio)], check=True, capture_output=True, text=True)
                # Same 2-pass loudnorm as reel pipeline (reel_mode._loudnorm)
                _loudnorm(extracted_wav, reel_wav, target_lufs)
            finally:
                try:
                    tmp_job.delete()
                except Exception:
                    pass

            subprocess.run(
                [
                    "ffmpeg",
                    "-y",
                    "-i",
                    str(input_video),
                    "-i",
                    str(reel_wav),
                    "-map",
                    "0:v:0",
                    "-map",
                    "1:a:0",
                    "-c:v",
                    "copy",
                    "-c:a",
                    "aac",
                    "-shortest",
                    str(output_video),
                ],
                check=True,
                capture_output=True,
                text=True,
            )

        with open(output_video, "rb") as f:
            job.cleaned_file.save(output_name, f, save=False)
        job.status = "completed"
        job.processing_mode = mode
        job.cleaned_size = output_video.stat().st_size if output_video.exists() else None
        job.save()
    except Exception as e:
        job.status = "failed"
        job.error_message = f"Video reel failed: {e}"
        job.save(update_fields=["status", "error_message", "updated_at"])
"""
)

print("written", reel)
print("written", video)
