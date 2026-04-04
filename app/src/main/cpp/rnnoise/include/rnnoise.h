/* Copyright (c) 2018, Gianluca Guida */
/* All rights reserved. */

#ifndef RNNOISE_H
#define RNNOISE_H 1

#ifdef __cplusplus
extern "C" {
#endif

#include <stdlib.h>

typedef struct DenoiseState DenoiseState;

#define FRAME_SIZE_SHIFT 2
#define FRAME_SIZE (120<<FRAME_SIZE_SHIFT)
#define FFT_SIZE (2*120)

DenoiseState *rnnoise_create(void);
void rnnoise_destroy(DenoiseState *st);
float rnnoise_process_frame(DenoiseState *st, float *out, const float *in);

#ifdef __cplusplus
}
#endif

#endif
