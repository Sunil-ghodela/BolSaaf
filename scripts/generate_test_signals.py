#!/usr/bin/env python3
"""Write synthetic mono 48kHz 16-bit WAVs under testdata/wav/ for offline benchmarks (no ffmpeg)."""
import math
import struct
import wave
from pathlib import Path

OUT = Path(__file__).resolve().parents[1] / "testdata" / "wav"


def write_wav(path: Path, samples: list[int], rate: int = 48000) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with wave.open(str(path), "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(rate)
        for s in samples:
            s = max(-32768, min(32767, int(s)))
            w.writeframes(struct.pack("<h", s))


def sine(frames: int, freq: float, amp: float, rate: int = 48000) -> list[int]:
    out = []
    for i in range(frames):
        v = amp * 32767.0 * math.sin(2 * math.pi * freq * i / rate)
        out.append(int(v))
    return out


def main() -> None:
    sr = 48000
    half = sr // 2

    write_wav(OUT / "sine_quiet.wav", sine(half, 440.0, 0.004))
    write_wav(OUT / "sine_nominal.wav", sine(half, 440.0, 0.08))
    write_wav(OUT / "square_clip.wav", [32000 if (i // 100) % 2 == 0 else -32000 for i in range(half)])
    noise = [int((hash(str(i)) % 2000) - 1000) for i in range(half)]
    write_wav(OUT / "sparse_noise.wav", noise)
    print(f"Wrote WAVs under {OUT}")


if __name__ == "__main__":
    main()
