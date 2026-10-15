#!/bin/bash
set -e
cd /tmp/bggen

# --- 1. Bhakti: temple bells + tanpura-like drone (60s loop) ---
ffmpeg -y -loglevel error -f lavfi -i "aevalsrc=0.06*sin(2*PI*196*t)+0.05*sin(2*PI*147*t)+0.04*sin(2*PI*98*t):s=44100:d=60:c=stereo" drone.wav
ffmpeg -y -loglevel error -f lavfi -i "aevalsrc=0.4*sin(2*PI*392*t)*exp(-1.8*t)+0.28*sin(2*PI*1056*t)*exp(-2.5*t)+0.2*sin(2*PI*2112*t)*exp(-3.2*t)+0.12*sin(2*PI*784*t)*exp(-1.2*t):s=44100:d=4:c=mono" bell.wav
ffmpeg -y -loglevel error -i bell.wav -filter_complex "[0]adelay=0[a];[0]adelay=7500[b];[0]adelay=15000[c];[0]adelay=22500[d];[0]adelay=30000[e];[0]adelay=37500[f];[0]adelay=45000[g];[0]adelay=52500[h];[a][b][c][d][e][f][g][h]amix=inputs=8:normalize=0[bells]" -map "[bells]" -t 60 bells_track.wav
ffmpeg -y -loglevel error -i drone.wav -i bells_track.wav -filter_complex "[1]volume=0.45[bt];[0][bt]amix=inputs=2:normalize=0" -t 60 bhakti_raw.wav
ffmpeg -y -loglevel error -i bhakti_raw.wav -af "afade=t=in:d=1,afade=t=out:st=59:d=1" -b:a 128k bhakti.mp3

# --- 2. Cricket: stadium crowd (60s loop) ---
ffmpeg -y -loglevel error -f lavfi -i "anoisesrc=color=pink:amplitude=0.7:seed=42:d=60" -af "pan=stereo|c0=c0|c1=c0,lowpass=f=3500,highpass=f=250" crowd_base.wav
ffmpeg -y -loglevel error -f lavfi -i "anoisesrc=color=pink:amplitude=0.5:seed=77:d=60" -af "pan=stereo|c0=c0|c1=c0,bandpass=f=1200:w=800,volume='0.9+0.6*sin(2*PI*t/13)':eval=frame" swell.wav
ffmpeg -y -loglevel error -f lavfi -i "anoisesrc=color=brown:amplitude=0.3:seed=11:d=60" -af "pan=stereo|c0=c0|c1=c0,lowpass=f=150" rumble.wav
ffmpeg -y -loglevel error -i crowd_base.wav -i swell.wav -i rumble.wav -filter_complex "[0]volume=0.5[b];[1]volume=0.35[s];[2]volume=0.8[r];[s][r][b]amix=inputs=3:normalize=0" -t 60 cricket_raw.wav
ffmpeg -y -loglevel error -i cricket_raw.wav -af "afade=t=in:d=1,afade=t=out:st=59:d=1" -b:a 128k cricket.mp3

# --- 3. Confession: intimate soft ambience (60s loop) ---
ffmpeg -y -loglevel error -f lavfi -i "anoisesrc=color=brown:amplitude=0.25:seed=5:d=60" -af "pan=stereo|c0=c0|c1=c0,lowpass=f=700" warm.wav
ffmpeg -y -loglevel error -f lavfi -i "aevalsrc=0.05*sin(2*PI*110*t)+0.035*sin(2*PI*164.8*t):s=44100:d=60:c=stereo" drone2.wav
ffmpeg -y -loglevel error -i warm.wav -i drone2.wav -filter_complex "[1]volume=0.5[d];[0][d]amix=inputs=2:normalize=0" -t 60 confession_raw.wav
ffmpeg -y -loglevel error -i confession_raw.wav -af "afade=t=in:d=1.5,afade=t=out:st=58.5:d=1.5" -b:a 128k confession.mp3

# --- 4. Study: lo-fi focus chords + gentle bass + crackle (60s loop) ---
ffmpeg -y -loglevel error -f lavfi -i "aevalsrc=if(lt(mod(t\,12)\,6)\, 0.07*(sin(2*PI*220*t)+sin(2*PI*261.63*t)+sin(2*PI*329.63*t)+sin(2*PI*392*t))*exp(-0.35*mod(t\,6)) + 0.06*sin(2*PI*55*t)\, 0.07*(sin(2*PI*174.61*t)+sin(2*PI*220*t)+sin(2*PI*261.63*t)+sin(2*PI*349.23*t))*exp(-0.35*mod(t\,6)) + 0.06*sin(2*PI*43.65*t)):s=44100:d=60:c=stereo" chords.wav
ffmpeg -y -loglevel error -f lavfi -i "anoisesrc=color=pink:amplitude=0.06:seed=99:d=60" -af "pan=stereo|c0=c0|c1=c0,highpass=f=4000" crackle.wav
ffmpeg -y -loglevel error -i chords.wav -i crackle.wav -filter_complex "[0]lowpass=f=2800[m];[m][1]amix=inputs=2:normalize=0" -t 60 study_raw.wav
ffmpeg -y -loglevel error -i study_raw.wav -af "afade=t=in:d=1,afade=t=out:st=59:d=1" -b:a 128k study.mp3

# --- Previews (20s) ---
for id in bhakti cricket confession study; do
  ffmpeg -y -loglevel error -i $id.mp3 -t 20 -b:a 96k ${id}_20s.mp3
done

echo "=== durations + sizes ==="
for id in bhakti cricket confession study; do
  d=$(ffprobe -v error -show_entries format=duration -of csv=p=0 $id.mp3)
  echo "$id.mp3 dur=${d}s size=$(stat -c%s $id.mp3)"
done
