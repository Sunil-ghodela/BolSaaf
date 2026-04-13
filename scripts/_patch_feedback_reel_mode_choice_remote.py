from pathlib import Path

mp = Path('/var/www/simplelms/backend/apps/voice/models.py')
mt = mp.read_text()
if "('reel_mode', 'Reel Mode')" not in mt:
    mt = mt.replace("        ('reel', 'Reel'),\n", "        ('reel', 'Reel'),\n        ('reel_mode', 'Reel Mode'),\n", 1)
    mp.write_text(mt)
    print('models.py updated for reel_mode choice')
else:
    print('models.py already has reel_mode')

sp = Path('/var/www/simplelms/backend/apps/voice/serializers.py')
st = sp.read_text()
st = st.replace("allowed = {'clean', 'extract_voice', 'add_background', 'reel', 'video_process'}", "allowed = {'clean', 'extract_voice', 'add_background', 'reel', 'reel_mode', 'video_process'}")
sp.write_text(st)
print('serializers.py allowed modes updated')
