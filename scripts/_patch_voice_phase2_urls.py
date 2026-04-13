from pathlib import Path

p = Path("/var/www/simplelms/backend/apps/voice/urls.py")
text = p.read_text()

text = text.replace(
    "from .views import CleanAudioView, JobStatusView, HealthCheckView",
    "from .views import ("
    "CleanAudioView, JobStatusView, HealthCheckView, "
    "ExtractVoiceView, AddBackgroundView, ReelModeView, VideoProcessView, BackgroundCatalogView"
    ")",
)

new_paths = """
    # Phase-2 scaffold endpoints
    path('extract_voice/', ExtractVoiceView.as_view(), name='extract_voice'),
    path('add_background/', AddBackgroundView.as_view(), name='add_background'),
    path('reel/', ReelModeView.as_view(), name='reel_mode'),
    path('video/process/', VideoProcessView.as_view(), name='video_process'),
    path('backgrounds/', BackgroundCatalogView.as_view(), name='background_catalog'),
"""

if "extract_voice/" not in text:
    text = text.replace(
        "    # Health check\n    path('health/', HealthCheckView.as_view(), name='health_check'),",
        "    # Health check\n    path('health/', HealthCheckView.as_view(), name='health_check'),\n"
        + new_paths,
    )

p.write_text(text)
print("patched", p)
