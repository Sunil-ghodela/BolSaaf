# Feedback Result Tracker

- Generated: `2026-04-10T03:13:55.570571+00:00`
- Total entries: `6`
- Clear voice yes/no: `2/1`

## Split by Result
- `good`: 4
- `artifacts`: 2

## Split by Mode
- `extract_voice`: 4
- `reel`: 2

## Split by Issue Type
- `none`: 5
- `Artifacts`: 1

## Recent Entries (Latest 10)

- id=6 ts=20260409_160355 mode=extract_voice result=artifacts clear=False issue=Artifacts notes=
- id=5 ts=20260409_160520 mode=extract_voice result=good clear=True issue= notes=not smoothness.bqckground not added
- id=4 ts=20260409_160613 mode=extract_voice result=good clear=True issue= notes=smoothness nhi ho aa rhi
- id=3 ts=20260409_124500 mode=reel result=good clear=None issue= notes=reel_mode final check
- id=2 ts=20260409_123700 mode=extract_voice result=artifacts clear=None issue= notes=post+get check
- id=1 ts=20260409_123000 mode=reel result=good clear=None issue= notes=smoke-test-ext

## Tuning Plan (Next)
- Focus first on `extract_voice` artifacts and smoothness.
- Add one mild post-smoothing profile for extract path and compare 5 takes.
- Gate stronger denoise when signal is already clear to reduce robotic texture.
- Track improvement target: artifacts ratio under 15% for extract mode.
