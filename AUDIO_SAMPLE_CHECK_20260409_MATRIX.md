# Audio sample matrix check — 2026-04-09

**Source:** Device folder `Android/data/com.bolsaaf/files/Music/BolSaaf`, filter `cleaned_20260409_*.wav`.  
**Mapping:** Files sorted by name (timestamp order) assigned to matrix slots **a→c** × **clean / extract / bg_mix** as in `scripts/audio_sample_matrix_check.py`.

**Note:** Only **8** outputs were present on device; slot **c · studio · bg_mix** was not in the folder at check time.

**Automation:** `python3 scripts/audio_sample_matrix_check.py --date 20260409`

## Summary (smoke: rate/channels match, duration drift ≤ 0.2s, signal changed)

| # | Row | Preset | Flow | File | Result |
|---|-----|--------|------|------|--------|
| 1 | a | normal | clean | `cleaned_20260409_093611.wav` | PASS |
| 2 | a | normal | extract | `cleaned_20260409_093637.wav` | PASS |
| 3 | a | normal | bg_mix | `cleaned_20260409_093702.wav` | PASS |
| 4 | b | strong | clean | `cleaned_20260409_093725.wav` | PASS |
| 5 | b | strong | extract | `cleaned_20260409_093807.wav` | PASS |
| 6 | b | strong | bg_mix | `cleaned_20260409_093826.wav` | PASS |
| 7 | c | studio | clean | `cleaned_20260409_093846.wav` | PASS |
| 8 | c | studio | extract | `cleaned_20260409_093903.wav` | PASS |
| 9 | c | studio | bg_mix | — | *missing on device* |

## Key metrics (original → output)

| Slot | Dur (s) | RMS dBFS | Peak dBFS | RMS diff (linear) |
|------|---------|----------|-----------|-------------------|
| a clean | 17.22 | −36.63 → −34.65 | −6.02 → −5.43 | 638 |
| a extract | 15.90 | −41.71 → −38.99 | −13.79 → −13.52 | 398 |
| a bg_mix | 11.16 | −43.95 → −38.76 | −22.85 → −14.90 | 378 |
| b clean | 14.64 | −40.12 → −39.04 | −10.87 → −11.42 | 392 |
| b extract | 12.27 | −47.14 → −43.04 | −23.35 → −17.40 | 238 |
| b bg_mix | 13.10 | −40.80 → −36.72 | −8.42 → −7.21 | 475 |
| c clean | 10.76 | −44.42 → −38.90 | −15.08 → −7.35 | 403 |
| c extract | 12.90 | −39.35 → −40.45 | −10.76 → −8.25 | 442 |

## Listen / product note

- **c · studio · extract:** overall RMS slightly lower than input while peak rises — worth an ear test for naturalness.
- **“Noiseless” goal:** pipeline quality is a **Phase 2–5** track (adaptive params, artifact guards, loudness standard, optional VAD). This matrix only validates **technical smoke** (format, duration, signal change), not perceptual noise floor.

## Machine-readable data

- **Saved JSON:** `data/audio_sample_matrix_20260409.json` (full metrics per row; regenerate with  
  `python3 scripts/audio_sample_matrix_check.py --date 20260409 > data/audio_sample_matrix_20260409.json`)
- **Pulled WAVs (local):** `tmp_audio/matrix_checks/` after script run
- **Script:** `scripts/audio_sample_matrix_check.py`
