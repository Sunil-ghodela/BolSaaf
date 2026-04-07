# Audio Sample Check: `20260406_150219`

Date: 2026-04-06  
Build state: `HPF -> RNNoise (single pass) -> pre-gain -> soft post-gain`  
Files checked:
- Input: `original_20260406_150219.wav`
- Output: `cleaned_20260406_150219.wav`

## Input / Output Format Validation

- Both files are valid WAV.
- Sample rate: `48000 Hz`
- Channels: `1 (mono)`
- Bit depth: `16-bit PCM`
- Duration: `11.38 s` (input/output matched)

## Measured Metrics

### Input (`original_20260406_150219.wav`)
- RMS: `-63.52 dBFS`
- Peak: `-37.99 dBFS`
- Zero samples: `8.54%`
- Near-zero samples (`|x| <= 8`): `58.71%`

### Output (`cleaned_20260406_150219.wav`)
- RMS: `-61.44 dBFS`
- Peak: `-39.30 dBFS`
- Zero samples: `33.33%`
- Near-zero samples (`|x| <= 8`): `65.47%`

### Delta (`cleaned - original`)
- RMS: `+2.09 dB` (louder)
- Peak: `-1.31 dB`
- Zero samples: `+24.78%`
- Near-zero samples: `+6.77%`
- Frame p10: `-34.93 dB`
- Frame p50: `-3.80 dB`
- Frame p90: `+1.44 dB`

## Reference Comparison

Reference (earlier known-better): `20260406_140036` delta:
- RMS: `-0.49 dB`
- Peak: `+0.87 dB`
- Zero: `+22.50%`
- Near-zero: `+18.62%`
- p50: `-2.79 dB`

## Interpretation

- Collapse pattern improved vs recent bad runs (no huge `-5 to -8 dB` RMS collapse).
- Loudness now recovered (`+2 dB` RMS), which confirms pre-gain is helping low-input recordings.
- Zero ratio is still elevated (`+24.78%`) but better balanced than prior collapse cases.
- This run looks **usable/stable**, with room for tonal fine-tuning only.

## Recommended Next Step

1. Do one more repeat recording in same environment for consistency check.  
2. If listening test is good, lock this as current baseline and stop major DSP changes.
