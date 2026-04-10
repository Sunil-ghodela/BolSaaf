# Reel / clean benchmark — 5 real takes

**Goal:** Replace guessing with a small, repeatable listening + metrics pass.  
Fill one row per recording after running `scripts/adaptive_audio_analyze.py` on exported WAV (or device pull).

## Takes

| # | File / ID | Scenario (where / mic) | Adaptive flags (from script) | Suggested preset | Conf. | RMS in dBFS | Peak dBFS | Notes (listen) |
|---|-----------|-------------------------|------------------------------|------------------|-------|-------------|-----------|----------------|
| 1 | | | | | | | | |
| 2 | | | | | | | | |
| 3 | | | | | | | | |
| 4 | | | | | | | | |
| 5 | | | | | | | | |

## Listening notes (short)

1. **Take ___:** natural / thin / pumping / artifacts:  
2. **Take ___:**  
3. **Take ___:**  
4. **Take ___:**  
5. **Take ___:**  

## Decisions

- Threshold tweaks needed (which flag misfired):  
- Server reel pipeline gap vs app (BG / LUFS / video):  

## Commands

```bash
python3 scripts/adaptive_audio_analyze.py path/to/take.wav
python3 scripts/pair_quality_guard.py original.wav cleaned.wav
```
