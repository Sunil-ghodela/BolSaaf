#include <jni.h>
#include <android/log.h>
#include "rnnoise.h"

#define LOG_TAG "RNNoiseJNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

extern "C"
JNIEXPORT jlong JNICALL
Java_com_bolsaaf_audio_RNNoise_create(JNIEnv *env, jobject thiz) {
    DenoiseState *st = rnnoise_create(NULL);
    if (st) {
        LOGI("RNNoise state created: %p", st);
    } else {
        LOGE("Failed to create RNNoise state");
    }
    return (jlong) st;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_bolsaaf_audio_RNNoise_destroy(JNIEnv *env, jobject thiz, jlong ptr) {
    if (ptr != 0) {
        rnnoise_destroy((DenoiseState *) ptr);
        LOGI("RNNoise state destroyed: %p", (DenoiseState *) ptr);
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_com_bolsaaf_audio_RNNoise_process(JNIEnv *env, jobject thiz, jlong ptr, jshortArray input) {
    if (ptr == 0) {
        LOGE("Invalid RNNoise state pointer");
        return;
    }

    jshort *buffer = env->GetShortArrayElements(input, NULL);
    if (!buffer) {
        LOGE("Failed to get array elements");
        return;
    }

    // RNNoise works with 480 samples (FRAME_SIZE)
    float in[480];
    float out[480];

    // Convert short to float [-1, 1]
    for (int i = 0; i < 480; i++) {
        in[i] = buffer[i] / 32768.0f;
    }

    // Process frame
    rnnoise_process_frame((DenoiseState *) ptr, out, in);

    // Convert float back to short
    for (int i = 0; i < 480; i++) {
        float val = out[i] * 32768.0f;
        if (val > 32767.0f) val = 32767.0f;
        if (val < -32768.0f) val = -32768.0f;
        buffer[i] = (jshort) val;
    }

    env->ReleaseShortArrayElements(input, buffer, 0);
}
