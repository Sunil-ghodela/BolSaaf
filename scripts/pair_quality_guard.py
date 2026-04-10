#!/usr/bin/env python3
"""Phase 5: compare two mono PCM16 WAVs (original vs cleaned); print JSON guard report."""
from __future__ import annotations

import argparse
import json
import math
import struct
import wave
from pathlib import Path


def read_wav_pcm(path: Path) -> list[int]:
    with wave.open(str(path), "rb") as w:
        if w.getnchannels() != 1 or w.getsampwidth() != 2:
            raise SystemExit(f"Need mono 16-bit WAV: {path}")
        raw = w.readframes(w.getnframes())
    return [struct.unpack_from("<h", raw, i)[0] for i in range(0, len(raw), 2)]


def rms_peak(samples: list[int]) -> tuple[float, int]:
    n = len(samples)
    if n == 0:
        return 0.0, 0
    s = sum(x * x for x in samples)
    rms = math.sqrt(s / n)
    peak = max(abs(x) for x in samples)
    return rms, peak


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("original", type=Path)
    ap.add_argument("cleaned", type=Path)
    args = ap.parse_args()
    o = read_wav_pcm(args.original)
    c = read_wav_pcm(args.cleaned)
    n = min(len(o), len(c))
    o, c = o[:n], c[:n]
    rms_o, peak_o = rms_peak(o)
    rms_c, peak_c = rms_peak(c)
    rms_delta_db = 20 * math.log10(rms_c / rms_o) if rms_o > 1e-8 else 0.0
    peak_delta_db = (
        20 * math.log10(peak_c / peak_o) if peak_o > 0 else 0.0
    )
    rms_out_dbfs = 20 * math.log10(rms_c / 32768.0) if rms_c > 1e-12 else -120.0
    issues = []
    if rms_delta_db < -8:
        issues.append("heavy_rms_drop")
    if peak_delta_db < -10 and peak_o > 1000:
        issues.append("peak_collapsed")
    if rms_out_dbfs < -55:
        issues.append("output_very_quiet")
    report = {
        "pass": len(issues) == 0,
        "rms_delta_db": round(rms_delta_db, 3),
        "peak_delta_db": round(peak_delta_db, 3),
        "rms_out_dbfs": round(rms_out_dbfs, 2),
        "issues": issues,
        "frames_compared": n,
    }
    print(json.dumps(report, indent=2))
    return 0 if report["pass"] else 2


if __name__ == "__main__":
    raise SystemExit(main())
