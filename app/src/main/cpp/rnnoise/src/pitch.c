/* Copyright (c) 2018, Gianluca Guida */
/* Pitch detection for RNNoise */

#include <math.h>
#include <string.h>
#include "pitch.h"

#define PITCH_MIN_PERIOD 32
#define PITCH_MAX_PERIOD 256
#define PITCH_FRAME_SIZE 320

void pitch_init(PitchState *st) {
    memset(st, 0, sizeof(*st));
}

static float inner_prod(const float *x, const float *y, int N) {
    float sum = 0;
    for (int i = 0; i < N; i++) {
        sum += x[i] * y[i];
    }
    return sum;
}

float pitch_search(PitchState *st, const float *x, int min_period, int max_period) {
    int best_period = min_period;
    float best_corr = -1e15f;
    
    // Simple normalized cross-correlation
    for (int period = min_period; period <= max_period; period++) {
        float corr = inner_prod(x, x + period, PITCH_FRAME_SIZE - period);
        float norm = sqrtf(inner_prod(x, x, PITCH_FRAME_SIZE - period) * 
                           inner_prod(x + period, x + period, PITCH_FRAME_SIZE - period) + 1e-15f);
        
        float score = corr / norm;
        if (score > best_corr) {
            best_corr = score;
            best_period = period;
        }
    }
    
    return (float)best_period;
}

float pitch_gain(const PitchState *st, float pitch, int min_period, int max_period) {
    // Simple pitch gain computation
    return 1.0f;
}
