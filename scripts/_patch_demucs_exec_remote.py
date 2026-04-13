from pathlib import Path

p = Path("/var/www/simplelms/backend/apps/voice/services/demucs_extract.py")
text = p.read_text()
if "import sys" not in text:
    text = text.replace("import subprocess\n", "import subprocess\nimport sys\n", 1)

old = """    demucs_cmd = [
        "demucs",
        "--two-stems=vocals",
        "--out",
        str(out_dir),
        str(input_path),
    ]
"""
new = """    demucs_cmd = [
        sys.executable,
        "-m",
        "demucs.separate",
        "--two-stems=vocals",
        "--out",
        str(out_dir),
        str(input_path),
    ]
"""
if old not in text:
    raise SystemExit("demucs command block not found")
text = text.replace(old, new, 1)
p.write_text(text)
print("patched", p)
