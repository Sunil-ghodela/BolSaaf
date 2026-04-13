# Skeleton for Django — merge into your `voice` app (paths/names match your project).
# Requires: yt-dlp on PATH, existing ExtractVoice / Job / Celery patterns.
#
# urls.py (add next to extract_voice/):
#   path("extract_from_url/", ExtractFromUrlView.as_view(), name="extract_from_url"),
#
# views.py (sketch only — adapt imports, permissions, and enqueue helper):

import json
import shutil
import subprocess
import tempfile
from pathlib import Path

from django.http import JsonResponse
from django.views import View
from django.views.decorators.csrf import csrf_exempt
from django.utils.decorators import method_decorator


ALLOWED_HOST_FRAGMENTS = (
    "youtube.com",
    "youtu.be",
    "instagram.com",
    "tiktok.com",
    "vm.tiktok.com",
)


def _allowed_url(url: str) -> bool:
    u = url.strip().lower()
    if not u.startswith("https://"):
        return False
    return any(h in u for h in ALLOWED_HOST_FRAGMENTS)


@method_decorator(csrf_exempt, name="dispatch")
class ExtractFromUrlView(View):
    def post(self, request):
        try:
            body = json.loads(request.body.decode("utf-8"))
        except json.JSONDecodeError:
            return JsonResponse({"error": "invalid_json"}, status=400)
        source_url = (body.get("source_url") or "").strip()
        mode = (body.get("mode") or "studio").strip()
        if not source_url or not _allowed_url(source_url):
            return JsonResponse({"error": "unsupported_or_invalid_url"}, status=400)

        # TODO: enqueue Celery task instead of blocking.
        # job = create_voice_job(job_type="extract_from_url", mode=mode, meta={"source_url": source_url})
        # enqueue_extract_from_url.delay(job.id)
        # return JsonResponse({"job_id": job.id, "job_type": "extract_from_url", "message": "Job accepted"})

        workdir = Path(tempfile.mkdtemp(prefix="url_extract_"))
        audio_wav = workdir / "source.wav"
        try:
            subprocess.run(
                [
                    "yt-dlp",
                    "-x",
                    "--audio-format",
                    "wav",
                    "-o",
                    str(audio_wav.with_suffix("")) + ".%(ext)s",
                    "--no-playlist",
                    source_url,
                ],
                check=True,
                capture_output=True,
                text=True,
                timeout=600,
            )
            produced = next(workdir.glob("source.*"))
            if produced.suffix.lower() != ".wav":
                return JsonResponse({"error": "download_failed"}, status=502)
        except Exception as e:
            shutil.rmtree(workdir, ignore_errors=True)
            return JsonResponse({"error": str(e)}, status=502)

        # TODO: hand `produced` to your existing extract_voice pipeline; return job_id from that enqueue.
        shutil.rmtree(workdir, ignore_errors=True)
        return JsonResponse(
            {"error": "wire_to_existing_extract_job_here"},
            status=501,
        )
