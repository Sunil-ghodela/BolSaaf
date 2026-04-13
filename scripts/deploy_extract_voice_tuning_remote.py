#!/usr/bin/env python3
"""
Run ON the Django server after scp of extract_feedback_tuning.py and demucs_extract.py:

  cd /var/www/simplelms/backend && .venv/bin/python /path/to/deploy_extract_voice_tuning_remote.py

Or pipe: ssh root@host 'python3 -' < deploy_extract_voice_tuning_remote.py
"""
from __future__ import annotations

import subprocess
import sys
from pathlib import Path

BASE = Path("/var/www/simplelms/backend/apps/voice")
MODELS = BASE / "models.py"
VIEWS = BASE / "views.py"
ADMIN = BASE / "admin.py"
REEL = BASE / "services" / "reel_mode.py"

REEL_OLD = """def _maybe_extract_voice(input_wav: Path, output_wav: Path) -> None:
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
    shutil.copy2(vocals, output_wav)"""

REEL_NEW = """def _maybe_extract_voice(input_wav: Path, output_wav: Path) -> None:
    from apps.voice.services.extract_feedback_tuning import (
        blend_vocals_dry,
        get_vocals_dry_ratio,
        refresh_tuning_if_stale,
    )

    refresh_tuning_if_stale(max_seconds=600)
    dry_ratio = get_vocals_dry_ratio()
    out_dir = output_wav.parent / "demucs_out"
    out_dir.mkdir(parents=True, exist_ok=True)
    cmd = [
        sys.executable,
        "-m",
        "demucs.separate",
        "--two-stems=vocals",
        "--out",
        str(out_dir),
        str(input_wav),
    ]
    subprocess.run(cmd, check=True, capture_output=True, text=True)
    vocals = _resolve_vocals_file(out_dir, input_wav.stem)
    if dry_ratio >= 0.002:
        blend_vocals_dry(vocals, input_wav, output_wav, dry_ratio)
    else:
        shutil.copy2(vocals, output_wav)"""

MODEL_BLOCK = """


class ExtractVoiceTuningState(models.Model):
    \"\"\"Singleton (pk=1): vocals/original blend after Demucs, driven by VoiceFeedback.\"\"\"

    id = models.PositiveSmallIntegerField(primary_key=True, default=1, editable=False)
    vocals_dry_ratio = models.FloatField(
        default=0.06,
        help_text="Blend weight for original mix into Demucs vocals (reduces metallic artifacts).",
    )
    last_summary = models.JSONField(default=dict, blank=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        verbose_name = "Extract voice tuning"
        verbose_name_plural = "Extract voice tuning"

    def save(self, *args, **kwargs):
        self.pk = 1
        super().save(*args, **kwargs)

    @classmethod
    def get_solo(cls):
        obj, _ = cls.objects.get_or_create(
            pk=1,
            defaults={"vocals_dry_ratio": 0.06, "last_summary": {}},
        )
        return obj

    def __str__(self):
        return f\"ExtractVoiceTuning dry={self.vocals_dry_ratio:.3f}\"
"""


def patch_models() -> None:
    text = MODELS.read_text(encoding="utf-8")
    if "class ExtractVoiceTuningState" in text:
        print("models.py: ExtractVoiceTuningState already present")
        return
    MODELS.write_text(text.rstrip() + MODEL_BLOCK + "\n", encoding="utf-8")
    print("models.py: appended ExtractVoiceTuningState")


def patch_admin() -> None:
    text = ADMIN.read_text(encoding="utf-8")
    if "ExtractVoiceTuningState" in text:
        print("admin.py: already patched")
        return
    text = text.replace(
        "from .models import AudioFile, VoiceFeedback",
        "from .models import AudioFile, ExtractVoiceTuningState, VoiceFeedback",
    )
    block = """


@admin.register(ExtractVoiceTuningState)
class ExtractVoiceTuningStateAdmin(admin.ModelAdmin):
    list_display = ("id", "vocals_dry_ratio", "updated_at")
    readonly_fields = ("id", "updated_at")

"""
    ADMIN.write_text(text.rstrip() + block + "\n", encoding="utf-8")
    print("admin.py: registered ExtractVoiceTuningState")


def patch_views() -> None:
    text = VIEWS.read_text(encoding="utf-8")
    if "invalidate_tuning_cache" in text and "recompute_from_feedback" in text:
        print("views.py: extract_feedback_tuning import already present")
    elif "from .services.video_process import run_video_reel_job" in text:
        text = text.replace(
            "from .services.video_process import run_video_reel_job",
            "from .services.video_process import run_video_reel_job\n\n"
            "from .services.extract_feedback_tuning import invalidate_tuning_cache, recompute_from_feedback\n",
        )
    else:
        print("views.py: could not find video_process import anchor", file=sys.stderr)
        sys.exit(1)

    old_post = """        obj = serializer.save()
        return Response({
            'status': 'ok',
            'id': obj.id,
            'created_at': obj.created_at.isoformat(),
        }, status=status.HTTP_201_CREATED)"""

    new_post = """        obj = serializer.save()
        if obj.mode_used in ('extract_voice', 'reel', 'reel_mode'):
            def _tune():
                try:
                    invalidate_tuning_cache()
                    recompute_from_feedback()
                except Exception:
                    logger.exception('feedback-driven demucs tuning')
            threading.Thread(target=_tune, daemon=True).start()
        return Response({
            'status': 'ok',
            'id': obj.id,
            'created_at': obj.created_at.isoformat(),
        }, status=status.HTTP_201_CREATED)"""

    if "feedback-driven demucs tuning" in text:
        print("views.py: VoiceFeedback post already patched")
    elif old_post in text:
        text = text.replace(old_post, new_post, 1)
    else:
        old_post2 = old_post.replace("'", '"')
        new_post2 = new_post.replace("'", '"')
        if old_post2 in text:
            text = text.replace(old_post2, new_post2, 1)
        else:
            print("views.py: VoiceFeedback post block not found", file=sys.stderr)
            sys.exit(1)
    VIEWS.write_text(text, encoding="utf-8")
    print("views.py: ok")


def run_migrate() -> None:
    manage = Path("/var/www/simplelms/backend/manage.py")
    py = Path("/var/www/simplelms/backend/.venv/bin/python")
    exe = str(py) if py.is_file() else sys.executable
    subprocess.run(
        [exe, str(manage), "makemigrations", "voice", "--name", "extract_voice_tuning_state"],
        cwd=str(manage.parent),
        check=True,
    )
    subprocess.run(
        [exe, str(manage), "migrate", "voice", "--noinput"],
        cwd=str(manage.parent),
        check=True,
    )
    print("migrate: ok")


def patch_reel_mode() -> None:
    text = REEL.read_text(encoding="utf-8")
    if "refresh_tuning_if_stale" in text and "_maybe_extract_voice" in text:
        print("reel_mode.py: already uses extract_feedback_tuning")
        return
    if "import sys" not in text:
        text = text.replace("import shutil\n", "import shutil\nimport sys\n", 1)
    if REEL_OLD not in text:
        print("reel_mode.py: _maybe_extract_voice anchor not found", file=sys.stderr)
        sys.exit(1)
    REEL.write_text(text.replace(REEL_OLD, REEL_NEW, 1), encoding="utf-8")
    print("reel_mode.py: patched _maybe_extract_voice")


def main() -> int:
    patch_models()
    patch_admin()
    patch_views()
    patch_reel_mode()
    run_migrate()
    print("Done. Restart Django if needed.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
