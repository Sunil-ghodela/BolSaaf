# Audio Tuning Test Log (BolSaaf)

Date: 2026-04-06  
Platform: Android (Redmi Note 7 Pro, release builds)  
Pipeline: 48kHz mono WAV, RNNoise native chain

## Goal

Make cleaned voice sound clearly better ("clean + natural"), especially with fan/background noise, without robotic artifacts.

## Baseline Changes Completed

1. Replaced unstable RNNoise internals with upstream-compatible native path.
2. Added robust decode/resample path (WAV parse + MediaCodec fallback).
3. Added studio chain in JNI:
   - pre high-pass (~95 Hz)
   - multi-pass RNNoise
   - RMS compensation
   - presence EQ
4. Fixed app build/install pipeline:
   - Gradle wrapper restored
   - signed release APK flow configured
5. UI updates:
   - recording circle stabilized
   - recent list moved under record button

## Experiments Run and Outcomes

### 1) Dual/Triple RNNoise + stronger smoothing
- Change: more aggressive denoise (alpha tuning in `denoise.c`, extra RNNoise passes).
- Result: fan/hiss reduction improved, but speech often became over-attenuated in some takes.
- Risk seen: musical/choppy artifacts, occasional hollow voice.

### 2) Post adaptive gate (several variants)
- Change: frame-level gate added after denoise with multiple threshold/smoothing tries.
- Result: noise floor dropped, but artifacts increased (speech tails cut, robotic texture).
- Decision: post-gate became main artifact source.

### 3) Gate bypass
- Change: removed post-gate and kept RNNoise+EQ+RMS only.
- Result: significant artifact reduction and better speech naturalness.
- Note: some runs were good/stable, but later sessions showed occasional severe level collapse.

### 4) Collapse guard / safety clamps (final stabilization attempt)
- Change: stronger gain clamps + min-energy guard + dry fallback blend.
- Result: did not stabilize reliably; some samples still collapsed heavily.
- Decision: reverted this attempt.

### 5) EQ softening (2.8 dB -> 1.9 dB)
- Change: reduced presence boost for natural tone.
- Result: slightly less harsh but reduced clarity/body in several samples.
- Status: not a clear net win.

## Quantitative Pattern Observed

Good runs:
- RMS drop around ~0.3 to 1.2 dB
- zero-sample increase moderate
- better perceived naturalness

Bad runs (collapse):
- RMS drop ~4 to 8+ dB
- high zero/sparse frame ratio
- hollow/robotic perceived output

## Current Recommended Baseline (Rollback State)

Current code was rolled back to the known better audio state:
- no aggressive final collapse guard additions
- no adaptive post-gate
- previous gain clamp behavior restored
- UI fixes retained

This is the safest state currently in repo for further controlled tuning.

## What to Test Next (Controlled Plan)

1. Record 3 fixed scenarios (same speaking style each run):
   - quiet room
   - fan noise
   - moderate street/ambient noise
2. For each scenario, compare:
   - original vs cleaned loudness
   - speech clarity
   - artifact/choppy feel
3. Only one small DSP change per iteration (no multi-variable jumps).

## Practical Conclusion

The app can suppress fan noise well, but stability across sessions is the main issue.  
Primary blocker is not basic denoise capability; it is preventing occasional output collapse while preserving natural speech.

## Locked Baseline (Current)

Status: **LOCKED for now** (based on latest listening acceptance + stability check)

Active chain:
- `HPF -> RNNoise (single pass) -> pre-gain -> light post-gain`
- No hard post-gate
- No multi-pass stacking

Reference accepted sample:
- `original_20260406_150219.wav`
- `cleaned_20260406_150219.wav`
- Detailed report: `AUDIO_SAMPLE_CHECK_20260406_150219.md`

## UI Updates Included

- Recording button animation stabilized to avoid layout break during `isRecording` toggle.
- Recent list already moved under main recording button.
