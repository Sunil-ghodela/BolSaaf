#!/usr/bin/env python3
from pathlib import Path


BASE = Path("/var/www/simplelms/backend/apps/voice")
VIEWS = BASE / "views.py"
URLS = BASE / "urls.py"
TASKS = BASE / "tasks.py"


def patch_tasks() -> None:
    text = TASKS.read_text()
    if 'name="voice.fastlib_clean"' in text:
        return

    anchor = "from apps.voice.services.url_extract import run_extract_from_url_job\n"
    extra_imports = (
        "from apps.voice.models import AudioFile\n"
        "from apps.voice.services.studio_pipeline import StudioPipeline, ProcessingMode\n"
        "import os\n"
        "import soundfile as sf\n"
        "from django.conf import settings\n"
        "import logging\n\n"
        "logger = logging.getLogger(__name__)\n"
    )
    if anchor in text and "from apps.voice.models import AudioFile" not in text:
        text = text.replace(anchor, anchor + extra_imports)

    text += """


@shared_task(name="voice.fastlib_clean")
def fastlib_clean_task(job_id: int) -> None:
    try:
        audio_file = AudioFile.objects.get(id=job_id)
    except AudioFile.DoesNotExist:
        logger.error("fastlib_clean: job %s not found", job_id)
        return

    try:
        audio_file.status = "processing"
        audio_file.save(update_fields=["status", "updated_at"])

        input_path = audio_file.original_file.path
        output_filename = f"fastlib_clean_{audio_file.id}.wav"
        output_path = os.path.join(settings.MEDIA_ROOT, "cleaned", output_filename)
        os.makedirs(os.path.dirname(output_path), exist_ok=True)

        pipeline = StudioPipeline()
        pipeline.process(input_path, output_path, mode=ProcessingMode.STUDIO, return_info=False)

        audio, sr = sf.read(output_path)
        duration = len(audio) / sr if sr else 0

        with open(output_path, "rb") as f:
            audio_file.cleaned_file.save(output_filename, f, save=False)

        audio_file.status = "completed"
        audio_file.duration = duration
        audio_file.cleaned_size = os.path.getsize(output_path)
        audio_file.processing_mode = "studio"
        audio_file.error_message = ""
        audio_file.save()
    except Exception as e:
        logger.exception("fastlib_clean failed for job_id=%s", job_id)
        audio_file.status = "failed"
        audio_file.error_message = str(e)
        audio_file.save(update_fields=["status", "error_message", "updated_at"])


@shared_task(name="voice.fastlib_video")
def fastlib_video_task(job_id: int) -> None:
    # Reuse existing video service but lock to studio config for lab testing.
    run_video_reel_job(job_id, "studio", -16.0, None, 0.15, False, None, True)
"""
    TASKS.write_text(text)


def patch_views() -> None:
    text = VIEWS.read_text()
    if "class FastLibCleanView(APIView):" not in text:
        marker = "\n\nclass BackgroundCatalogView(APIView):\n"
        block = """


class FastLibCleanView(APIView):
    \"\"\"Lab-only: full FastLib-style studio clean path (Celery).\"\"\"

    permission_classes = [AllowAny]
    parser_classes = [MultiPartParser, FormParser]

    def post(self, request):
        uploaded_file = request.FILES.get("file")
        if not uploaded_file:
            return Response({"status": "error", "message": "file is required"}, status=status.HTTP_400_BAD_REQUEST)

        job = AudioFile.objects.create(
            original_file=uploaded_file,
            original_size=getattr(uploaded_file, "size", None),
            processing_mode="studio",
            status="pending",
        )

        voice_tasks.fastlib_clean_task.delay(job.id)

        return Response(
            {
                "status": "accepted",
                "job_id": job.id,
                "job_type": "fastlib_clean",
                "message": "FastLib clean job queued",
                "engine": "fast_music_remover_method",
            },
            status=status.HTTP_202_ACCEPTED,
        )


class FastLibVideoProcessView(APIView):
    \"\"\"Lab-only: video path for FastLib test screen.\"\"\"

    permission_classes = [AllowAny]
    parser_classes = [MultiPartParser, FormParser]

    def post(self, request):
        uploaded_file = request.FILES.get("file")
        if not uploaded_file:
            return Response({"status": "error", "message": "file is required"}, status=status.HTTP_400_BAD_REQUEST)

        job = AudioFile.objects.create(
            original_file=uploaded_file,
            original_size=getattr(uploaded_file, "size", None),
            processing_mode="studio",
            status="pending",
        )

        voice_tasks.fastlib_video_task.delay(job.id)

        return Response(
            {
                "status": "accepted",
                "job_id": job.id,
                "job_type": "fastlib_video",
                "message": "FastLib video job queued",
                "engine": "fast_music_remover_method",
            },
            status=status.HTTP_202_ACCEPTED,
        )
"""
        if marker in text:
            text = text.replace(marker, block + marker)

    if "elif 'fastlib_' in name:" not in text:
        text = text.replace(
            "                elif 'mix_' in name:\n                    job_type = 'add_background'\n",
            "                elif 'mix_' in name:\n                    job_type = 'add_background'\n"
            "                elif 'fastlib_' in name:\n                    job_type = 'fastlib_clean'\n",
        )

    old_jobs = "'available_job_types': ['clean', 'extract_voice', 'add_background', 'reel', 'video_reel'],"
    new_jobs = "'available_job_types': ['clean', 'extract_voice', 'add_background', 'reel', 'video_reel', 'fastlib_clean', 'fastlib_video'],"
    if old_jobs in text:
        text = text.replace(old_jobs, new_jobs)

    VIEWS.write_text(text)


def patch_urls() -> None:
    text = URLS.read_text()
    if "FastLibCleanView" not in text:
        text = text.replace(
            "    ExtractFromUrlView, ReelCreateV2View, ReelStatusV2View\n)",
            "    ExtractFromUrlView, ReelCreateV2View, ReelStatusV2View, FastLibCleanView, FastLibVideoProcessView\n)",
        )
    if "path('lab/clean/'" not in text:
        text = text.replace(
            "    path('video/process/', VideoProcessView.as_view(), name='video_process'),\n",
            "    path('video/process/', VideoProcessView.as_view(), name='video_process'),\n"
            "    path('lab/clean/', FastLibCleanView.as_view(), name='fastlib_clean'),\n"
            "    path('lab/video/process/', FastLibVideoProcessView.as_view(), name='fastlib_video_process'),\n",
        )
    URLS.write_text(text)


if __name__ == "__main__":
    patch_tasks()
    patch_views()
    patch_urls()
    print("fastlib lab backend patches applied")
