/* Copyright (c) 2018, Gianluca Guida, Mumble Developers */

#ifndef RNN_H
#define RNN_H 1

#include "rnnoise.h"

#define WEIGHTS_SCALE 0.00392157f
#define MAX_NEURONS 128
#define MAX_FEATURES 42
#define NB_BANDS 22

/* GRU Layer structure */
typedef struct {
    int nhidden;
    int nb_inputs;
    const float *bias;
    const float *input_weights;
    const float *recurrent_weights;
} GRULayer;

/* Dense Layer structure */
typedef struct {
    int nb_inputs;
    int nb_outputs;
    const float *bias;
    const float *input_weights;
} DenseLayer;

/* Complete model structure */
typedef struct {
    GRULayer gru[3];
    DenseLayer dense;
} NNetModel;

/* External model init function from rnn_data.c */
void rnnoise_model_init(NNetModel *model);

static const int eband5ms[] = {
    0, 1, 2, 3, 4, 5, 6, 7, 8, 10, 12, 14, 16, 20, 24, 28, 34, 40, 48, 60, 78, 100
};

#endif
