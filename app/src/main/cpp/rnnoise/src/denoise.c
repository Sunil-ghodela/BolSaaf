/* Copyright (c) 2018, Gianluca Guida */
/* All rights reserved. */

#ifdef HAVE_CONFIG_H
#include "config.h"
#endif

#include <stdlib.h>
#include <string.h>
#include <math.h>
#include "rnnoise.h"
#include "arch.h"
#include "rnn.h"
#include "nnet.h"
#include "pitch.h"
#include "kiss_fft.h"
#include "common.h"
#include "cpu_support.h"

#define FRAME_SIZE_SHIFT 2
#define FRAME_SIZE (120<<FRAME_SIZE_SHIFT)
#define WINDOW_SIZE (2*FRAME_SIZE)
#define FREQ_SIZE (FRAME_SIZE + 1)

#define SQUARE(x) ((x)*(x))
#define VAD_THRESHOLD 0.6f

static const float window[WINDOW_SIZE] = {
    0.000000f, 0.000000f, 0.000000f, 0.000000f, 0.000000f, 0.000000f, 0.000000f, 0.000000f,
    0.000061f, 0.000183f, 0.000305f, 0.000519f, 0.000733f, 0.001007f, 0.001282f, 0.001617f,
    0.001953f, 0.002350f, 0.002747f, 0.003174f, 0.003601f, 0.004059f, 0.004517f, 0.005005f,
    0.005493f, 0.005981f, 0.006500f, 0.007019f, 0.007538f, 0.008057f, 0.008575f, 0.009094f,
    0.009613f, 0.010101f, 0.010620f, 0.011108f, 0.011597f, 0.012054f, 0.012512f, 0.012939f,
    0.013367f, 0.013763f, 0.014160f, 0.014526f, 0.014893f, 0.015228f, 0.015564f, 0.015869f,
    0.016174f, 0.016449f, 0.016724f, 0.016968f, 0.017212f, 0.017426f, 0.017609f, 0.017792f,
    0.017944f, 0.018066f, 0.018188f, 0.018280f, 0.018341f, 0.018402f, 0.018433f, 0.018433f,
    0.018433f, 0.018402f, 0.018341f, 0.018280f, 0.018188f, 0.018066f, 0.017944f, 0.017792f,
    0.017609f, 0.017426f, 0.017212f, 0.016968f, 0.016724f, 0.016449f, 0.016174f, 0.015869f,
    0.015564f, 0.015228f, 0.014893f, 0.014526f, 0.014160f, 0.013763f, 0.013367f, 0.012939f,
    0.012512f, 0.012054f, 0.011597f, 0.011108f, 0.010620f, 0.010101f, 0.009613f, 0.009094f,
    0.008575f, 0.008057f, 0.007538f, 0.007019f, 0.006500f, 0.005981f, 0.005493f, 0.005005f,
    0.004517f, 0.004059f, 0.003601f, 0.003174f, 0.002747f, 0.002350f, 0.001953f, 0.001617f,
    0.001282f, 0.001007f, 0.000733f, 0.000519f, 0.000305f, 0.000183f, 0.000061f, 0.000000f,
    // ... additional 368 values to complete 480-sample window
};

struct DenoiseState {
    float analysis_mem[FRAME_SIZE];
    float synthesis_mem[FRAME_SIZE];
    float pitch_buf[PITCH_BUF_SIZE];
    float pitch_enh_buf[PITCH_BUF_SIZE];
    float last_gain;
    float last_period;
    float mem_hp_x[2];
    float ring[FRAME_SIZE];
    kiss_fft_state *kfft;
    PitchState pitch;
    NNetModel model;
    float nlp_gain;
    int enabled;
    float window[WINDOW_SIZE];
};

static void compute_window(float *window) {
    for (int i = 0; i < WINDOW_SIZE; i++) {
        window[i] = sinf(0.5f * M_PI * sinf(0.5f * M_PI * (i + 0.5f) / WINDOW_SIZE) * 
                        sinf(0.5f * M_PI * (i + 0.5f) / WINDOW_SIZE));
    }
}

DenoiseState *rnnoise_create(void) {
    DenoiseState *st = (DenoiseState *)calloc(1, sizeof(DenoiseState));
    if (!st) return NULL;

    st->kfft = opus_fft_alloc(2 * FRAME_SIZE, NULL, NULL, NULL);
    if (!st->kfft) {
        free(st);
        return NULL;
    }

    st->enabled = 1;
    st->last_gain = 1.0f;
    compute_window(st->window);

    pitch_init(&st->pitch);
    
    /* Initialize neural network model */
    rnnoise_model_init(&st->model);

    return st;
}

void rnnoise_destroy(DenoiseState *st) {
    if (st) {
        if (st->kfft) opus_fft_free(st->kfft, 0);
        free(st);
    }
}

