import sys

f = "apps/voice/views.py"
s = open(f).read()

old = '''        bg = (request.data.get("bg") or "").strip().lower()
        if bg not in {"rain", "cafe", "ocean", "forest", "street"}:
            return Response(
                {"status": "error", "message": "bg must be one of: rain, cafe, ocean, forest, street"},
                status=status.HTTP_400_BAD_REQUEST,
            )'''
new = '''        bg = (request.data.get("bg") or "").strip().lower()
        from apps.voice.services.background_mix import BG_MAP as _BG_MAP
        if bg not in _BG_MAP:
            return Response(
                {"status": "error", "message": "bg must be one of: " + ", ".join(sorted(_BG_MAP))},
                status=status.HTTP_400_BAD_REQUEST,
            )'''
if old not in s:
    print("WARN: addbg validation block not found")
    sys.exit(1)
s = s.replace(old, new)
open(f, "w").write(s)
print("patched OK")
