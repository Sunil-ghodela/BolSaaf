/* Copyright (c) 2018, Gianluca Guida, Mumble Developers */
/* GRU and Dense layer computation for RNNoise */

#include <math.h>
#include <string.h>
#include "rnn.h"
#include "nnet.h"

#define MAX_NEURONS 128

static inline float sigmoid_approx(float x) {
    return 1.0f / (1.0f + expf(-x));
}

static inline float tanh_approx(float x) {
    return tanhf(x);
}

/* Compute GRU layer - state is updated in place */
void compute_gru(const GRULayer *gru, float *state, const float *input) {
    int nhidden = gru->nhidden;
    int nb_inputs = gru->nb_inputs;
    
    float z[MAX_NEURONS];  /* Update gate */
    float r[MAX_NEURONS];  /* Reset gate */
    float h[MAX_NEURONS];  /* Candidate activation */
    
    /* Compute update gate z and reset gate r */
    for (int i = 0; i < nhidden; i++) {
        float sum_z = gru->bias[i];
        float sum_r = gru->bias[nhidden + i];
        
        /* Input contribution */
        for (int j = 0; j < nb_inputs; j++) {
            sum_z += input[j] * gru->input_weights[i * nb_inputs + j];
            sum_r += input[j] * gru->input_weights[(nhidden + i) * nb_inputs + j];
        }
        
        /* Recurrent contribution */
        for (int j = 0; j < nhidden; j++) {
            sum_z += state[j] * gru->recurrent_weights[i * nhidden + j];
            sum_r += state[j] * gru->recurrent_weights[(nhidden + i) * nhidden + j];
        }
        
        z[i] = sigmoid_approx(sum_z);
        r[i] = sigmoid_approx(sum_r);
    }
    
    /* Compute candidate activation h */
    for (int i = 0; i < nhidden; i++) {
        float sum_h = gru->bias[2 * nhidden + i];
        
        /* Input contribution */
        for (int j = 0; j < nb_inputs; j++) {
            sum_h += input[j] * gru->input_weights[(2 * nhidden + i) * nb_inputs + j];
        }
        
        /* Reset recurrent contribution */
        for (int j = 0; j < nhidden; j++) {
            sum_h += (state[j] * r[j]) * gru->recurrent_weights[(2 * nhidden + i) * nhidden + j];
        }
        
        h[i] = tanh_approx(sum_h);
    }
    
    /* Update hidden state: h_new = (1 - z) * h_old + z * h */
    for (int i = 0; i < nhidden; i++) {
        state[i] = (1.0f - z[i]) * state[i] + z[i] * h[i];
    }
}

/* Compute Dense layer */
void compute_dense(const DenseLayer *layer, float *output, const float *input) {
    for (int i = 0; i < layer->nb_outputs; i++) {
        float sum = layer->bias[i];
        for (int j = 0; j < layer->nb_inputs; j++) {
            sum += input[j] * layer->input_weights[i * layer->nb_inputs + j];
        }
        output[i] = sum;
    }
}
