#!/usr/bin/env python3
"""
Run ON THE SERVER (paths below) after deploy.

Problem: cleaned_url uses Django FileField.url -> /media/cleaned/... but nginx sends
/media/* to Next.js, so mobile downloads get HTTP 404 HTML.

Fix: expose files at GET /voice/cleaned_output/<pk>/ (already routed to Django) and
return that path in JSON instead of cleaned_file.url.
"""
from pathlib import Path

VIEWS = Path("/var/www/simplelms/backend/apps/voice/views.py")
URLS = Path("/var/www/simplelms/backend/apps/voice/urls.py")

DOWNLOAD_VIEW = '''

class CleanedFileDownloadView(APIView):
    """
    Serve cleaned audio under /voice/ so the public host does not rely on /media/
    (often swallowed by Next.js on the same domain).
    """
    permission_classes = [AllowAny]

    def get(self, request, pk):
        from django.http import FileResponse
        from django.shortcuts import get_object_or_404

        af = get_object_or_404(AudioFile, pk=pk)
        if not af.cleaned_file:
            return Response(
                {"detail": "No cleaned file"},
                status=status.HTTP_404_NOT_FOUND,
            )
        fh = af.cleaned_file.open("rb")
        name = str(af.cleaned_file.name).rsplit("/", 1)[-1]
        return FileResponse(
            fh,
            content_type="audio/wav",
            as_attachment=False,
            filename=name,
        )
'''

REPLACEMENTS = [
    (
        "'cleaned_url': audio_file.cleaned_file.url if audio_file.cleaned_file else None,",
        "'cleaned_url': (f\"/voice/cleaned_output/{audio_file.id}/\" if audio_file.cleaned_file else None),",
    ),
    (
        "cleaned_url = audio_file.cleaned_file.url if audio_file.cleaned_file else None",
        "cleaned_url = (f\"/voice/cleaned_output/{audio_file.id}/\" if audio_file.cleaned_file else None)",
    ),
]


def patch_views() -> None:
    text = VIEWS.read_text()
    if "class CleanedFileDownloadView" not in text:
        text = text.rstrip() + "\n" + DOWNLOAD_VIEW + "\n"
        print("appended CleanedFileDownloadView")
    else:
        print("CleanedFileDownloadView already present; skip append")

    for old, new in REPLACEMENTS:
        if old in text:
            text = text.replace(old, new)
            print("replaced:", old[:50], "...")
        else:
            print("pattern not found (ok if already patched):", old[:60])

    VIEWS.write_text(text)


def patch_urls() -> None:
    text = URLS.read_text()
    if "voice_cleaned_output" in text or "cleaned_output/<int:pk>" in text:
        print("urls: cleaned_output already present")
        return

    if "CleanedFileDownloadView" not in text:
        needle = "from .views import ("
        if needle not in text:
            raise SystemExit(
                "urls.py: add `CleanedFileDownloadView` to the views import, then re-run"
            )
        text = text.replace(
            needle,
            needle + "\n    CleanedFileDownloadView,",
            1,
        )
        print("urls: added CleanedFileDownloadView to import")

    marker = "path('health/', HealthCheckView.as_view(), name='health_check'),"
    if marker not in text:
        marker = 'path("health/", HealthCheckView.as_view(), name="health_check"),'
    insert = (
        "\n    path('cleaned_output/<int:pk>/', "
        "CleanedFileDownloadView.as_view(), name='voice_cleaned_output'),"
    )
    if marker not in text:
        raise SystemExit("urls.py: could not find health_check path line to anchor insert")
    text = text.replace(marker, marker + insert, 1)
    print("urls: inserted cleaned_output path")

    URLS.write_text(text)


def main() -> None:
    if not VIEWS.is_file():
        raise SystemExit(f"missing {VIEWS}")
    if not URLS.is_file():
        raise SystemExit(f"missing {URLS}")
    patch_views()
    patch_urls()
    print("done — restart gunicorn/uwsgi and retry mobile download")


if __name__ == "__main__":
    main()
