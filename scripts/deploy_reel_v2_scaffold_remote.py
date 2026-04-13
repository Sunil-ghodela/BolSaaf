#!/usr/bin/env python3
"""
Deploy/patch Reel V2 scaffold + hardening on production backend.
Run on server:
  /var/www/simplelms/backend/.venv/bin/python /tmp/deploy_reel_v2_scaffold_remote.py
"""
from pathlib import Path
import subprocess
import sys

BASE = Path("/var/www/simplelms/backend/apps/voice")
MODELS = BASE / "models.py"
SERVICES = BASE / "services"
ADMIN = BASE / "admin.py"
VIEWS = BASE / "views.py"
URLS = BASE / "urls.py"
MGMT_DIR = BASE / "management" / "commands"

SERVICE_CODE = '''import threading
from pathlib import Path

import soundfile as sf

from apps.voice.models import AudioFile, ReelJob, ReelOutput
from apps.voice.services.reel_mode import run_reel_job
from apps.voice.services.video_process import run_video_reel_job

STAGES = ["analyzing", "cleaning", "extracting", "mixing", "encoding"]


def _default_stage_progress():
    return {k: 0 for k in STAGES}


def _set_stage(job: ReelJob, stage: str, stage_value: int, overall: int):
    sp = dict(job.stage_progress or _default_stage_progress())
    if stage in sp:
        sp[stage] = max(0, min(100, int(stage_value)))
    job.current_stage = stage
    job.overall_progress = max(0, min(100, int(overall)))
    job.stage_progress = sp
    job.save(update_fields=["current_stage", "overall_progress", "stage_progress", "updated_at"])


def _analysis_summary(source: AudioFile) -> dict:
    try:
        p = Path(source.original_file.path)
        audio, sr = sf.read(str(p))
        if hasattr(audio, "ndim") and audio.ndim > 1:
            import numpy as np
            m = np.mean(audio, axis=1)
        else:
            m = audio
        import numpy as np
        rms = float(np.sqrt(np.mean(np.square(m)))) if len(m) else 0.0
        peak = float(np.max(np.abs(m))) if len(m) else 0.0
        crest = (peak / rms) if rms > 1e-9 else 0.0
        duration = float(len(m) / sr) if sr else 0.0
        silence_ratio = float(np.mean(np.abs(m) < 0.01)) if len(m) else 0.0
        rms_dbfs = 20.0 * np.log10(max(rms, 1e-9))
        noise_floor_db = 20.0 * np.log10(max(float(np.percentile(np.abs(m), 20)), 1e-9)) if len(m) else -90.0
        return {
            "duration_sec": round(duration, 2),
            "sample_rate": int(sr),
            "rms_dbfs": round(float(rms_dbfs), 2),
            "crest_factor": round(float(crest), 2),
            "noise_floor_db": round(float(noise_floor_db), 2),
            "silence_ratio": round(float(silence_ratio), 4),
        }
    except Exception as e:
        return {"error": f"analysis_failed: {e}"}


def _variant_cfg(variant: str, bg: str | None, preset_id: str | None):
    if variant == "clean_only":
        return {"mode": "standard", "extract_voice": False, "bg": None, "bg_volume": 0.0, "preset_id": None}
    if variant == "with_bg":
        return {"mode": "standard", "extract_voice": False, "bg": bg or "cafe", "bg_volume": 0.15, "preset_id": preset_id}
    return {"mode": "studio", "extract_voice": True, "bg": bg or "street", "bg_volume": 0.20, "preset_id": preset_id or "viral_reel"}


def _spawn_with_timeout(fn, args: tuple, timeout_sec: int):
    result = {"error": None}

    def _run():
        try:
            fn(*args)
        except Exception as e:
            result["error"] = str(e)

    t = threading.Thread(target=_run, daemon=True)
    t.start()
    t.join(timeout=timeout_sec)
    if t.is_alive():
        raise TimeoutError(f"timeout after {timeout_sec}s")
    if result["error"]:
        raise RuntimeError(result["error"])


def _create_child_from_source(source: AudioFile, mode: str) -> AudioFile:
    return AudioFile.objects.create(
        original_file=source.original_file.name,
        original_size=source.original_size,
        processing_mode=mode,
        status="pending",
    )


def run_reel_v2_job(reel_job_id: int):
    job = ReelJob.objects.get(id=reel_job_id)
    job.status = "processing"
    job.current_stage = "analyzing"
    job.stage_progress = _default_stage_progress()
    job.overall_progress = 1
    job.error_message = ""
    job.save(update_fields=["status", "current_stage", "stage_progress", "overall_progress", "error_message", "updated_at"])

    variant_errors = {}
    success_count = 0

    try:
        source = job.source_audio_file
        if not source:
            raise RuntimeError("source_audio_file missing")

        summary = _analysis_summary(source)
        job.analysis_summary = summary
        job.save(update_fields=["analysis_summary", "updated_at"])

        duration = float(summary.get("duration_sec") or 60.0)
        base_timeout = int(max(120, min(1200, duration * 1.8 + 60)))

        _set_stage(job, "analyzing", 100, 12)

        requested = list(job.requested_variants or ["clean_only", "with_bg", "viral_boosted"])
        allowed = {"clean_only", "with_bg", "viral_boosted"}
        requested = [v for v in requested if v in allowed] or ["clean_only", "with_bg", "viral_boosted"]
        total = max(1, len(requested))

        for idx, variant in enumerate(requested):
            cfg = _variant_cfg(variant, job.background_preset or None, job.preset_id or None)

            _set_stage(job, "cleaning", 20 + int(40 * idx / total), 18 + int(30 * idx / total))
            audio_child = _create_child_from_source(source, cfg["mode"])
            try:
                _spawn_with_timeout(
                    run_reel_job,
                    (
                        audio_child.id,
                        cfg["mode"],
                        float(job.target_lufs),
                        cfg["bg"],
                        float(cfg["bg_volume"]),
                        bool(cfg["extract_voice"]),
                        cfg["preset_id"],
                        True,
                    ),
                    timeout_sec=base_timeout,
                )
                audio_child.refresh_from_db()
                if audio_child.status != "completed" or not audio_child.cleaned_file:
                    raise RuntimeError(audio_child.error_message or "no cleaned file")
            except Exception as e:
                variant_errors[variant] = f"audio_failed: {e}"
                continue

            _set_stage(job, "mixing", 45 + int(30 * (idx + 1) / total), 52 + int(20 * (idx + 1) / total))
            out = ReelOutput.objects.create(
                reel_job=job,
                variant=variant,
                audio_file=audio_child,
                duration_sec=audio_child.duration,
                loudness_lufs=float(job.target_lufs),
                size_bytes=audio_child.cleaned_size,
            )
            success_count += 1

            if job.include_video:
                _set_stage(job, "encoding", 20 + int(75 * (idx + 1) / total), 72 + int(25 * (idx + 1) / total))
                video_child = _create_child_from_source(source, cfg["mode"])
                try:
                    _spawn_with_timeout(
                        run_video_reel_job,
                        (
                            video_child.id,
                            cfg["mode"],
                            float(job.target_lufs),
                            cfg["bg"],
                            float(cfg["bg_volume"]),
                            bool(cfg["extract_voice"]),
                            cfg["preset_id"],
                            True,
                        ),
                        timeout_sec=max(150, int(base_timeout * 1.4)),
                    )
                    video_child.refresh_from_db()
                    if video_child.status == "completed" and video_child.cleaned_file:
                        out.video_file = video_child
                        out.save(update_fields=["video_file", "updated_at"])
                    else:
                        variant_errors[f"{variant}:video"] = video_child.error_message or "video_failed"
                except Exception as e:
                    variant_errors[f"{variant}:video"] = str(e)

        sp = dict(job.stage_progress or _default_stage_progress())
        sp.update({"cleaning": 100, "extracting": 100, "mixing": 100, "encoding": 100})
        job.stage_progress = sp
        job.current_stage = "completed"
        job.overall_progress = 100

        analysis = dict(job.analysis_summary or {})
        if variant_errors:
            analysis["variant_errors"] = variant_errors
            job.error_message = "; ".join([f"{k}={v}" for k, v in variant_errors.items()])[:2000]

        job.analysis_summary = analysis
        if success_count > 0:
            job.status = "completed"
        else:
            job.status = "failed"
            if not job.error_message:
                job.error_message = "all_variants_failed"

        job.save(update_fields=["status", "current_stage", "overall_progress", "stage_progress", "analysis_summary", "error_message", "updated_at"])
    except Exception as e:
        job.status = "failed"
        job.error_message = str(e)
        job.save(update_fields=["status", "error_message", "updated_at"])
'''

