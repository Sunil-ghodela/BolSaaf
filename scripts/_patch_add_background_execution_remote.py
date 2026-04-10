from pathlib import Path

p = Path("/var/www/simplelms/backend/apps/voice/views.py")
text = p.read_text()

if "from .services.background_mix import run_add_background_job" not in text:
    text = text.replace(
        "from .services.demucs_extract import run_extract_voice_job\n",
        "from .services.demucs_extract import run_extract_voice_job\n"
        "from .services.background_mix import run_add_background_job\n",
        1,
    )

old_add_bg = """class AddBackgroundView(_Phase2JobMixin, APIView):
    \"\"\"Phase-2 scaffold: accepts add background jobs.\"\"\"

    def post(self, request):
        bg = (request.data.get(\"bg\") or \"\").strip().lower()
        if bg not in {\"rain\", \"cafe\", \"ocean\", \"forest\", \"street\"}:
            return Response(
                {\"status\": \"error\", \"message\": \"bg must be one of: rain, cafe, ocean, forest, street\"},
                status=status.HTTP_400_BAD_REQUEST,
            )
        return self._accept_job(request, job_type=\"add_background\", default_mode=\"standard\")
"""

new_add_bg = """class AddBackgroundView(_Phase2JobMixin, APIView):
    \"\"\"Phase-2: async add background mix job.\"\"\"

    def post(self, request):
        uploaded_file = request.FILES.get(\"file\")
        if not uploaded_file:
            return Response({\"status\": \"error\", \"message\": \"file is required\"}, status=status.HTTP_400_BAD_REQUEST)

        bg = (request.data.get(\"bg\") or \"\").strip().lower()
        if bg not in {\"rain\", \"cafe\", \"ocean\", \"forest\", \"street\"}:
            return Response(
                {\"status\": \"error\", \"message\": \"bg must be one of: rain, cafe, ocean, forest, street\"},
                status=status.HTTP_400_BAD_REQUEST,
            )

        mode = (request.data.get(\"mode\") or \"standard\").strip().lower()
        mode = mode if mode in (\"basic\", \"standard\", \"studio\", \"pro\", \"fast\") else \"standard\"
        try:
            bg_volume = float(request.data.get(\"bg_volume\", 0.15))
        except Exception:
            bg_volume = 0.15
        bg_volume = max(0.0, min(bg_volume, 1.0))

        job = AudioFile.objects.create(
            original_file=uploaded_file,
            original_size=getattr(uploaded_file, \"size\", None),
            processing_mode=mode,
            status=\"pending\",
        )

        t = threading.Thread(target=run_add_background_job, args=(job.id, bg, bg_volume), daemon=True)
        t.start()

        return Response(
            {
                \"status\": \"accepted\",
                \"job_id\": job.id,
                \"job_type\": \"add_background\",
                \"message\": \"Job queued\",
            },
            status=status.HTTP_202_ACCEPTED,
        )
"""

if old_add_bg not in text:
    raise SystemExit("AddBackgroundView scaffold block not found")
text = text.replace(old_add_bg, new_add_bg, 1)

text = text.replace(
    "            job_type = 'extract_voice' if (audio_file.cleaned_file and 'extract_' in str(audio_file.cleaned_file.name)) else 'clean'",
    "            job_type = 'clean'\n"
    "            if audio_file.cleaned_file:\n"
    "                name = str(audio_file.cleaned_file.name)\n"
    "                if 'extract_' in name:\n"
    "                    job_type = 'extract_voice'\n"
    "                elif 'mix_' in name:\n"
    "                    job_type = 'add_background'",
    1,
)

p.write_text(text)
print("patched", p)
