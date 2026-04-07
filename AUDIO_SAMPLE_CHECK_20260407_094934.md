# Audio Sample Check: `20260407_094934`

Date: 2026-04-07  
Build state: **Phase 2 + dry mix** — `HPF → pre-gain → RNNoise → 12% dry blend → post-gain → presence + high-shelf → de-esser → compressor (thresh 0.16, ratio 2.5) → optional near-silence floor lift`  

Files (pulled from device `Music/BolSaaf/`):

- Input: `original_20260407_094934.wav`
- Output: `cleaned_20260407_094934.wav`

## Input / Output Format Validation

- Both files are valid WAV.
- Sample rate: `48000 Hz`
- Channels: `1 (mono)`
- Bit depth: `16-bit PCM`
- Duration: `10.88 s` (`522240` samples)

## Measured Metrics

Same definitions as earlier reports: RMS/peak vs full scale; frame p10/p50/p90 from 480-sample frame energies.

### Input (`original_20260407_094934.wav`)

- RMS: `-40.63 dBFS`
- Peak: **`0.00 dBFS`** (hits full scale — very hot recording)
- Zero samples: `3.98%`
- Near-zero samples (`|x| ≤ 8`): `22.64%`
- Frame p10 / p50 / p90: `-68.18 / -57.27 / -40.19 dBFS`

### Output (`cleaned_20260407_094934.wav`)

- RMS: `-38.16 dBFS`
- Peak: **`-5.47 dBFS`** (peak reduced vs clipped input — headroom restored)
- Zero samples: `4.00%`
- Near-zero samples (`|x| ≤ 8`): **`16.67%`**
- Frame p10 / p50 / p90: `-66.19 / -51.24 / -35.04 dBFS`

### Delta (`cleaned − original`)

- RMS: **`+2.47 dB`**
- Peak: **`-5.47 dB`** (input was at 0 dBFS; output peak lower — limiting / dynamics / clipping behaviour)
- Zero samples: `+0.02%` (essentially unchanged)
- Near-zero samples: **`-5.97%`** (fewer near-silence bins than input — good sign for “body” vs over-thin output)
- p10: `+1.99 dB`
- p50: **`+6.03 dB`**
- p90: **`+5.15 dB`**

## Comparison vs Previous Take (`20260407_093026`, older Phase 2 without dry mix)

| Metric | `093026` delta | `094934` delta |
|--------|----------------|----------------|
| RMS | +5.95 dB | +2.47 dB |
| Peak | +8.01 dB | −5.47 dB (hot input case) |
| Zero % | +29.49% | ~0% |
| Near-zero % | +6.73% | **−5.97%** |
| p50 | −4.90 dB | **+6.03 dB** |
| p90 | +3.07 dB | +5.15 dB |

**Context:** `093026` had a much quieter original peak (−28.8 dBFS) and showed a large zero-sample increase after processing. This new take is **louder in the room**, so metrics are not directly comparable take-for-take; the important pattern here is **near-zero share dropping** vs input and **strong positive p50/p90** movement on this clip.

## Interpretation

- **Dry mix + softer compressor** on this sample: processed output keeps **more energy in the mid-level body** (p50/p90 up) and **does not inflate near-zero counts** the way the very quiet-input runs did.
- Original at **0 dBFS peak** is borderline clipped; cleaned peak at **−5.5 dBFS** suggests the chain is **backing off peaks** while still lifting perceived level in RMS/mid percentiles — **listen for** any harshness or distortion that was already on the mic bus.

## Recommended Next Step

1. **Ear test** this file vs original on headphones — confirm naturalness after dry mix.  
2. If new recordings are often **that hot**, consider **input attenuation / limiter before RNNoise** in a future pass (separate task).  
3. Optional: record again at **slightly lower input** (avoid 0 dBFS peaks) for a cleaner A/B of noise reduction only.
