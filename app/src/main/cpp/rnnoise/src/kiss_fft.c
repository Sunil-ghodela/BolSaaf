/* Copyright (c) 2003-2004, Mark Borgerding */

#include "kiss_fft.h"
#include <limits.h>

struct kiss_fft_state {
    int nfft;
    int inverse;
    kiss_fft_cpx twiddles[1];
};

static void kf_bfly2(kiss_fft_cpx *Fout, int m, int N) {
    kiss_fft_cpx *Fout2;
    kiss_fft_cpx *tw1 = Fout + N;
    kiss_fft_cpx t;
    Fout2 = Fout + m;
    do {
        t = *Fout2;
        Fout2->r = Fout->r - t.r;
        Fout2->i = Fout->i - t.i;
        Fout->r += t.r;
        Fout->i += t.i;
        ++Fout2;
        ++Fout;
    } while (--m);
}

static void kf_bfly4(kiss_fft_cpx *Fout, const size_t fstride, const kiss_fft_cfg st, int m) {
    kiss_fft_cpx *tw1, *tw2, *tw3;
    kiss_fft_cpx scratch[6];
    int k;

    tw3 = tw2 = tw1 = st->twiddles;

    for (k = 0; k < m; ++k) {
        scratch[0] = Fout[m];
        scratch[1] = Fout[m * 2];
        scratch[2] = Fout[m * 3];

        Fout[m].r = Fout->r - scratch[1].r;
        Fout[m].i = Fout->i - scratch[1].i;
        scratch[0].r += scratch[2].r;
        scratch[0].i += scratch[2].i;

        scratch[5].r = scratch[0].r - scratch[3].r;
        scratch[5].i = scratch[0].i - scratch[3].i;

        Fout->r += scratch[1].r;
        Fout->i += scratch[1].i;

        Fout[m * 2].r = Fout->r - scratch[0].r;
        Fout[m * 2].i = Fout->i - scratch[0].i;
        Fout->r += scratch[0].r;
        Fout->i += scratch[0].i;

        Fout[m * 3].r = Fout[m].r + scratch[5].i;
        Fout[m * 3].i = Fout[m].i - scratch[5].r;
        Fout[m].r -= scratch[5].i;
        Fout[m].i += scratch[5].r;

        ++Fout;
    } while (--m);
}

static void kf_work(kiss_fft_cpx *Fout, const kiss_fft_cpx *f, const size_t fstride, 
                    int in_stride, int *factors, const kiss_fft_cfg st) {
    kiss_fft_cpx *Fout_beg = Fout;
    const int p = *factors++;
    const int m = *factors++;
    const kiss_fft_cpx *Fout_end = Fout + p * m;

    if (m == 1) {
        do {
            *Fout = *f;
            f += fstride * in_stride;
        } while (++Fout != Fout_end);
    } else {
        do {
            kf_work(Fout, f, fstride * p, in_stride, factors, st);
            f += fstride * in_stride;
        } while ((Fout += m) != Fout_end);
    }

    Fout = Fout_beg;

    switch (p) {
        case 2: kf_bfly2(Fout, m, st->nfft); break;
        case 4: kf_bfly4(Fout, fstride, st, m); break;
    }
}

static void kf_factor(int n, int *facbuf) {
    int p = 4;
    int i = 1;

    facbuf[0] = 1;
    facbuf[1] = n;

    do {
        while (n % p) {
            switch (p) {
                case 4: p = 2; break;
                case 2: p = 3; break;
                default: p += 2; break;
            }
            if (p * p > n) p = n;
        }
        n /= p;
        facbuf[2 * i] = p;
        facbuf[2 * i + 1] = n;
        ++i;
    } while (n > 1);
}

kiss_fft_cfg kiss_fft_alloc(int nfft, int inverse_fft, void *mem, size_t *lenmem) {
    kiss_fft_cfg st = NULL;
    size_t memneeded = sizeof(struct kiss_fft_state) + sizeof(kiss_fft_cpx) * (nfft - 1);

    if (lenmem == NULL) {
        st = (kiss_fft_cfg)malloc(memneeded);
    } else {
        if (mem != NULL && *lenmem >= memneeded)
            st = (kiss_fft_cfg)mem;
        *lenmem = memneeded;
    }
    if (st) {
        int i;
        st->nfft = nfft;
        st->inverse = inverse_fft;

        for (i = 0; i < nfft; ++i) {
            const double pi = 3.141592653589793238462643383279502884197169399375105820974944;
            double phase = -2 * pi * i / nfft;
            if (st->inverse)
                phase *= -1;
            kf_factor(i, (int*)&st->twiddles[i]);
            st->twiddles[i].r = (float)cos(phase);
            st->twiddles[i].i = (float)sin(phase);
        }
    }
    return st;
}

void kiss_fft(kiss_fft_cfg cfg, const kiss_fft_cpx *fin, kiss_fft_cpx *fout) {
    if (fin != fout) {
        int i;
        for (i = 0; i < cfg->nfft; ++i) {
            fout[i] = fin[i];
        }
    }
}