MODELS_APPEND = '''

class ReelJob(models.Model):
    STATUS_CHOICES = [
        ("pending", "Pending"),
        ("processing", "Processing"),
        ("completed", "Completed"),
        ("failed", "Failed"),
    ]

    source_audio_file = models.ForeignKey(AudioFile, on_delete=models.SET_NULL, null=True, blank=True, related_name="reel_jobs")
    source_type = models.CharField(max_length=16, default="audio")
    requested_variants = models.JSONField(default=list, blank=True)
    preset_id = models.CharField(max_length=64, blank=True, default="")
    background_preset = models.CharField(max_length=64, blank=True, default="")
    target_lufs = models.FloatField(default=-16.0)
    include_video = models.BooleanField(default=True)

    status = models.CharField(max_length=20, choices=STATUS_CHOICES, default="pending")
    current_stage = models.CharField(max_length=32, blank=True, default="pending")
    overall_progress = models.PositiveSmallIntegerField(default=0)
    stage_progress = models.JSONField(default=dict, blank=True)
    analysis_summary = models.JSONField(default=dict, blank=True)
    error_message = models.TextField(blank=True, default="")

    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)
    expires_at = models.DateTimeField(null=True, blank=True)

    class Meta:
        ordering = ["-created_at"]


class ReelOutput(models.Model):
    reel_job = models.ForeignKey(ReelJob, on_delete=models.CASCADE, related_name="outputs")
    variant = models.CharField(max_length=32)
    audio_file = models.ForeignKey(AudioFile, on_delete=models.SET_NULL, null=True, blank=True, related_name="reel_audio_outputs")
    video_file = models.ForeignKey(AudioFile, on_delete=models.SET_NULL, null=True, blank=True, related_name="reel_video_outputs")
    duration_sec = models.FloatField(null=True, blank=True)
    loudness_lufs = models.FloatField(null=True, blank=True)
    size_bytes = models.PositiveIntegerField(null=True, blank=True)
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        ordering = ["variant", "-created_at"]
'''

