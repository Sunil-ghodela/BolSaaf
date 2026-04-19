#include <jni.h>
#include <android/log.h>
#include <cmath>
#include <cstring>
#include "rnnoise.h"

#define LOG_TAG "RNNoiseJNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

#ifndef M_PI
#define M_PI 3.14159265358979323846
#endif

/*
 * Core denoise + studio chain (processStudio) is treated as LOCKED — tune in Kotlin via
 * CleaningPreset + AudioInputStage (gain/limiter before RNNoise), not ad-hoc coefficient edits here.
 */

extern "C"
JNIEXPORT jlong JNICALL
Java_com_reelvoice_audio_RNNoise_create(JNIEnv *env, jobject thiz) {
    DenoiseState *st = rnnoise_create(nullptr);
    if (st) {
        LOGI("RNNoise state created: %p", st);
    } else {
        LOGE("Failed to create RNNoise state");
    }
    return (jlong) st;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_reelvoice_audio_RNNoise_destroy(JNIEnv *env, jobject thiz, jlong ptr) {
    if (ptr != 0) {
        rnnoise_destroy((DenoiseState *) ptr);
        LOGI("RNNoise state destroyed: %p", (DenoiseState *) ptr);
    }
}

/* Pre-highpass ~95 Hz — cuts HVAC rumble / mud before RNNoise */
static float hp_x1, hp_x2, hp_y1, hp_y2;
static float hp_b0, hp_b1, hp_b2, hp_a1, hp_a2;
static bool hp_ready = false;

static void ensure_hp_coefs() {
    if (hp_ready) return;
    const float fs = 48000.f;
    const float fc = 95.f;
    const float Q = 0.70710678f;
    float w0 = 2.0f * (float)M_PI * fc / fs;
    float c = cosf(w0);
    float s = sinf(w0);
    float alpha = s / (2.0f * Q);
    float b0 = (1.0f + c) * 0.5f;
    float b1 = -(1.0f + c);
    float b2 = (1.0f + c) * 0.5f;
    float a0 = 1.0f + alpha;
    hp_b0 = b0 / a0;
    hp_b1 = b1 / a0;
    hp_b2 = b2 / a0;
    hp_a1 = (-2.0f * c) / a0;
    hp_a2 = (1.0f - alpha) / a0;
    hp_ready = true;
}

/* Peaking EQ (presence) */
static float pk_x1, pk_x2, pk_y1, pk_y2;
static float pk_b0, pk_b1, pk_b2, pk_a1, pk_a2;
static bool pk_ready = false;
/* High-shelf EQ (air / clarity above speech band) */
static float hs_x1, hs_x2, hs_y1, hs_y2;
static float hs_b0, hs_b1, hs_b2, hs_a1, hs_a2;
static bool hs_ready = false;
/* De-esser: bandpass ~6.5 kHz for envelope follower (detector path only) */
static float ds_x1, ds_x2, ds_y1, ds_y2;
static float ds_b0, ds_b1, ds_b2, ds_a1, ds_a2;
static bool ds_ready = false;
static float ds_env = 0.f;

/* Smoothed post-gain after RNNoise (Phase 1 staging) */
static float ng_gain_smoothed = 1.0f;
/* Soft compressor envelope (Phase 2) */
static float comp_env = 0.f;

static void ensure_peaking_coefs() {
    if (pk_ready) return;
    const float fs = 48000.f;
    const float f0 = 3100.f;
    const float Q = 0.78f;
    const float dB = 1.9f;
    float A = powf(10.0f, dB / 40.0f);
    float w0 = 2.0f * (float)M_PI * f0 / fs;
    float c = cosf(w0);
    float s = sinf(w0);
    float alpha = s / (2.0f * Q);
    float a0 = 1.0f + alpha / A;
    pk_b0 = (1.0f + alpha * A) / a0;
    pk_b1 = (-2.0f * c) / a0;
    pk_b2 = (1.0f - alpha * A) / a0;
    pk_a1 = (-2.0f * c) / a0;
    pk_a2 = (1.0f - alpha / A) / a0;
    pk_ready = true;
}

static void ensure_highshelf_coefs() {
    if (hs_ready) return;
    const float fs = 48000.f;
    const float f0 = 7200.f;
    const float S = 0.85f;
    const float dB = 1.15f;
    float A = powf(10.0f, dB / 40.0f);
    float w0 = 2.0f * (float)M_PI * f0 / fs;
    float c = cosf(w0);
    float s = sinf(w0);
    float alpha = s * sqrtf((A + 1.f / A) * (1.f / S - 1.f) + 2.f);
    /* RBJ high-shelf (see Audio EQ Cookbook) */
    float b0 = A * ((A + 1.f) - (A - 1.f) * c + alpha);
    float b1 = 2.f * A * ((A - 1.f) - (A + 1.f) * c);
    float b2 = A * ((A + 1.f) - (A - 1.f) * c - alpha);
    float a0 = (A + 1.f) + (A - 1.f) * c + alpha;
    float a1 = -2.f * ((A - 1.f) + (A + 1.f) * c);
    float a2 = (A + 1.f) + (A - 1.f) * c - alpha;
    hs_b0 = b0 / a0;
    hs_b1 = b1 / a0;
    hs_b2 = b2 / a0;
    hs_a1 = a1 / a0;
    hs_a2 = a2 / a0;
    hs_ready = true;
}

/* Constant 0 dB peak bandpass (RBJ) — sibilance detection */
static void ensure_deesser_detector_coefs() {
    if (ds_ready) return;
    const float fs = 48000.f;
    const float f0 = 6500.f;
    const float Q = 1.0f;
    float w0 = 2.0f * (float)M_PI * f0 / fs;
    float c = cosf(w0);
    float s = sinf(w0);
    float alpha = s / (2.0f * Q);
    float b0 = alpha;
    float b1 = 0.f;
    float b2 = -alpha;
    float a0 = 1.f + alpha;
    float a1 = -2.f * c;
    float a2 = 1.f - alpha;
    ds_b0 = b0 / a0;
    ds_b1 = b1 / a0;
    ds_b2 = b2 / a0;
    ds_a1 = a1 / a0;
    ds_a2 = a2 / a0;
    ds_ready = true;
}

static inline float biquad_df1(float x, float b0, float b1, float b2, float a1, float a2,
                             float *x1, float *x2, float *y1, float *y2) {
    float y = b0 * x + b1 * *x1 + b2 * *x2 - a1 * *y1 - a2 * *y2;
    *x2 = *x1;
    *x1 = x;
    *y2 = *y1;
    *y1 = y;
    return y;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_reelvoice_audio_RNNoise_resetPostState(JNIEnv *env, jobject thiz) {
    hp_x1 = hp_x2 = hp_y1 = hp_y2 = 0.f;
    pk_x1 = pk_x2 = pk_y1 = pk_y2 = 0.f;
    hs_x1 = hs_x2 = hs_y1 = hs_y2 = 0.f;
    ds_x1 = ds_x2 = ds_y1 = ds_y2 = 0.f;
    ng_gain_smoothed = 1.0f;
    ds_env = 0.f;
    comp_env = 0.f;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_reelvoice_audio_RNNoise_process(JNIEnv *env, jobject thiz, jlong ptr, jshortArray input) {
    if (ptr == 0) {
        LOGE("Invalid RNNoise state pointer");
        return;
    }

    jshort *buffer = env->GetShortArrayElements(input, NULL);
    if (!buffer) {
        LOGE("Failed to get array elements");
        return;
    }

    float in[480];
    float out[480];

    for (int i = 0; i < 480; i++) {
        in[i] = buffer[i] / 32768.0f;
    }

    rnnoise_process_frame((DenoiseState *) ptr, out, in);

    float eIn = 1e-20f, eOut = 1e-20f;
    for (int i = 0; i < 480; i++) {
        eIn += in[i] * in[i];
        eOut += out[i] * out[i];
    }
    float g = sqrtf(eIn / eOut);
    if (eIn < 1e-14f) g = 1.0f;
    if (g > 3.2f) g = 3.2f;
    if (g < 0.55f) g = 0.55f;

    for (int i = 0; i < 480; i++) {
        float val = out[i] * g * 32768.0f;
        if (val > 32767.0f) val = 32767.0f;
        if (val < -32768.0f) val = -32768.0f;
        buffer[i] = (jshort) val;
    }

    env->ReleaseShortArrayElements(input, buffer, 0);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_reelvoice_audio_RNNoise_processStudio(JNIEnv *env, jobject thiz, jlong ptr, jshortArray input) {
    if (ptr == 0) {
        LOGE("Invalid RNNoise state pointer");
        return;
    }

    jshort *buffer = env->GetShortArrayElements(input, NULL);
    if (!buffer) {
        LOGE("Failed to get array elements");
        return;
    }

    ensure_hp_coefs();
    ensure_peaking_coefs();
    ensure_highshelf_coefs();
    ensure_deesser_detector_coefs();

    float in[480];
    float out[480];

    const float preGain = 3.5f;
    for (int i = 0; i < 480; i++) {
        float x = buffer[i] / 32768.0f;
        float hp = biquad_df1(x, hp_b0, hp_b1, hp_b2, hp_a1, hp_a2, &hp_x1, &hp_x2, &hp_y1, &hp_y2);
        float boosted = hp * preGain;
        if (boosted > 1.0f) boosted = 1.0f;
        if (boosted < -1.0f) boosted = -1.0f;
        in[i] = boosted;
    }

    rnnoise_process_frame((DenoiseState *) ptr, out, in);

    const float postGain = 1.2f;
    ng_gain_smoothed = 0.96f * ng_gain_smoothed + 0.04f * postGain;

    /* Phase 2: presence + air -> gentle de-esser -> soft compressor (no hard gate) */
    const float ds_thresh = 0.016f;
    const float ds_maxAtt = 0.35f; /* max ~3 dB */
    const float ds_att = 1.f - expf(-1.f / (0.0022f * 48000.f));
    const float ds_rel = 1.f - expf(-1.f / (0.045f * 48000.f));

    /* Less aggressive: higher threshold (~−16 dBFS env), ratio 2.5:1 — less pumping */
    const float comp_thresh = 0.16f;
    const float comp_ratio = 2.5f;
    const float comp_att = 1.f - expf(-1.f / (0.0045f * 48000.f));
    const float comp_rel = 1.f - expf(-1.f / (0.110f * 48000.f));

    const float dryMix = 0.12f; /* 10–15% dry: body + naturalness after Phase 2 */

    for (int i = 0; i < 480; i++) {
        float clean = out[i];
        float dry = in[i];
        float s = clean + dry * dryMix;

        /* Very light noise-floor lift (optional ambience in near-silence) */
        if (fabsf(s) < 0.002f) {
            s += in[i] * 0.03f;
        }

        s *= ng_gain_smoothed;

        /* Tonal polish */
        s = biquad_df1(s, pk_b0, pk_b1, pk_b2, pk_a1, pk_a2, &pk_x1, &pk_x2, &pk_y1, &pk_y2);
        s = biquad_df1(s, hs_b0, hs_b1, hs_b2, hs_a1, hs_a2, &hs_x1, &hs_x2, &hs_y1, &hs_y2);

        float sib = biquad_df1(s, ds_b0, ds_b1, ds_b2, ds_a1, ds_a2, &ds_x1, &ds_x2, &ds_y1, &ds_y2);
        float aS = fabsf(sib);
        if (aS > ds_env) {
            ds_env += ds_att * (aS - ds_env);
        } else {
            ds_env += ds_rel * (aS - ds_env);
        }
        float gDs = 1.f;
        if (ds_env > ds_thresh) {
            float over = (ds_env - ds_thresh) / (ds_thresh + 1e-6f);
            gDs = 1.f / (1.f + ds_maxAtt * over * over);
        }
        s *= gDs;

        float aFull = fabsf(s);
        if (aFull > comp_env) {
            comp_env += comp_att * (aFull - comp_env);
        } else {
            comp_env += comp_rel * (aFull - comp_env);
        }
        float gComp = 1.f;
        if (comp_env > comp_thresh) {
            float over = comp_env / comp_thresh;
            gComp = powf(over, -(1.f - 1.f / comp_ratio));
        }
        s *= gComp;

        if (s > 1.0f) s = 1.0f;
        if (s < -1.0f) s = -1.0f;
        float val = s * 32768.0f;
        if (val > 32767.0f) val = 32767.0f;
        if (val < -32768.0f) val = -32768.0f;
        buffer[i] = (jshort) val;
    }

    env->ReleaseShortArrayElements(input, buffer, 0);
}
