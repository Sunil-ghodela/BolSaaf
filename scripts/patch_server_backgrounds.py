import sys

new_map = '''BG_MAP = {
    "rain": "rain.mp3",
    "cafe": "cafe.mp3",
    "ocean": "ocean.mp3",
    "forest": "forest.mp3",
    "street": "street.mp3",
    "bhakti": "bhakti.mp3",
    "cricket": "cricket.mp3",
    "confession": "confession.mp3",
    "study": "study.mp3",
}'''

for f in ["apps/voice/services/background_mix.py", "apps/voice/services/reel_mode.py"]:
    s = open(f).read()
    old_map = '''BG_MAP = {
    "rain": "rain.mp3",
    "cafe": "cafe.mp3",
    "ocean": "ocean.mp3",
    "forest": "forest.mp3",
    "street": "street.mp3",
}'''
    if old_map not in s:
        print(f"WARN: map not found in {f}")
        sys.exit(1)
    s = s.replace(old_map, new_map)
    open(f, "w").write(s)

s = open("apps/voice/views.py").read()
old_line = '{"id": "street", "label": "Street", "preview_url": "/media/backgrounds/previews/street_20s.mp3", "default_volume": 0.15},'
if old_line not in s:
    print("WARN: street catalog line not found")
    sys.exit(1)
entries = [
    ("bhakti", "Bhakti", "0.20"),
    ("cricket", "Cricket", "0.25"),
    ("confession", "Confession", "0.12"),
    ("study", "Study", "0.18"),
]
add = "\n".join(
    '                    {"id": "%s", "label": "%s", "preview_url": "/media/backgrounds/previews/%s_20s.mp3", "default_volume": %s},' % (b, l, b, v)
    for b, l, v in entries
)
s = s.replace(old_line, old_line + "\n" + add)
open("apps/voice/views.py", "w").write(s)
print("patched OK")
