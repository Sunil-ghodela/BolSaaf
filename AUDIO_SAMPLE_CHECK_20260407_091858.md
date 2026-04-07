# Audio Sample Check: `20260407_091858`

Date: 2026-04-07  
Build state: `HPF -> RNNoise (single pass) -> pre-gain -> soft post-gain`  
Files checked:
- Input: `original_20260407_091858.wav`
- Output: `cleaned_20260407_091858.wav`

## Input / Output Format Validation

- Both files are valid WAV.
- Sample rate: `48000 Hz`
- Channels: `1 (mono)`
- Bit depth: `16-bit PCM`
- Duration: `8.10 s` (input/output matched)

## Measured Metrics

### Input (`original_20260407_091858.wav`)
- RMS: `-61.57 dBFS`
- Peak: `-20.43 dBFS`
- Zero samples: `10.18%`
- Near-zero samples (`|x| <= 8`): `65.13%`
- Frame p10 / p50 / p90: `-86.06 / -72.61 / -61.74 dBFS`

### Output (`cleaned_20260407_091858.wav`)
- RMS: `-58.09 dBFS`
- Peak: `-17.23 dBFS`
- Zero samples: `39.54%`
- Near-zero samples (`|x| <= 8`): `70.38%`
- Frame p10 / p50 / p90: `-120.00 / -78.05 / -59.37 dBFS`

### Delta (`cleaned - original`)
- RMS: `+3.47 dB`
- Peak: `+3.21 dB`
- Zero samples: `+29.36%`
- Near-zero samples: `+5.25%`
- p10: `-33.94 dB`
- p50: `-5.44 dB`
- p90: `+2.37 dB`

## Comparison vs Previous Reference (`20260406_150219`)

Reference delta:
- RMS: `+2.09 dB`
- Peak: `-1.31 dB`
- Zero: `+24.78%`
- Near-zero: `+6.77%`
- p50: `-3.80 dB`

Observation against reference:
- New sample is louder (+RMS, +peak), which is good for audibility.
- Zero ratio is higher (`+29.36%`), indicating stronger suppression/possible speech thinning in quieter regions.
- Mid-frame p50 drop is larger (`-5.44 dB`), so voice body may still feel somewhat hollow.

## Interpretation

This run is **usable** and no catastrophic collapse is observed.  
However, suppression is still aggressive in low/mid-energy regions, so “fully studio-natural” output is not yet reached.

## Recommended Next Step

1. Keep this chain for now (stable enough for testing).  
2. If voice feels thin, slightly reduce pre-gain (`3.5 -> 3.0`) and retest once.
