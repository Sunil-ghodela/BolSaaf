#!/usr/bin/env python3
"""Adaptive analysis mirror for Kotlin thresholds."""
from __future__ import annotations

import argparse
import json
import math
import struct
import wave
from pathlib import Path

FULL_SCALE = 32768.0
MAX_SEC = 20


def read_wav_pcm(path: Path) -> tuple[list[int], int]:
    with wave.open(str(path), "rb") as w:
        ch = w.getnchannels()
        sw = w.getsampwidth()
        sr = w.getframerate()
        if sw != 2:
            raise SystemExit(f"Need 16-bit WAV, got sampwidth={sw}")
        raw = w.readframes(w.getnframes())
    samples = [struct.unpack_from("<h", raw, i)[0] for i in range(0, len(raw), 2 * ch)]
    return samples, sr


def confidence(rms_db: float, peak_db: float, near_zero_pct: float, zero_pct: float, flags: list[str]) -> float:
    c = 0.48
    if rms_db < -60 or rms_db > -45:
        c += 0.16
    if near_zero_pct > 65 or near_zero_pct < 25:
        c += 0.14
    if peak_db > -3:
        c += 0.12
    if zero_pct > 40:
        c += 0.08
    c += min(len(flags), 4) * 0.04
    return max(0.35, min(0.96, c))


def analyze(samples: list[int], sample_rate: int) -> dict:
    if not samples:
        return {
            "rms_dbfs": -120.0,
            "peak_dbfs": -120.0,
            "suggested_preset": "NORMAL",
            "suggested_cloud_mode": "standard",
            "flags": ["empty_signal"],
            "confidence": 0.0,
            "adaptive_preset": {
                "preGain": 2.5,
                "denoiseLevel": "MEDIUM",
                "compressorStrength": "MEDIUM",
                "dryMix": 0.1,
                "mode": "standard",
            },
        }

    window = samples[: max(1, sample_rate * MAX_SEC)]
    n = len(window)
    sum_sq = 0.0
    peak = 0
    zeros = 0
    near_zero = 0
    near_clip = 0
    for v in window:
        av = abs(v)
        sum_sq += v * v
        peak = max(peak, av)
        if v == 0:
            zeros += 1
        if av <= 8:
            near_zero += 1
        if av >= 31000:
            near_clip += 1

    rms = math.sqrt(sum_sq / n)
    rms_db = 20 * math.log10(rms / FULL_SCALE) if rms > 1e-12 else -120.0
    peak_db = 20 * math.log10(peak / FULL_SCALE) if peak > 0 else -120.0
    crest = (peak / rms) if rms > 1e-6 else float(peak)
    crest_db = 20 * math.log10(max(crest, 1e-6))

    zero_frac = zeros / n
    near_zero_frac = near_zero / n
    clip_frac = near_clip / n

    frame_len = max(sample_rate // 50, 160)
    quiet_frames = 0
    frame_count = 0
    i = 0
    while i + frame_len <= len(window):
        fs = sum(window[j] * window[j] for j in range(i, i + frame_len))
        fr = math.sqrt(fs / frame_len)
        fr_db = 20 * math.log10(fr / FULL_SCALE) if fr > 1e-12 else -120.0
        if fr_db < -50.0:
            quiet_frames += 1
        frame_count += 1
        i += frame_len
    quiet_frame_frac = quiet_frames / frame_count if frame_count else 0.0

    near_zero_pct = near_zero_frac * 100.0
    zero_pct = zero_frac * 100.0

    pre_gain = 2.5
    denoise = "MEDIUM"
    compressor = "MEDIUM"
    dry_mix = 0.1
    mode = "standard"
    flags: list[str] = []

    if rms_db < -60:
        pre_gain = 4.0
        denoise = "STRONG"
        mode = "studio"
        flags.append("very_low_rms")
    elif rms_db > -45:
        pre_gain = 1.5
        denoise = "LIGHT"
        mode = "standard"
        flags.append("loud_input")
    else:
        pre_gain = 2.5
        denoise = "MEDIUM"
        mode = "standard"
        flags.append("normal_rms")

    if near_zero_pct > 65:
        denoise = "STRONG"
        flags.append("high_noise_near_zero")
    elif near_zero_pct > 40:
        denoise = "MEDIUM"
        flags.append("moderate_noise_near_zero")
    else:
        denoise = "LIGHT"
        flags.append("cleaner_input")

    if peak_db > -3:
        pre_gain -= 1.0
        compressor = "STRONG"
        flags.append("clipping_risk")

    if zero_pct > 40:
        dry_mix = 0.15
        flags.append("hollow_risk_zero")

    pre_gain = max(0.8, min(6.0, pre_gain))
    suggested_preset = {"STRONG": "STRONG", "MEDIUM": "NORMAL", "LIGHT": "STUDIO"}[denoise]

    return {
        "rms_dbfs": round(rms_db, 2),
        "peak_dbfs": round(peak_db, 2),
        "crest_factor_db": round(crest_db, 2),
        "zero_sample_fraction": round(zero_frac, 4),
        "near_zero_fraction": round(near_zero_frac, 4),
        "near_clip_fraction": round(clip_frac, 4),
        "quiet_frame_fraction": round(quiet_frame_frac, 4),
        "suggested_preset": suggested_preset,
        "suggested_cloud_mode": mode,
        "flags": flags,
        "confidence": round(confidence(rms_db, peak_db, near_zero_pct, zero_pct, flags), 3),
        "adaptive_preset": {
            "preGain": round(pre_gain, 3),
            "denoiseLevel": denoise,
            "compressorStrength": compressor,
            "dryMix": dry_mix,
            "mode": mode,
        },
    }


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("wav", type=Path, help="Input WAV path")
    args = ap.parse_args()
    samples, sr = read_wav_pcm(args.wav)
    out = analyze(samples, sr)
    out["file"] = str(args.wav)
    print(json.dumps(out, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
