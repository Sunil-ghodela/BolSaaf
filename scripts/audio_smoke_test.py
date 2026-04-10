#!/usr/bin/env python3
import argparse
import audioop
import hashlib
import json
import subprocess
import sys
import wave
from pathlib import Path


DEVICE_AUDIO_DIR = "/sdcard/Android/data/com.bolsaaf/files/Music/BolSaaf"


def run(cmd: list[str]) -> str:
    p = subprocess.run(cmd, capture_output=True, text=True)
    if p.returncode != 0:
        raise RuntimeError(f"Command failed: {' '.join(cmd)}\n{p.stderr.strip()}")
    return p.stdout


def latest_pair() -> tuple[str, str]:
    out = run(["adb", "shell", "ls", "-t", DEVICE_AUDIO_DIR])
    names = [x.strip() for x in out.splitlines() if x.strip()]
    cleaned = next((n for n in names if n.startswith("cleaned_") and n.endswith(".wav")), None)
    if not cleaned:
        raise RuntimeError("No cleaned_*.wav file found on device.")
    original = cleaned.replace("cleaned_", "original_", 1)
    if original not in names:
        raise RuntimeError(f"Matching original file not found for {cleaned}")
    return original, cleaned


def pull_files(original_name: str, cleaned_name: str, target_dir: Path) -> tuple[Path, Path]:
    target_dir.mkdir(parents=True, exist_ok=True)
    original_local = target_dir / original_name
    cleaned_local = target_dir / cleaned_name
    run(["adb", "pull", f"{DEVICE_AUDIO_DIR}/{original_name}", str(original_local)])
    run(["adb", "pull", f"{DEVICE_AUDIO_DIR}/{cleaned_name}", str(cleaned_local)])
    return original_local, cleaned_local


def wav_stats(path: Path) -> tuple[dict, bytes]:
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
        "rms": audioop.rms(raw, sw),
        "peak": audioop.max(raw, sw),
        "sha256": hashlib.sha256(raw).hexdigest(),
    }, raw


def compare(a_raw: bytes, b_raw: bytes, sample_width: int) -> int:
    m = min(len(a_raw), len(b_raw))
    if m == 0:
        return 0
    diff = audioop.add(a_raw[:m], audioop.mul(b_raw[:m], sample_width, -1.0), sample_width)
    return audioop.rms(diff, sample_width)


def main() -> int:
    parser = argparse.ArgumentParser(description="BolSaaf device audio smoke test")
    parser.add_argument(
        "--out-dir",
        default="tmp_audio/device_checks",
        help="Local directory to pull test files into",
    )
    parser.add_argument(
        "--min-rms-diff",
        type=int,
        default=5,
        help="Minimum RMS difference to consider processing changed signal",
    )
    parser.add_argument(
        "--max-duration-drift-sec",
        type=float,
        default=0.2,
        help="Maximum allowed duration drift between original and cleaned",
    )
    args = parser.parse_args()

    try:
        run(["adb", "get-state"])
        original_name, cleaned_name = latest_pair()
        original_path, cleaned_path = pull_files(original_name, cleaned_name, Path(args.out_dir))

        o_stats, o_raw = wav_stats(original_path)
        c_stats, c_raw = wav_stats(cleaned_path)

        rms_diff = compare(o_raw, c_raw, o_stats["sample_width"])
        duration_drift = abs(o_stats["duration_sec"] - c_stats["duration_sec"])
        bit_identical = o_stats["sha256"] == c_stats["sha256"]

        checks = {
            "same_sample_rate": o_stats["sample_rate"] == c_stats["sample_rate"],
            "same_channels": o_stats["channels"] == c_stats["channels"],
            "duration_within_limit": duration_drift <= args.max_duration_drift_sec,
            "signal_changed": (not bit_identical) and (rms_diff >= args.min_rms_diff),
        }
        passed = all(checks.values())

        report = {
            "passed": passed,
            "original_file": str(original_path),
            "cleaned_file": str(cleaned_path),
            "checks": checks,
            "metrics": {
                "duration_drift_sec": round(duration_drift, 4),
                "rms_diff": rms_diff,
                "bit_identical": bit_identical,
                "original": o_stats,
                "cleaned": c_stats,
            },
        }
        print(json.dumps(report, indent=2))
        return 0 if passed else 2
    except Exception as e:
        print(f"Smoke test failed: {e}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    sys.exit(main())