VIEWS_APPEND = '''

class ReelCreateV2View(APIView):
    permission_classes = [AllowAny]
    parser_classes = [JSONParser, MultiPartParser, FormParser]

    def post(self, request):
        from datetime import timedelta
        from django.utils import timezone
        from .models import ReelJob

        uploaded_file = request.FILES.get("file")
        input_file_id = request.data.get("input_file_id") or request.data.get("inputFileId")

        source = None
        if uploaded_file:
            source = AudioFile.objects.create(
                original_file=uploaded_file,
                original_size=getattr(uploaded_file, "size", None),
                processing_mode="standard",
                status="completed",
            )
        elif input_file_id:
            try:
                source = AudioFile.objects.get(id=int(input_file_id))
            except Exception:
                return Response({"status": "error", "message": "input_file_id not found"}, status=status.HTTP_400_BAD_REQUEST)
        else:
            return Response({"status": "error", "message": "file or input_file_id required"}, status=status.HTTP_400_BAD_REQUEST)

        requested = request.data.get("requested_variants") or request.data.get("requestedVariants")
        if isinstance(requested, str):
            requested = [x.strip() for x in requested.split(",") if x.strip()]
        if not requested:
            requested = ["clean_only", "with_bg", "viral_boosted"]
        allowed = {"clean_only", "with_bg", "viral_boosted"}
        cleaned = []
        seen = set()
        for v in requested:
            if v in allowed and v not in seen:
                cleaned.append(v)
                seen.add(v)
        requested = cleaned
        if not requested:
            return Response({"status": "error", "message": "requested_variants invalid", "allowed": sorted(list(allowed))}, status=status.HTTP_400_BAD_REQUEST)

        try:
            target_lufs = float(request.data.get("target_lufs", request.data.get("loudnessTarget", -16)))
        except Exception:
            target_lufs = -16.0

        include_video_raw = str(request.data.get("include_video", "true")).strip().lower()
        include_video = include_video_raw in {"1", "true", "yes", "on"}

        job = ReelJob.objects.create(
            source_audio_file=source,
            source_type=str(request.data.get("source_type", "audio")),
            requested_variants=requested,
            preset_id=str(request.data.get("preset_id", "") or ""),
            background_preset=str(request.data.get("background_preset", "") or ""),
            target_lufs=target_lufs,
            include_video=include_video,
            status="pending",
            current_stage="analyzing",
            overall_progress=0,
            stage_progress={"analyzing": 0, "cleaning": 0, "extracting": 0, "mixing": 0, "encoding": 0},
            expires_at=timezone.now() + timedelta(days=30),
        )

        threading.Thread(target=run_reel_v2_job, args=(job.id,), daemon=True).start()

        return Response(
            {
                "status": "accepted",
                "reel_job_id": job.id,
                "estimated_time_sec": 45,
                "current_stage": "analyzing",
                "overall_progress": 0,
                "stages": [
                    {"stage": "analyzing", "progress": 0},
                    {"stage": "cleaning", "progress": 0},
                    {"stage": "extracting", "progress": 0},
                    {"stage": "mixing", "progress": 0},
                    {"stage": "encoding", "progress": 0},
                ],
            },
            status=status.HTTP_202_ACCEPTED,
        )


class ReelStatusV2View(APIView):
    permission_classes = [AllowAny]

    def get(self, request, reel_job_id: int):
        from .models import ReelJob

        try:
            job = ReelJob.objects.get(id=reel_job_id)
        except ReelJob.DoesNotExist:
            return Response({"status": "error", "message": "reel job not found"}, status=status.HTTP_404_NOT_FOUND)

        outputs = {"clean_only": None, "with_bg": None, "viral_boosted": None}
        for o in job.outputs.all():
            audio_url = f"/voice/cleaned_output/{o.audio_file_id}/" if o.audio_file_id else None
            video_url = f"/voice/cleaned_output/{o.video_file_id}/" if o.video_file_id else None
            outputs[o.variant] = {
                "audio_url": audio_url,
                "video_url": video_url,
                "duration_sec": o.duration_sec,
                "loudness_lufs": o.loudness_lufs,
            }

        return Response(
            {
                "reel_job_id": job.id,
                "status": job.status,
                "current_stage": job.current_stage,
                "overall_progress": job.overall_progress,
                "stage_progress": job.stage_progress,
                "analysis_summary": job.analysis_summary,
                "outputs": outputs,
                "error_code": _reel_error_code(job.error_message) if job.status == "failed" else None,
                "error_message": job.error_message if job.status == "failed" else None,
                "created_at": job.created_at,
                "updated_at": job.updated_at,
                "expires_at": job.expires_at,
            },
            status=status.HTTP_200_OK,
        )
'''

