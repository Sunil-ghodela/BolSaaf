# Benchmark log — 2026-04-09 (first 2 takes)

Assumption from device timestamps:
- `111454` = **normal + clean**
- `111533` = **normal + bg mix**

| # | File / ID | Scenario | Adaptive flags | Suggested preset | Conf. | RMS in dBFS (orig->clean) | Peak dBFS (orig->clean) | Guard |
|---|-----------|----------|----------------|------------------|-------|----------------------------|--------------------------|-------|
| 1 | `20260409_111454` | normal + clean | `very_quiet_boost, quiet_recording` | Studio / pro | 0.76 | `-70.03 -> -64.36` | `-35.24 -> -27.82` | FAIL (`output_very_quiet`) |
| 2 | `20260409_111533` | normal + bg mix | `very_quiet_boost, quiet_recording` | Studio / pro | 0.76 | `-76.13 -> -72.86` | `-50.94 -> -39.86` | FAIL (`output_very_quiet`) |

## Listening notes

- Both captures are near-silence level at source, so processing improves level but remains too quiet for user playback.
- Added app-side safety: if quality guard marks `output_very_quiet`, apply limited post-gain floor (target around `-42 dBFS`, max +18 dB).

