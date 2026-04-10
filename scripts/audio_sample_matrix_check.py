#!/usr/bin/env python3
"""
Pull original/cleaned WAV pairs from device (BolSaaf folder), map to a fixed
preset × flow matrix (chronological by filename timestamp), print JSON + summary.
"""
from __future__ import annotations

import argparse
import audioop
import hashlib
import json
import math
import subprocess
import sys
import wave
from pathlib import Path

DEVICE_AUDIO_DIR = "/sdcard/Android/data/com.bolsaaf/files/Music/BolSaaf"

# Expected run order (chronological on device = user run order).
MATRIX_ROWS: list[tuple[str, str, str]] = [
    ("a", "normal", "clean"),
    ("a", "normal", "extract"),
    ("a", "normal", "bg_mix"),
    ("b", "strong", "clean"),
    ("b", "strong", "extract"),
    ("b", "strong", "bg_mix"),
    ("c", "studio", "clean"),
    ("c", "studio", "extract"),
    ("c", "studio", "bg_mix"),
]


def run(cmd: list[str]) -> str:
    p = subprocess.run(cmd, capture_output=True, text=True)
    if p.returncode != 0:
        raise RuntimeError(f"Command failed: {' '.join(cmd)}\n{p.stderr.strip()}")
    return p.stdout


def list_cleaned_names(date_prefix: str | None) -> list[str]:
    out = run(["adb", "shell", "ls", DEVICE_AUDIO_DIR])
    names = [x.strip() for x in out.splitlines() if x.strip()]
    cleaned = [n for n in names if n.startswith("cleaned_") and n.endswith(".wav")]
    cleaned.sort()
    if date_prefix:
        cleaned = [n for n in cleaned if n.startswith(f"cleaned_{date_prefix}")]
    return cleaned


def pull_pair(cleaned_name: str, target_dir: Path) -> tuple[Path, Path]:
    original_name = cleaned_name.replace("cleaned_", "original_", 1)
    target_dir.mkdir(parents=True, exist_ok=True)
    o_path = target_dir / original_name
    c_path = target_dir / cleaned_name
    run(["adb", "pull", f"{DEVICE_AUDIO_DIR}/{original_name}", str(o_path)])
    run(["adb", "pull", f"{DEVICE_AUDIO_DIR}/{cleaned_name}", str(c_path)])
    return o_path, c_path


def wav_pcm(path: Path) -> tuple[dict, bytes]:
    with wave.open(str(path), "rb") as w:
        frames = w.getnframes()
        sr = w.getframerate()
        ch = w.getnchannels()
        sw = w.getsampwidth()
        raw = w.readframes(frames)
    return {
        "frames": frames,
        "sample_rate": sr,
        "channels": ch,
        "sample_width": sw,
        "duration_sec": frames / sr if sr else 0,
    }, raw


def rms_to_dbfs(rms: int, peak_int: int = 32768) -> float:
    if rms <= 0:
        return -120.0
    return 20.0 * math.log10(rms / float(peak_int))


def peak_to_dbfs(peak: int, peak_int: int = 32768) -> float:
    if peak <= 0:
        return -120.0
    return 20.0 * math.log10(peak / float(peak_int))


def enrich_stats(meta: dict, raw: bytes) -> dict:
    sw = meta["sample_width"]
    rms = audioop.rms(raw, sw)
    peak = audioop.max(raw, sw)
    peak_int = 2 ** (8 * sw - 1)
    # 16-bit mono metrics
    if sw == 2:
        z = near = 0
        n = len(raw) // 2
        for i in range(0, len(raw), 2):
            v = int.from_bytes(raw[i : i + 2], "little", signed=True)
            if v == 0:
                z += 1
            if abs(v) <= 8:
                near += 1
        zero_pct = 100.0 * z / n if n else 0.0
        near_pct = 100.0 * near / n if n else 0.0
    else:
        zero_pct = near_pct = -1.0

    return {
        **meta,
        "rms_linear": rms,
        "peak_linear": peak,
        "rms_dbfs": round(rms_to_dbfs(rms, peak_int), 2),
        "peak_dbfs": round(peak_to_dbfs(peak, peak_int), 2),
        "zero_pct": round(zero_pct, 2) if zero_pct >= 0 else None,
        "near_zero_pct": round(near_pct, 2) if near_pct >= 0 else None,
        "sha256": hashlib.sha256(raw).hexdigest(),
    }


