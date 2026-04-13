# Feedback Result Tracker

- Generated: `2026-04-13T03:56:14.322579+00:00`
- Total entries: `13`
- Clear voice yes/no: `3/7`

## Split by Result
- `artifacts`: 6
- `good`: 5
- `quiet`: 2

## Split by Mode
- `clean`: 5
- `extract_voice`: 4
- `add_background`: 2
- `reel`: 2

## Split by Issue Type
- `none`: 6
- `Noise bacha`: 3
- `Quiet`: 2
- `Artifacts`: 2

## Recent Entries (Latest 10)

- id=13 ts=20260413_091923 mode=add_background result=quiet clear=False issue=Quiet notes=background not added
- id=12 ts=20260413_092021 mode=add_background result=artifacts clear=False issue=Artifacts notes=smooth bnao bhai
- id=11 ts=20260411_083225 mode=clean result=quiet clear=False issue=Quiet notes=loudness good h , noice remove still bacha h. piche se sui, suu voice chl rhi h
- id=10 ts=20260411_082652 mode=clean result=artifacts clear=False issue=Noise bacha notes=background noice still. but loud shi a smooth. ho. gya
- id=9 ts=20260411_082617 mode=clean result=artifacts clear=False issue=Noise bacha notes=background noice still. but loud shi a smooth. ho. gya
- id=8 ts=20260411_082545 mode=clean result=artifacts clear=False issue=Noise bacha notes=background noice still. but loud shi a smooth. ho. gya
- id=7 ts=20260410_105412 mode=clean result=good clear=True issue= notes=ye shi h
- id=6 ts=20260409_160355 mode=extract_voice result=artifacts clear=False issue=Artifacts notes=
- id=5 ts=20260409_160520 mode=extract_voice result=good clear=True issue= notes=not smoothness.bqckground not added
- id=4 ts=20260409_160613 mode=extract_voice result=good clear=True issue= notes=smoothness nhi ho aa rhi

## Tuning Plan (Next)
- Focus first on `extract_voice` artifacts and smoothness.
- Add one mild post-smoothing profile for extract path and compare 5 takes.
- Gate stronger denoise when signal is already clear to reduce robotic texture.
- Track improvement target: artifacts ratio under 15% for extract mode.
