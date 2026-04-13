# Test audio (local, no download)

Generate synthetic WAV benchmarks (48 kHz mono 16-bit):

```bash
python3 scripts/generate_test_signals.py
python3 scripts/benchmark_batch_analyze.py
```

Outputs a JSON summary under `data/benchmark_adaptive_batch.json`.

Human benchmark sheet: `data/BENCHMARK_REEL_5TAKE_TEMPLATE.md`.

MP3 samples in the repo root are for manual listening only; the Python analyzer expects WAV unless you add ffmpeg-based decode later.

Compare pulled original/cleaned pairs (Phase 5):

```bash
python3 scripts/pair_quality_guard.py /path/to/original.wav /path/to/cleaned.wav
```
