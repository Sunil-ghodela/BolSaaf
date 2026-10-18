#!/usr/bin/env python3
"""Apply the conservative high-frequency cleanup to the deployed Reel path."""
from pathlib import Path

path = Path("/var/www/simplelms/backend/apps/voice/services/reel_mode.py")
text = path.read_text(encoding="utf-8")

if '"-af", "lowpass=f=7500"' in text and "vocals_lp.wav" in text:
    print("reel_mode.py: lowpass already present")
    raise SystemExit(0)

needle = "    vocals = _resolve_vocals_file(out_dir, input_wav.stem)\n"
insert = needle + '''    # Keep Demucs separation residue from becoming bright granular noise.\n    filtered = output_wav.parent / "vocals_lp.wav"\n    lp_cmd = [\n        "ffmpeg", "-y", "-loglevel", "error",\n        "-i", str(vocals),\n        "-af", "lowpass=f=7500",\n        "-c:a", "pcm_s16le",\n        str(filtered),\n    ]\n    lp = subprocess.run(lp_cmd, capture_output=True, text=True)\n    if lp.returncode == 0 and filtered.exists():\n        vocals = filtered\n'''
if needle not in text:
    raise SystemExit("reel_mode.py: extraction anchor not found")
path.write_text(text.replace(needle, insert, 1), encoding="utf-8")
print("reel_mode.py: lowpass patch applied")