CLEANUP_CMD = '''from django.core.management.base import BaseCommand
from django.utils import timezone

from apps.voice.models import ReelJob


class Command(BaseCommand):
    help = "Delete expired ReelJob rows (and cascaded ReelOutput rows)."

    def handle(self, *args, **options):
        now = timezone.now()
        qs = ReelJob.objects.filter(expires_at__isnull=False, expires_at__lt=now)
        count = qs.count()
        qs.delete()
        self.stdout.write(self.style.SUCCESS(f"cleanup_reel_expired: deleted={count}"))
'''


def patch_models() -> None:
    text = MODELS.read_text()
    if "class ReelJob(models.Model):" in text:
        print("models.py already patched")
        return
    MODELS.write_text(text.rstrip() + MODELS_APPEND + "\n")
    print("models.py patched")


def patch_service() -> None:
    SERVICES.mkdir(parents=True, exist_ok=True)
    (SERVICES / "reel_v2.py").write_text(SERVICE_CODE)
    print("services/reel_v2.py written")


def patch_admin() -> None:
    text = ADMIN.read_text()
    if "ReelJobAdmin" in text:
        print("admin.py already patched")
        return
    text = text.replace(
        "from .models import AudioFile, ExtractVoiceTuningState, VoiceFeedback",
        "from .models import AudioFile, ExtractVoiceTuningState, VoiceFeedback, ReelJob, ReelOutput",
    )
    text += """

@admin.register(ReelJob)
class ReelJobAdmin(admin.ModelAdmin):
    list_display = ("id", "status", "current_stage", "overall_progress", "created_at")
    list_filter = ("status", "current_stage", "created_at")
    search_fields = ("id",)


@admin.register(ReelOutput)
class ReelOutputAdmin(admin.ModelAdmin):
    list_display = ("id", "reel_job", "variant", "audio_file", "video_file", "created_at")
    list_filter = ("variant", "created_at")
"""
    ADMIN.write_text(text)
    print("admin.py patched")


