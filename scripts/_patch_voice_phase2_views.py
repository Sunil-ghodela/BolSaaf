from pathlib import Path

p = Path('/var/www/simplelms/backend/apps/voice/views.py')
text = p.read_text()

if 'class ExtractVoiceView(APIView):' not in text:
    text += '''


class _Phase2JobMixin:
    permission_classes = [AllowAny]
    parser_classes = [MultiPartParser, FormParser]

    def _accept_job(self, request, job_type: str, default_mode: str = "standard"):
        uploaded_file = request.FILES.get("file")
        if not uploaded_file:
            return Response({"status": "error", "message": "file is required"}, status=status.HTTP_400_BAD_REQUEST)

        mode = (request.data.get("mode") or default_mode).strip().lower()
        mode = mode if mode in ("basic", "standard", "studio", "pro", "fast") else default_mode

        preset_id = (request.data.get("preset_id") or "").strip().lower()
        adaptive_raw = str(request.data.get("adaptive", "true")).strip().lower()
        adaptive = adaptive_raw in {"1", "true", "yes", "on"}

        job = AudioFile.objects.create(
            original_file=uploaded_file,
            original_size=getattr(uploaded_file, "size", None),
            processing_mode=mode,
            status="pending",
        )

        return Response(
            {
                "status": "accepted",
                "job_id": job.id,
                "job_type": job_type,
                "message": "Job queued",
                "requested": {
                    "mode": mode,
                    "preset_id": preset_id or None,
                    "adaptive": adaptive,
                },
            },
            status=status.HTTP_202_ACCEPTED,
        )


class ExtractVoiceView(_Phase2JobMixin, APIView):
    """Phase-2 scaffold: accepts extract voice jobs."""

    def post(self, request):
        return self._accept_job(request, job_type="extract_voice", default_mode="studio")


class AddBackgroundView(_Phase2JobMixin, APIView):
    """Phase-2 scaffold: accepts add background jobs."""

    def post(self, request):
        bg = (request.data.get("bg") or "").strip().lower()
        if bg not in {"rain", "cafe", "ocean", "forest", "street"}:
            return Response(
                {"status": "error", "message": "bg must be one of: rain, cafe, ocean, forest, street"},
                status=status.HTTP_400_BAD_REQUEST,
            )
        return self._accept_job(request, job_type="add_background", default_mode="standard")


class ReelModeView(_Phase2JobMixin, APIView):
    """Phase-2 scaffold: accepts reel mode jobs."""

    def post(self, request):
        return self._accept_job(request, job_type="reel", default_mode="standard")


class VideoProcessView(_Phase2JobMixin, APIView):
    """Phase-2 scaffold: accepts video processing jobs."""

    def post(self, request):
        return self._accept_job(request, job_type="video_reel", default_mode="standard")


class BackgroundCatalogView(APIView):
    """Returns static background catalog for Kotlin integration."""

    permission_classes = [AllowAny]

    def get(self, request):
        return Response(
            {
                "status": "ok",
                "backgrounds": [
                    {"id": "rain", "label": "Rain", "preview_url": "/media/backgrounds/previews/rain_20s.mp3", "default_volume": 0.15},
                    {"id": "cafe", "label": "Cafe", "preview_url": "/media/backgrounds/previews/cafe_20s.mp3", "default_volume": 0.15},
                    {"id": "ocean", "label": "Ocean", "preview_url": "/media/backgrounds/previews/ocean_20s.mp3", "default_volume": 0.15},
                    {"id": "forest", "label": "Forest", "preview_url": "/media/backgrounds/previews/forest_20s.mp3", "default_volume": 0.15},
                    {"id": "street", "label": "Street", "preview_url": "/media/backgrounds/previews/street_20s.mp3", "default_volume": 0.15},
                ],
            },
            status=status.HTTP_200_OK,
        )
'''

needle = "'processing_modes': {"
if needle in text and "'available_job_types'" not in text:
    text = text.replace(
        needle,
        "'capabilities': {'adaptive_runtime': True, 'quality_retry': True, 'dry_mix_guard': True, 'loudness_floor_guard': True},\n            'available_job_types': ['clean', 'extract_voice', 'add_background', 'reel', 'video_reel'],\n            'processing_modes': {",
        1,
    )

p.write_text(text)
print('patched', p)
