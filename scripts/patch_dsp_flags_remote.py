#!/usr/bin/env python3
"""Make the server honor per-mode DSP enable flags for voice cleaning."""
from pathlib import Path

pipeline = Path("/var/www/simplelms/backend/apps/voice/services/studio_pipeline.py")
dsp = Path("/var/www/simplelms/backend/apps/voice/services/dsp_enhance.py")

ptext = pipeline.read_text(encoding="utf-8")
old_call = "        enhancer.process_file(input_path, output_path)"
new_call = "        enhancer.process_file(input_path, output_path, **dsp_enable)"
if old_call in ptext:
    ptext = ptext.replace(old_call, new_call, 1)
elif new_call not in ptext:
    raise SystemExit("studio_pipeline.py: DSP call anchor not found")
pipeline.write_text(ptext, encoding="utf-8")

dtext = dsp.read_text(encoding="utf-8")
old_sig = "    def process_file(self, input_path: str, output_path: str,\n                    sample_rate: int = 48000) -> str:"
new_sig = "    def process_file(self, input_path: str, output_path: str,\n                    sample_rate: int = 48000,\n                    enable_eq: bool = True,\n                    enable_compression: bool = True,\n                    enable_saturation: bool = True,\n                    enable_limiter: bool = True,\n                    enable_normalization: bool = True) -> str:"
if old_sig in dtext:
    dtext = dtext.replace(old_sig, new_sig, 1)
elif new_sig not in dtext:
    raise SystemExit("dsp_enhance.py: process_file signature anchor not found")

old_process = "        enhanced = self.process(audio, sample_rate=sr)"
new_process = "        enhanced = self.process(\n            audio, sample_rate=sr,\n            enable_eq=enable_eq,\n            enable_compression=enable_compression,\n            enable_saturation=enable_saturation,\n            enable_limiter=enable_limiter,\n            enable_normalization=enable_normalization,\n        )"
if old_process in dtext:
    dtext = dtext.replace(old_process, new_process, 1)
elif new_process not in dtext:
    raise SystemExit("dsp_enhance.py: process_file call anchor not found")
dsp.write_text(dtext, encoding="utf-8")
print("DSP mode flags wired")
