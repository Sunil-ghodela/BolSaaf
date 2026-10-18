#!/usr/bin/env python3
"""Add a small original-signal blend to smooth RNNoise artifacts in standard mode."""
from pathlib import Path

path = Path("/var/www/simplelms/backend/apps/voice/services/studio_pipeline.py")
text = path.read_text(encoding="utf-8")

# This is a follow-up to the already deployed 10% blend. Keep the patch
# idempotent and narrow so it cannot duplicate the standard-mode block.
old_blend = "                    blended = (0.90 * denoised) + (0.10 * original)"
new_blend = "                    blended = (0.65 * denoised) + (0.35 * original)"
if old_blend in text:
    path.write_text(text.replace(old_blend, new_blend, 1), encoding="utf-8")
    print("standard dry blend increased to 35%")
    raise SystemExit(0)

anchor = "            phase2_output = self._phase2_denoise(phase1_output, mode)\n            \n            # Phase 3: DSP Enhancement"
replacement = """            phase2_output = self._phase2_denoise(phase1_output, mode)

            # RNNoise can sound granular on already-clear speech. Preserve a
            # small amount of the preprocessed signal only in standard mode;
            # this smooths speech texture without bypassing denoise entirely.
            if mode == ProcessingMode.STANDARD:
                import numpy as np
                original, original_sr = sf.read(phase1_output)
                denoised, denoised_sr = sf.read(phase2_output)
                if original_sr == denoised_sr and len(original) == len(denoised):
                    if getattr(original, "ndim", 1) > 1:
                        original = np.mean(original, axis=1)
                    if getattr(denoised, "ndim", 1) > 1:
                        denoised = np.mean(denoised, axis=1)
                    clean_rms = float(np.sqrt(np.mean(np.square(denoised))))
                    dry_rms = float(np.sqrt(np.mean(np.square(original))))
                    if clean_rms > 1e-8 and dry_rms > 1e-8:
                        original = original * (clean_rms / dry_rms)
                    # A larger dry share is intentional here: the previous
                    # 10% blend did not lower the audible RNNoise texture on
                    # clear forwarded voice samples. Keep the denoised signal
                    # dominant while restoring enough natural speech floor.
                    blended = (0.65 * denoised) + (0.35 * original)
                    blended_path = self._get_temp_path("phase2_smoothed.wav")
                    sf.write(blended_path, np.clip(blended, -1.0, 1.0), denoised_sr)
                    phase2_output = blended_path

            # Phase 3: DSP Enhancement"""
if anchor not in text:
    raise SystemExit("studio_pipeline.py: standard blend anchor not found")
path.write_text(text.replace(anchor, replacement, 1), encoding="utf-8")
print("standard dry blend patch applied")
