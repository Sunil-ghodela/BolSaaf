/* Copyright (c) 2018, Gianluca Guida, Mumble Developers */
/* Neural network computation interface */

#ifndef NNET_H
#define NNET_H 1

#include "rnn.h"

/* Compute full RNN model for one frame */
void nnet_compute_rnn(NNetModel *model, const float *input, float *gains, float *vad_prob);

/* Initialize model with trained weights */
void nnet_init_model(NNetModel *model);

#endif
