# Audio Sample Check: `20260407_134932`

Date: 2026-04-07  
Build state: **Cloud-integrated flow** (upload clean: cloud-first for WAV <= 5MB, local fallback otherwise) + locked local DSP chain.

Files checked (pulled from device `Music/BolSaaf/`):

- Input: `original_20260407_134932.wav`
- Output: `cleaned_20260407_134932.wav`

## Input / Output Format Validation

- Both files are valid WAV.
- Sample rate: `48000 Hz`
- Channels: `1 (mono)`
- Bit depth: `16-bit PCM`
- Duration: `13.70 s` (`657600` samples)

## Measured Metrics

Same metric method as earlier sample checks: full-scale dBFS for RMS/peak; frame p10/p50/p90 from 480-sample frame energies.

### Input (`original_20260407_134932.wav`)

- RMS: `-68.91 dBFS`
- Peak: `-36.83 dBFS`
- Zero samples: `11.11%`
- Near-zero samples (`|x| <= 8`): `66.51%`
- Frame p10 / p50 / p90: `-83.91 / -71.74 / -66.75 dBFS`

### Output (`cleaned_20260407_134932.wav`)

- RMS: `-62.41 dBFS`
- Peak: `-25.70 dBFS`
- Zero samples: `16.84%`
- Near-zero samples (`|x| <= 8`): `55.56%`
- Frame p10 / p50 / p90: `-94.11 / -68.70 / -62.99 dBFS`

### Delta (`cleaned - original`)

- RMS: **`+6.49 dB`**
- Peak: **`+11.14 dB`**
- Zero samples: `+5.73%`
- Near-zero samples: **`-10.95%`**
- p10: `-10.20 dB`
- p50: **`+3.04 dB`**
- p90: **`+3.76 dB`**

## Quick Comparison vs Prior Latest (`20260407_094934`)

Observation:
- This new sample starts **much quieter** (input peak `-36.83 dBFS` vs older very hot takes), so a positive RMS/peak lift after cleaning is expected.
- Near-zero share drops strongly (`-10.95%`), which usually indicates less hollow/over-gated feel.
- Zero % increases a bit, but not collapse-like.

## Interpretation

- Output looks **healthy and usable**: energy/body improved (p50/p90 up), with no catastrophic collapse signs.
- For this low-level input case, cleaning provides strong audibility lift while still reducing sparse silence bins.

## Recommended Next Step

1. Ear-test this pair on headphones for naturalness and any processing texture.  
2. If this came from cloud path, verify latency feels acceptable in UX (toast currently shows cloud/local source).  
3. Record one more fan/traffic sample and repeat same metric check for consistency.
