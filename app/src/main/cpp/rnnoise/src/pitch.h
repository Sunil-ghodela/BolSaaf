/* Copyright (c) 2018, Gianluca Guida */
/* Pitch detection header */

#ifndef PITCH_H
#define PITCH_H 1

#define PITCH_BUF_SIZE (482)
#define PITCH_MIN_PERIOD 32
#define PITCH_MAX_PERIOD 256

typedef struct {
    float x[PITCH_BUF_SIZE];
    int pitch_index;
    float last_pitch;
} PitchState;

void pitch_init(PitchState *st);
float pitch_search(PitchState *st, const float *x, int min_period, int max_period);
float pitch_gain(const PitchState *st, float pitch, int min_period, int max_period);

#endif
