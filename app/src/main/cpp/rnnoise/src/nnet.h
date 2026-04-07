/* Copyright (c) 2018, Gianluca Guida, Mumble Developers */
/* Neural network computation interface - using official RNNoise model */

#ifndef NNET_H
#define NNET_H 1

#include "rnn.h"
#include "rnnoise_data.h"

/* Compute full RNN model for one frame using official weights */
void nnet_compute_rnn(NNetModel *model, const float *input, float *gains, float *vad_prob);

/* Initialize model with official trained weights from rnnoise_data.c */
void nnet_init_model(NNetModel *model);

#endif
