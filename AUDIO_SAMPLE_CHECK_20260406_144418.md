# Audio Sample Check: `20260406_144418`

Date: 2026-04-06  
Build state: Single-pass stable chain (`HPF -> RNNoise (1 pass) -> Soft Gain -> Output`)  
Files checked:
- Input: `original_20260406_144418.wav`
- Output: `cleaned_20260406_144418.wav`

## File Integrity / Format Check

- Input and output are both valid WAV files.
- Sample rate: `48000 Hz`
- Channels: `1 (mono)`
- Bit depth: `16-bit PCM`
- Duration: `10.28 s` (both files)

## Input vs Output Metrics

### Input (`original_20260406_144418.wav`)
- RMS: `-65.53 dBFS`
- Peak: `-26.45 dBFS`
- Zero samples: `6.63%`
- Near-zero samples (`|x| <= 8`): `67.66%`

### Output (`cleaned_20260406_144418.wav`)
- RMS: `-70.33 dBFS`
- Peak: `-31.46 dBFS`
- Zero samples: `54.73%`
- Near-zero samples (`|x| <= 8`): `86.51%`

### Delta (`cleaned - original`)
- RMS: `-4.79 dB`
- Peak: `-5.01 dB`
- Zero samples: `+48.11%`
- Near-zero samples: `+18.86%`
- Frame p50: `-16.65 dB` delta

## Reference Comparison (Known Better Run)

Reference pair: `original_20260406_140036.wav` vs `cleaned_20260406_140036.wav`

Reference delta was:
- RMS: `-0.49 dB`
- Peak: `+0.87 dB`
- Zero samples: `+22.50%`
- Near-zero samples: `+18.62%`
- Frame p50: `-2.79 dB`

## Interpretation

This sample still shows **output collapse / over-attenuation**:
- output loudness fell too much
- zero percentage increased heavily
- mid-frame energy (p50) is strongly suppressed

So this run is not yet in stable, natural-sounding range.

## Next Action (Recommended)

1. Capture one more controlled test with stronger input level (same script, same distance).  
2. Add frame-level debug logging in JNI for `eIn`, `eOut`, `gSoft` to trace where attenuation spikes.  
3. Tune soft-gain envelope only (no extra denoise passes, no post-gate).
