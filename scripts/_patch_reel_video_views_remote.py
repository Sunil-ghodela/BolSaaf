from pathlib import Path

p = Path("/var/www/simplelms/backend/apps/voice/views.py")
text = p.read_text()

if "from .services.reel_mode import run_reel_job" not in text:
    text = text.replace(
        "from .services.background_mix import run_add_background_job\n",
        "from .services.background_mix import run_add_background_job\n"
        "from .services.reel_mode import run_reel_job\n"
        "from .services.video_process import run_video_reel_job\n",
        1,
    )

old_reel = """class ReelModeView(_Phase2JobMixin, APIView):
    \"\"\"Phase-2 scaffold: accepts reel mode jobs.\"\"\"

    def post(self, request):
        return self._accept_job(request, job_type=\"reel\", default_mode=\"standard\")
"""

new_reel = """class ReelModeView(_Phase2JobMixin, APIView):
    \"\"\"Phase-2: async reel mode job (adaptive -> clean/extract -> bg -> loudnorm).\"\"\"

    def post(self, request):
        uploaded_file = request.FILES.get(\"file\")
        if not uploaded_file:
            return Response({\"status\": \"error\", \"message\": \"file is required\"}, status=status.HTTP_400_BAD_REQUEST)

        mode = (request.data.get(\"mode\") or \"standard\").strip().lower()
        mode = mode if mode in (\"basic\", \"standard\", \"studio\", \"pro\", \"fast\") else \"standard\"
        try:
            target_lufs = float(request.data.get(\"target_lufs\", -16))
        except Exception:
            target_lufs = -16.0
        bg = (request.data.get(\"bg\") or \"\").strip().lower() or None
        try:
            bg_volume = float(request.data.get(\"bg_volume\", 0.15))
        except Exception:
            bg_volume = 0.15
        extract_raw = str(request.data.get(\"extract_voice\", \"false\")).strip().lower()
        extract_voice = extract_raw in (\"1\", \"true\", \"yes\", \"on\")
        preset_id = (request.data.get(\"preset_id\") or \"\").strip().lower() or None
        adaptive_raw = str(request.data.get(\"adaptive\", \"true\")).strip().lower()
        adaptive = adaptive_raw in (\"1\", \"true\", \"yes\", \"on\")

        job = AudioFile.objects.create(
            original_file=uploaded_file,
            original_size=getattr(uploaded_file, \"size\", None),
            processing_mode=mode,
            status=\"pending\",
        )

        t = threading.Thread(
            target=run_reel_job,
            args=(job.id, mode, target_lufs, bg, bg_volume, extract_voice, preset_id, adaptive),
            daemon=True,
        )
        t.start()

        return Response(
            {
                \"status\": \"accepted\",
                \"job_id\": job.id,
                \"job_type\": \"reel\",
                \"message\": \"Job queued\",
                \"requested\": {
                    \"mode\": mode,
                    \"bg\": bg,
                    \"bg_volume\": bg_volume,
                    \"extract_voice\": extract_voice,
                    \"preset_id\": preset_id,
                    \"adaptive\": adaptive,
                },
            },
            status=status.HTTP_202_ACCEPTED,
        )
"""

old_video = """class VideoProcessView(_Phase2JobMixin, APIView):
    \"\"\"Phase-2 scaffold: accepts video processing jobs.\"\"\"

    def post(self, request):
        return self._accept_job(request, job_type=\"video_reel\", default_mode=\"standard\")
"""

new_video = """class VideoProcessView(_Phase2JobMixin, APIView):
    \"\"\"Phase-2: async video reel job (extract audio, clean, remux).\"\"\"

    def post(self, request):
        uploaded_file = request.FILES.get(\"file\")
        if not uploaded_file:
            return Response({\"status\": \"error\", \"message\": \"file is required\"}, status=status.HTTP_400_BAD_REQUEST)

        mode = (request.data.get(\"mode\") or \"standard\").strip().lower()
        mode = mode if mode in (\"basic\", \"standard\", \"studio\", \"pro\", \"fast\") else \"standard\"
        try:
            target_lufs = float(request.data.get(\"target_lufs\", -16))
        except Exception:
            target_lufs = -16.0
        bg = (request.data.get(\"bg\") or \"\").strip().lower() or None
        try:
            bg_volume = float(request.data.get(\"bg_volume\", 0.15))
        except Exception:
            bg_volume = 0.15
        extract_raw = str(request.data.get(\"extract_voice\", \"false\")).strip().lower()
        extract_voice = extract_raw in (\"1\", \"true\", \"yes\", \"on\")
        preset_id = (request.data.get(\"preset_id\") or \"\").strip().lower() or None
        adaptive_raw = str(request.data.get(\"adaptive\", \"true\")).strip().lower()
        adaptive = adaptive_raw in (\"1\", \"true\", \"yes\", \"on\")

        job = AudioFile.objects.create(
            original_file=uploaded_file,
            original_size=getattr(uploaded_file, \"size\", None),
            processing_mode=mode,
            status=\"pending\",
        )

        t = threading.Thread(
            target=run_video_reel_job,
            args=(job.id, mode, target_lufs, bg, bg_volume, extract_voice, preset_id, adaptive),
            daemon=True,
        )
        t.start()

        return Response(
            {
                \"status\": \"accepted\",
                \"job_id\": job.id,
                \"job_type\": \"video_reel\",
                \"message\": \"Job queued\",
                \"requested\": {
                    \"mode\": mode,
                    \"target_lufs\": target_lufs,
                    \"bg\": bg,
                    \"bg_volume\": bg_volume,
                    \"extract_voice\": extract_voice,
                    \"preset_id\": preset_id,
                    \"adaptive\": adaptive,
                },
            },
            status=status.HTTP_202_ACCEPTED,
        )
"""

if old_reel not in text:
    raise SystemExit("ReelModeView scaffold block not found")
if old_video not in text:
    raise SystemExit("VideoProcessView scaffold block not found")

text = text.replace(old_reel, new_reel, 1)
text = text.replace(old_video, new_video, 1)

old_infer = """            if audio_file.cleaned_file:
                name = str(audio_file.cleaned_file.name)
                if 'extract_' in name:
                    job_type = 'extract_voice'
                elif 'mix_' in name:
                    job_type = 'add_background'
"""

new_infer = """            if audio_file.cleaned_file:
                name = str(audio_file.cleaned_file.name)
                if 'extract_' in name:
                    job_type = 'extract_voice'
                elif 'mix_' in name:
                    job_type = 'add_background'
                elif 'reel_' in name:
                    job_type = 'reel'
                elif 'video_' in name:
                    job_type = 'video_reel'
"""

if old_infer in text:
    text = text.replace(old_infer, new_infer, 1)

old_outputs = """                'cleaned_url': cleaned_url,
                'output_audio_url': cleaned_url,
                'output_video_url': None,
"""
new_outputs = """                'cleaned_url': cleaned_url,
                'output_audio_url': cleaned_url if job_type != 'video_reel' else None,
                'output_video_url': cleaned_url if job_type == 'video_reel' else None,
"""
if old_outputs in text:
    text = text.replace(old_outputs, new_outputs, 1)

p.write_text(text)
print("patched", p)
