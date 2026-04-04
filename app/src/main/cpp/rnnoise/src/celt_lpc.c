/* Copyright (c) 2007-2008 CSIRO */
/* LPC (Linear Predictive Coding) utilities */

#include <math.h>
#include <string.h>

void celt_lpc(float *lpc, const float *ac, int p) {
    float error = ac[0];
    float lpc_mem[256];
    
    memset(lpc_mem, 0, sizeof(lpc_mem));
    
    for (int i = 0; i < p; i++) {
        float r = -ac[i + 1];
        for (int j = 0; j < i; j++) {
            r -= lpc_mem[j] * ac[i - j];
        }
        r /= error;
        lpc_mem[i] = r;
        for (int j = 0; j < i / 2; j++) {
            float tmp = lpc_mem[j];
            lpc_mem[j] += r * lpc_mem[i - 1 - j];
            lpc_mem[i - 1 - j] += r * tmp;
        }
        if (i % 2) {
            lpc_mem[i / 2] += lpc_mem[i / 2] * r;
        }
        error *= 1.0f - r * r;
    }
    
    memcpy(lpc, lpc_mem, p * sizeof(float));
}
