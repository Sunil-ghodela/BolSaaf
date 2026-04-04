/* Copyright (c) 2018, Gianluca Guida */
/* CPU feature detection */

#ifndef CPU_SUPPORT_H
#define CPU_SUPPORT_H 1

static inline int opus_cpu_supports_neon(void) {
#ifdef __ARM_NEON
    return 1;
#else
    return 0;
#endif
}

static inline int opus_cpu_supports_sse(void) {
#ifdef __SSE__
    return 1;
#else
    return 0;
#endif
}

#endif
