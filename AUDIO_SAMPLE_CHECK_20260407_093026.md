# Audio Sample Check: `20260407_093026`

Date: 2026-04-07  
Build state: **Phase 2** — `HPF → pre-gain → RNNoise → post-gain → presence EQ → high-shelf → de-esser → compressor`  
Files checked (pulled from device `Music/BolSaaf/`):

- Input: `original_20260407_093026.wav`
- Output: `cleaned_20260407_093026.wav`

## Input / Output Format Validation

- Both files are valid WAV.
- Sample rate: `48000 Hz`
- Channels: `1 (mono)`
- Bit depth: `16-bit PCM`
- Duration: `11.70 s` (input/output matched; `561600` samples)

## Measured Metrics

Metrics use linear full-scale amplitude: `dBFS = 20·log10(|x|/32768)`; RMS is overall RMS of all samples.  
Frame percentiles: 480-sample frames, energy percentiles over frames (same method as earlier sample reports).

### Input (`original_20260407_093026.wav`)

- RMS: `-62.08 dBFS`
- Peak: `-28.75 dBFS`
- Zero samples: `7.30%`
- Near-zero samples (`|x| ≤ 8`): `67.53%`
- Frame p10 / p50 / p90: `-84.74 / -73.34 / -62.56 dBFS`

### Output (`cleaned_20260407_093026.wav`)

- RMS: `-56.13 dBFS`
- Peak: `-20.75 dBFS`
- Zero samples: `36.79%`
- Near-zero samples (`|x| ≤ 8`): `74.25%`
- Frame p10 / p50 / p90: `-120.00 / -78.24 / -59.49 dBFS`

### Delta (`cleaned − original`)

- RMS: **`+5.95 dB`**
- Peak: **`+8.01 dB`**
- Zero samples: **`+29.49%`**
- Near-zero samples: **`+6.73%`**
- p10: `-35.26 dB` (many quiet frames pushed to digital silence in cleaned → p10 floors at −120 dBFS)
- p50: `-4.90 dB`
- p90: **`+3.07 dB`**

## Comparison vs Pre–Phase 2 (`20260407_091858`, same chain without EQ/de-esser/compressor)

| Metric | `091858` delta | `093026` delta |
|--------|----------------|----------------|
| RMS | +3.47 dB | +5.95 dB |
| Peak | +3.21 dB | +8.01 dB |
| Zero % | +29.36% | +29.49% |
| Near-zero % | +5.25% | +6.73% |
| p50 | −5.44 dB | −4.90 dB |
| p90 | +2.37 dB | +3.07 dB |

**Notes**

- Phase 2 adds **clear loudness lift** versus the earlier single-pass-only build: higher RMS and especially higher peak on this take.
- **Zero / near-zero** behaviour is in the same ballpark as `091858` (still a sizable zero increase vs input — typical of RNNoise + quiet background).
- **p50** is slightly less negative than `091858` (−4.9 vs −5.4 dB), i.e. a bit less mid-body drop on this file.
- Input for this clip was **quieter in peak** than `091858` (−28.8 vs −20.4 dBFS), so cross-take comparisons are indicative only; ear test still matters.

## Interpretation

- **No sign of the old “energy collapse”** pattern (large negative RMS deltas on the order of many dB down). Here RMS and p90 move **up**, consistent with makeup gain + compression after denoise.
- **Trade-off:** more processing (EQ + de-esser + compressor) can change timbre; subjectively check for **harshness**, **pumping**, or **thin** speech.

## Recommended Next Step

1. Listen to `cleaned_20260407_093026.wav` on headphones vs `original_*` — confirm “studio” polish vs artifacts.  
2. If peak or density feels too aggressive, next knob is usually **compressor threshold/ratio** or **slightly lower** presence EQ (native `rnnoise_jni.cpp`).
