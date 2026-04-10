from pathlib import Path

p = Path("/var/www/simplelms/backend/apps/voice/views.py")
text = p.read_text()

old = """            response_data = {
                # v2 contract: top-level status is the job state
                'job_id': audio_file.id,
                'status': audio_file.status,
                'duration': audio_file.duration,
                'processing_mode': audio_file.processing_mode,
                'cleaned_url': audio_file.cleaned_file.url if audio_file.cleaned_file else None,
                'error_message': audio_file.error_message if audio_file.status == 'failed' else None,
                'created_at': audio_file.created_at,
                'updated_at': audio_file.updated_at,
                # Backward compatibility fields for older clients
                'state': audio_file.status,
                'mode': audio_file.processing_mode,
                'processing_time': audio_file.processing_time,
            }
"""

new = """            cleaned_url = audio_file.cleaned_file.url if audio_file.cleaned_file else None
            job_type = 'extract_voice' if (audio_file.cleaned_file and 'extract_' in str(audio_file.cleaned_file.name)) else 'clean'

            response_data = {
                # v2 contract: top-level status is the job state
                'job_id': audio_file.id,
                'status': audio_file.status,
                'job_type': job_type,
                'duration': audio_file.duration,
                'processing_mode': audio_file.processing_mode,
                'cleaned_url': cleaned_url,
                'output_audio_url': cleaned_url,
                'output_video_url': None,
                'error_message': audio_file.error_message if audio_file.status == 'failed' else None,
                'created_at': audio_file.created_at,
                'updated_at': audio_file.updated_at,
                # Backward compatibility fields for older clients
                'state': audio_file.status,
                'mode': audio_file.processing_mode,
                'processing_time': audio_file.processing_time,
            }
"""

if old not in text:
    raise SystemExit("status response block not found")

p.write_text(text.replace(old, new, 1))
print("patched", p)
