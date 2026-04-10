#!/usr/bin/env python3
"""Apply on server: isolates each StudioPipeline.process() in a unique temp dir (fixes parallel /media race on fixed /tmp/*.wav names)."""
from pathlib import Path

p = Path("/var/www/simplelms/backend/apps/voice/services/studio_pipeline.py")
text = p.read_text()
if "voice_job_" in text and "_job_workspace" in text:
    print("studio_pipeline: already patched")
    raise SystemExit(0)

if "\nimport shutil\n" not in text:
    text = text.replace("import os\n", "import os\nimport shutil\n", 1)

old = """        logger.info(f"Starting Studio Pipeline: {input_path} -> {output_path}")
        logger.info(f"Mode: {mode.value}, Format: {export_format.value if export_format else 'WAV'}")
        
        try:
            # Phase 1: FFmpeg Preprocess"""
new = """        logger.info(f"Starting Studio Pipeline: {input_path} -> {output_path}")
        logger.info(f"Mode: {mode.value}, Format: {export_format.value if export_format else 'WAV'}")
        
        _saved_workspace = self.temp_dir
        _job_workspace = tempfile.mkdtemp(prefix="voice_job_", dir=_saved_workspace)
        self.temp_dir = _job_workspace
        self.temp_files = []
        try:
            # Phase 1: FFmpeg Preprocess"""
if old not in text:
    raise SystemExit("anchor1 not found")

old2 = """            # Cleanup temp files
            self._cleanup()
            
            # Cleanup DeepFilterNet if loaded
            if self.deepfilter is not None:
                self.deepfilter.cleanup()
            
            if return_info:
                return final_output, info
            else:
                return final_output
            
        except Exception as e:
            # Cleanup on error
            self._cleanup()
            if self.deepfilter is not None:
                self.deepfilter.cleanup()
            
            logger.error(f"Pipeline failed: {e}")
            raise"""
new2 = """            # Cleanup DeepFilterNet if loaded
            if self.deepfilter is not None:
                self.deepfilter.cleanup()
            
            if return_info:
                return final_output, info
            else:
                return final_output
            
        except Exception as e:
            if self.deepfilter is not None:
                self.deepfilter.cleanup()
            
            logger.error(f"Pipeline failed: {e}")
            raise
        finally:
            self._cleanup()
            shutil.rmtree(_job_workspace, ignore_errors=True)
            self.temp_dir = _saved_workspace"""
if old2 not in text:
    raise SystemExit("anchor2 not found")

p.write_text(text.replace(old, new, 1).replace(old2, new2, 1))
print("studio_pipeline: patched per-job temp dir")