def patch_views() -> None:
    text = VIEWS.read_text()

    if "def _reel_error_code(" not in text:
        helper = """

def _reel_error_code(msg: str | None) -> str | None:
    m = (msg or '').lower()
    if not m:
        return None
    if 'timeout' in m:
        return 'processing_timeout'
    if 'audio_failed' in m or 'no cleaned file' in m:
        return 'audio_variant_failed'
    if 'video' in m:
        return 'video_variant_failed'
    if 'all_variants_failed' in m:
        return 'all_variants_failed'
    return 'reel_processing_failed'
"""
        text = text.replace("logger = logging.getLogger(__name__)\n", "logger = logging.getLogger(__name__)\n" + helper)

    if "from .services.reel_v2 import run_reel_v2_job" not in text:
        text = text.replace(
            "from .services.extract_feedback_tuning import invalidate_tuning_cache, recompute_from_feedback\n",
            "from .services.extract_feedback_tuning import invalidate_tuning_cache, recompute_from_feedback\nfrom .services.reel_v2 import run_reel_v2_job\n",
        )

    if "class ReelCreateV2View(APIView):" not in text:
        text = text.rstrip() + "\n" + VIEWS_APPEND + "\n"

    VIEWS.write_text(text)
    print("views.py patched")


def patch_urls() -> None:
    text = URLS.read_text()
    if "ReelCreateV2View" not in text:
        text = text.replace("ExtractFromUrlView)", "ExtractFromUrlView, ReelCreateV2View, ReelStatusV2View)")
    if "path('reel/create/'" not in text:
        text = text.replace(
            "path('reel/', ReelModeView.as_view(), name='reel_mode'),",
            "path('reel/', ReelModeView.as_view(), name='reel_mode'),\n    path('reel/create/', ReelCreateV2View.as_view(), name='reel_create_v2'),\n    path('reel/<int:reel_job_id>/status/', ReelStatusV2View.as_view(), name='reel_status_v2'),",
        )
    URLS.write_text(text)
    print("urls.py patched")


def patch_cleanup_command() -> None:
    MGMT_DIR.mkdir(parents=True, exist_ok=True)
    cmd = MGMT_DIR / "cleanup_reel_expired.py"
    cmd.write_text(CLEANUP_CMD)
    print("cleanup_reel_expired.py written")


def migrate() -> None:
    python = Path("/var/www/simplelms/backend/.venv/bin/python")
    exe = str(python) if python.exists() else sys.executable
    subprocess.run([exe, "/var/www/simplelms/backend/manage.py", "makemigrations", "voice", "--name", "reel_v2_scaffold"], cwd="/var/www/simplelms/backend", check=True)
    subprocess.run([exe, "/var/www/simplelms/backend/manage.py", "migrate", "voice", "--noinput"], cwd="/var/www/simplelms/backend", check=True)
    print("migrations done")


def main() -> int:
    patch_models()
    patch_service()
    patch_admin()
    patch_views()
    patch_urls()
    patch_cleanup_command()
    migrate()
    print("reel v2 scaffold/hardening deploy complete")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
