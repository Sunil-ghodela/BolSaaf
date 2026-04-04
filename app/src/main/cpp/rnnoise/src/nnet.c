/* Copyright (c) 2018, Gianluca Guida, Mumble Developers */
/* RNNoise neural network implementation */

#include <math.h>
#include <string.h>
#include "rnn.h"
#include "nnet.h"

#define MAX_NEURONS 128
#define MAX_FEATURES 42

/* Activation functions */
static float sigmoid(float x) {
    return 1.0f / (1.0f + expf(-x));
}

static float tanh_approx(float x) {
    return tanhf(x);
}

static float relu(float x) {
    return x > 0 ? x : 0;
}

/* GRU layer computation */
static void compute_gru_layer(const GRULayer *gru, float *state, const float *input) {
    int nhidden = gru->nhidden;
    int nb_inputs = gru->nb_inputs;
    
    float z[MAX_NEURONS];  /* Update gate */
    float r[MAX_NEURONS];  /* Reset gate */
    float h[MAX_NEURONS];  /* Candidate activation */
    float temp_input[MAX_NEURONS];
    float temp_recurrent[MAX_NEURONS];
    
    /* Compute update gate z and reset gate r */
    for (int i = 0; i < nhidden; i++) {
        /* Update gate */
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
        
        z[i] = sigmoid(sum_z);
        r[i] = sigmoid(sum_r);
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

/* Dense layer computation */
static void compute_dense_layer(const DenseLayer *dense, float *output, const float *input) {
    for (int i = 0; i < dense->nb_outputs; i++) {
        float sum = dense->bias[i];
        for (int j = 0; j < dense->nb_inputs; j++) {
            sum += input[j] * dense->input_weights[i * dense->nb_inputs + j];
        }
        output[i] = sum;
    }
}

/* Compute entire RNNoise model */
void nnet_compute_rnn(NNetModel *model, const float *input, float *gains, float *vad_prob) {
    float state0[MAX_NEURONS] = {0};
    float state1[MAX_NEURONS] = {0};
    float state2[MAX_NEURONS] = {0};
    float dense_out[MAX_NEURONS];
    
    /* Layer 0: Input GRU (42 features -> 128 hidden) */
    compute_gru_layer(&model->gru[0], state0, input);
    
    /* Layer 1: Middle GRU (128 -> 128) */
    memcpy(state1, state0, sizeof(state0));
    compute_gru_layer(&model->gru[1], state1, state0);
    
    /* Layer 2: Output GRU (128 -> 128) */
    memcpy(state2, state1, sizeof(state1));
    compute_gru_layer(&model->gru[2], state2, state1);
    
    /* Dense output layer (128 -> 23: 22 gains + 1 VAD) */
    compute_dense_layer(&model->dense, dense_out, state2);
    
    /* Apply sigmoid to get gains in [0, 1] */
    for (int i = 0; i < NB_BANDS && i < model->dense.nb_outputs - 1; i++) {
        gains[i] = sigmoid(dense_out[i]);
    }
    
    /* VAD probability (last output) */
    if (vad_prob) {
        *vad_prob = sigmoid(dense_out[model->dense.nb_outputs - 1]);
    }
}

/* Initialize model with trained weights */
void nnet_init_model(NNetModel *model) {
    extern void rnnoise_model_init(NNetModel *model);
    rnnoise_model_init(model);
}
