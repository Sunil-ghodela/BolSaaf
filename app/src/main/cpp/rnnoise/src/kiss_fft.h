/* Copyright (c) 2003-2004, Mark Borgerding */
/* Kiss FFT implementation */

#ifndef KISS_FFT_H
#define KISS_FFT_H 1

#ifdef __cplusplus
extern "C" {
#endif

#include <stdlib.h>
#include <math.h>

typedef struct {
    float r;
    float i;
} kiss_fft_cpx;

typedef struct kiss_fft_state* kiss_fft_cfg;

kiss_fft_cfg kiss_fft_alloc(int nfft, int inverse_fft, void *mem, size_t *lenmem);
void kiss_fft(kiss_fft_cfg cfg, const kiss_fft_cpx *fin, kiss_fft_cpx *fout);

#define opus_fft_alloc(nfft, x1, x2, arch) kiss_fft_alloc(nfft, 0, x1, x2)
#define opus_fft_free(cfg, arch) free(cfg)
#define opus_fft(cfg, fin, fout, arch) kiss_fft(cfg, fin, fout)

#ifdef __cplusplus
}
#endif

#endif
