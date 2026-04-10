from pathlib import Path
p = Path('/var/www/simplelms/backend/apps/voice/serializers.py')
t = p.read_text()
old = """    def validate_mode_used(self, value):
        v = (value or '').strip().lower()
        allowed = {'clean','extract_voice','add_background','reel','video_process'}
        return v if v in allowed else 'unknown'
"""
new = """    def validate_mode_used(self, value):
        v = (value or '').strip().lower()
        if v == 'reel_mode':
            return 'reel'
        allowed = {'clean', 'extract_voice', 'add_background', 'reel', 'video_process'}
        return v if v in allowed else 'unknown'
"""
if old in t:
    t = t.replace(old, new, 1)
    p.write_text(t)
    print('updated mode normalization for reel_mode')
else:
    print('pattern not found, skip')