def compare_rms_diff(a_raw: bytes, b_raw: bytes, sample_width: int) -> int:
    m = min(len(a_raw), len(b_raw))
    if m == 0:
        return 0
    diff = audioop.add(a_raw[:m], audioop.mul(b_raw[:m], sample_width, -1.0), sample_width)
    return audioop.rms(diff, sample_width)


def analyze_pair(o_path: Path, c_path: Path) -> dict:
    o_meta, o_raw = wav_pcm(o_path)
    c_meta, c_raw = wav_pcm(c_path)
    o_s = enrich_stats(o_meta, o_raw)
    c_s = enrich_stats(c_meta, c_raw)
    rms_diff = compare_rms_diff(o_raw, c_raw, o_meta["sample_width"])
    duration_drift = abs(o_s["duration_sec"] - c_s["duration_sec"])
    bit_identical = o_s["sha256"] == c_s["sha256"]
    checks = {
        "same_sample_rate": o_s["sample_rate"] == c_s["sample_rate"],
        "same_channels": o_s["channels"] == c_s["channels"],
        "duration_within_0_2s": duration_drift <= 0.2,
        "signal_changed": (not bit_identical) and (rms_diff >= 5),
    }
    return {
        "original": o_s,
        "cleaned": c_s,
        "rms_diff_linear": rms_diff,
        "duration_drift_sec": round(duration_drift, 4),
        "bit_identical": bit_identical,
        "checks": checks,
        "smoke_passed": all(checks.values()),
    }


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--date", default=None, help="Filter cleaned_YYYYMMDD_* (e.g. 20260409)")
    ap.add_argument("--out-dir", default="tmp_audio/matrix_checks")
    args = ap.parse_args()

    try:
        run(["adb", "get-state"])
    except Exception as e:
        print(f"adb not ready: {e}", file=sys.stderr)
        return 1

    cleaned_list = list_cleaned_names(args.date)
    if not cleaned_list:
        print("No cleaned_*.wav files found (check --date filter).", file=sys.stderr)
        return 1

    target = Path(args.out_dir)
    rows_out: list[dict] = []

    for idx, cleaned_name in enumerate(cleaned_list):
        label = MATRIX_ROWS[idx] if idx < len(MATRIX_ROWS) else ("?", "unknown", "unknown")
        row_id, preset, flow = label
        try:
            o_path, c_path = pull_pair(cleaned_name, target)
            analysis = analyze_pair(o_path, c_path)
        except Exception as e:
            rows_out.append(
                {
                    "matrix_index": idx + 1,
                    "row": row_id,
                    "preset": preset,
                    "flow": flow,
                    "cleaned_file": cleaned_name,
                    "error": str(e),
                }
            )
            continue

        rows_out.append(
            {
                "matrix_index": idx + 1,
                "row": row_id,
                "preset": preset,
                "flow": flow,
                "cleaned_file": cleaned_name,
                **analysis,
            }
        )

    summary = {
        "date_filter": args.date,
        "pairs_found": len(cleaned_list),
        "matrix_slots": len(MATRIX_ROWS),
        "rows": rows_out,
    }
    print(json.dumps(summary, indent=2))

    # Human-readable one-liners
    print("\n--- summary ---", file=sys.stderr)
    for r in rows_out:
        if "error" in r:
            print(f"{r.get('row')} {r.get('preset')} {r.get('flow')}: ERROR {r['error']}", file=sys.stderr)
            continue
        ch = r["cleaned"]
        o = r["original"]
        sp = "PASS" if r["smoke_passed"] else "FAIL"
        print(
            f"{r['row']} {r['preset']} {r['flow']}: {sp} | "
            f"dur {o['duration_sec']:.2f}s | "
            f"RMS {o['rms_dbfs']:.1f} -> {ch['rms_dbfs']:.1f} dBFS | "
            f"peak {o['peak_dbfs']:.1f} -> {ch['peak_dbfs']:.1f} dBFS | "
            f"rms_diff={r['rms_diff_linear']}",
            file=sys.stderr,
        )

    if len(cleaned_list) < len(MATRIX_ROWS):
        print(
            f"\nNote: only {len(cleaned_list)} file(s); matrix expects up to {len(MATRIX_ROWS)} "
            f"(remaining labels unassigned).",
            file=sys.stderr,
        )

    failed = sum(1 for r in rows_out if r.get("error") or not r.get("smoke_passed", False))
    return 2 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
