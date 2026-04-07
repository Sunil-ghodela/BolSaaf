# Audio Sample Check: `20260407_140621`

Date: 2026-04-07  
Flow: **Upload -> MP3 to WAV conversion -> Cloud clean API -> cleaned WAV download**

Files checked:
- Input upload: `uploaded_20260407_140621.mp3`
- Cloud source WAV: `uploaded_20260407_140621_cloud_source.wav`
- Output: `cleaned_20260407_140621.wav`

## What happened (timing concern)

- This run used **cloud path** (not local fallback).
- It took longer because pipeline includes:
  1. MP3 decode + resample to WAV on device
  2. API upload
  3. server processing
  4. cleaned WAV download
- No 5MB chunk batching happened in this sample:
  - cloud source WAV size: `1,397,804 bytes` (< 5MB)
  - so only **single API call**.

## Format + size

- `uploaded_...mp3`: `153,336 bytes`
- `uploaded_..._cloud_source.wav`: `1,397,804 bytes`
- `cleaned_...wav`: `1,397,804 bytes`
- WAV duration: `14.56 s`, mono 48kHz, 16-bit PCM

## Metrics (Cloud source WAV vs cleaned WAV)

### Source WAV (`uploaded_20260407_140621_cloud_source.wav`)
- RMS: `-30.60 dBFS`
- Peak: `-3.01 dBFS`
- Zero samples: `3.41%`
- Near-zero (`|x| <= 8`): `17.98%`
- p10 / p50 / p90: `-75.23 / -36.92 / -26.06 dBFS`

### Cleaned (`cleaned_20260407_140621.wav`)
- RMS: `-33.76 dBFS`
- Peak: `-10.71 dBFS`
- Zero samples: `7.85%`
- Near-zero (`|x| <= 8`): `21.07%`
- p10 / p50 / p90: `-81.30 / -40.63 / -28.77 dBFS`

### Delta (`cleaned - source`)
- RMS: `-3.16 dB`
- Peak: `-7.70 dB`
- Zero samples: `+4.44%`
- Near-zero: `+3.09%`
- p10: `-6.07 dB`
- p50: `-3.71 dB`
- p90: `-2.70 dB`

## Interpretation

- Cloud cleaner is currently **more suppressive/attenuating** on this sample (noticeable level drop).
- The longer processing time is expected for cloud flow even without chunking.