static void biquad(float *y, float mem[2], const float *x, float b[3], float a[3], int N) {
    for (int i = 0; i < N; i++) {
        float xi = x[i];
        float yi = b[0] * xi + mem[0];
        mem[0] = b[1] * xi - a[1] * yi + mem[1];
        mem[1] = b[2] * xi - a[2] * yi;
        y[i] = yi;
    }
}

static void highpass(float *out, float *mem, const float *in, int len) {
    float a[3] = {1, -1.99599f, 0.99600f};
    float b[3] = {0.99800f, -1.99600f, 0.99800f};
    biquad(out, mem, in, b, a, len);
}

static void frame_analysis(DenoiseState *st, float *X, float *Ex, const float *in) {
    float x[WINDOW_SIZE];
    float tmp[WINDOW_SIZE];

    // Apply window and overlap
    for (int i = 0; i < FRAME_SIZE; i++) {
        x[i] = st->window[i] * st->analysis_mem[i];
        x[FRAME_SIZE + i] = st->window[FRAME_SIZE + i] * in[i];
        st->analysis_mem[i] = in[i];
    }

    // FFT
    opus_fft(st->kfft, x, tmp, 0);

    // Convert to power spectrum
    for (int i = 0; i < FREQ_SIZE; i++) {
        float real = tmp[2 * i];
        float imag = tmp[2 * i + 1];
        X[i] = real * real + imag * imag;
    }

    // Compute band energies (simplified - using critical bands approximation)
    for (int i = 0; i < NB_BANDS; i++) {
        float sum = 0;
        int band_start = eband5ms[i] * 2;
        int band_end = eband5ms[i + 1] * 2;
        for (int j = band_start; j < band_end && j < FREQ_SIZE; j++) {
            sum += X[j];
        }
        Ex[i] = sum;
    }
}

static void frame_synthesis(DenoiseState *st, float *out, const float *y) {
    float x[WINDOW_SIZE];

    // Inverse FFT and window
    for (int i = 0; i < WINDOW_SIZE; i++) {
        x[i] = y[i] * st->window[i];
    }

    // Overlap-add
    for (int i = 0; i < FRAME_SIZE; i++) {
        out[i] = x[i] + st->synthesis_mem[i];
        st->synthesis_mem[i] = x[FRAME_SIZE + i];
    }
}

float rnnoise_process_frame(DenoiseState *st, float *out, const float *in) {
    float x[FRAME_SIZE];
    float X[FREQ_SIZE];
    float Ex[NB_BANDS];
    float g[NB_BANDS];
    float gf[FREQ_SIZE];
    float y[WINDOW_SIZE];
    float vad_prob = 0.0f;

    if (!st->enabled) {
        memcpy(out, in, FRAME_SIZE * sizeof(float));
        return 1.0f;
    }

    // High-pass filter to remove DC
    highpass(x, st->mem_hp_x, in, FRAME_SIZE);

    // Analysis
    frame_analysis(st, X, Ex, x);

    // Pitch analysis
    float pitch_buf[PITCH_BUF_SIZE];
    for (int i = 0; i < FRAME_SIZE; i++) {
        pitch_buf[i] = x[i];
    }
    float pitch = pitch_search(&st->pitch, pitch_buf, PITCH_MIN_PERIOD, PITCH_MAX_PERIOD);
    float gain = pitch_gain(&st->pitch, pitch, PITCH_MIN_PERIOD, PITCH_MAX_PERIOD);

    // Simple noise suppression (place holder for full RNN)
    // In full RNNoise, this uses the neural network
    for (int i = 0; i < NB_BANDS; i++) {
        // Simple suppression based on signal level
        float speech_prob = 1.0f / (1.0f + expf(-(Ex[i] - 1.0f)));
        g[i] = 0.5f + 0.5f * speech_prob;
    }

    // Interpolate gains
    for (int i = 0; i < FREQ_SIZE; i++) {
        int band = 0;
        for (int j = 0; j < NB_BANDS - 1; j++) {
            if (i >= eband5ms[j] * 2) band = j;
        }
        float t = (i - eband5ms[band] * 2.0f) / 
                  (eband5ms[band + 1] - eband5ms[band]) / 2.0f;
        gf[i] = (1 - t) * g[band] + t * g[band + 1];
        gf[i] = fmaxf(0.0f, fminf(1.0f, gf[i]));
    }

    // Apply gains in frequency domain
    float complex_x[WINDOW_SIZE];
    opus_fft(st->kfft, x, complex_x, 0);

    for (int i = 0; i < FREQ_SIZE; i++) {
        complex_x[2 * i] *= gf[i];
        complex_x[2 * i + 1] *= gf[i];
    }

    // Inverse FFT
    opus_fft(st->kfft, complex_x, y, 1);

    // Synthesis
    frame_synthesis(st, out, y);

    // Compute VAD probability
    float total_energy = 0;
    for (int i = 0; i < NB_BANDS; i++) {
        total_energy += Ex[i];
    }
    vad_prob = total_energy > 10.0f ? 0.9f : 0.1f;

    st->last_period = pitch;
    st->last_gain = gain;

    return vad_prob;
}
