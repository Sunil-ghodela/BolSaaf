from pathlib import Path

p = Path("/var/www/simplelms/backend/apps/voice/views.py")
text = p.read_text()

if "import threading" not in text:
    text = text.replace("import logging\n", "import logging\nimport threading\n", 1)

if "from .services.demucs_extract import run_extract_voice_job" not in text:
    text = text.replace(
        "from .services.studio_pipeline import StudioPipeline, ProcessingMode\n",
        "from .services.studio_pipeline import StudioPipeline, ProcessingMode\n"
        "from .services.demucs_extract import run_extract_voice_job\n",
        1,
    )

old_extract = """class ExtractVoiceView(_Phase2JobMixin, APIView):
    \"\"\"Phase-2 scaffold: accepts extract voice jobs.\"\"\"

    def post(self, request):
        return self._accept_job(request, job_type=\"extract_voice\", default_mode=\"studio\")
"""

new_extract = """class ExtractVoiceView(_Phase2JobMixin, APIView):
    \"\"\"Phase-2: async extract voice job using Demucs.\"\"\"

    def post(self, request):
        uploaded_file = request.FILES.get(\"file\")
        if not uploaded_file:
            return Response({\"status\": \"error\", \"message\": \"file is required\"}, status=status.HTTP_400_BAD_REQUEST)

        mode = (request.data.get(\"mode\") or \"studio\").strip().lower()
        mode = mode if mode in (\"studio\", \"pro\") else \"studio\"

        job = AudioFile.objects.create(
            original_file=uploaded_file,
            original_size=getattr(uploaded_file, \"size\", None),
            processing_mode=mode,
            status=\"pending\",
        )

        t = threading.Thread(target=run_extract_voice_job, args=(job.id, mode), daemon=True)
        t.start()

        return Response(
            {
                \"status\": \"accepted\",
                \"job_id\": job.id,
                \"job_type\": \"extract_voice\",
                \"message\": \"Job queued\",
            },
            status=status.HTTP_202_ACCEPTED,
        )
"""

if old_extract not in text:
    raise SystemExit("ExtractVoiceView scaffold block not found")
text = text.replace(old_extract, new_extract, 1)

if "'output_audio_url'" not in text:
    text = text.replace(
        "'cleaned_url': audio_file.cleaned_file.url if audio_file.cleaned_file else None,\n",
        "'cleaned_url': audio_file.cleaned_file.url if audio_file.cleaned_file else None,\n"
        "                'output_audio_url': audio_file.cleaned_file.url if audio_file.cleaned_file else None,\n"
        "                'job_type': 'extract_voice' if (audio_file.cleaned_file and 'extract_' in str(audio_file.cleaned_file.name)) else 'clean',\n",
        1,
    )

p.write_text(text)
print("patched", p)
