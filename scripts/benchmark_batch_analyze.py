#!/usr/bin/env python3
"""Run adaptive_audio_analyze on every *.wav under testdata/ (recursive)."""
from __future__ import annotations

import json
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SCRIPT = Path(__file__).resolve().parent / "adaptive_audio_analyze.py"


def main() -> int:
    testdata = ROOT / "testdata"
    if not testdata.is_dir():
        print("Run scripts/generate_test_signals.py first.", file=sys.stderr)
        return 1
    wavs = sorted(testdata.rglob("*.wav"))
    if not wavs:
        print("No WAV files under testdata/", file=sys.stderr)
        return 1
    rows = []
    for w in wavs:
        p = subprocess.run(
            [sys.executable, str(SCRIPT), str(w)],
            capture_output=True,
            text=True,
        )
        if p.returncode != 0:
            print(p.stderr, file=sys.stderr)
            return p.returncode
        rows.append(json.loads(p.stdout))
    summary = {"count": len(rows), "files": rows}
    out_path = ROOT / "data" / "benchmark_adaptive_batch.json"
    out_path.parent.mkdir(parents=True, exist_ok=True)
    out_path.write_text(json.dumps(summary, indent=2), encoding="utf-8")
    print(json.dumps(summary, indent=2))
    print(f"\nWrote {out_path}", file=sys.stderr)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
