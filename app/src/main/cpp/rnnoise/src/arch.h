/* Copyright (c) 2007-2008 CSIRO */
/* Architecture-specific optimizations */

#ifndef ARCH_H
#define ARCH_H 1

#ifdef __ARM_NEON
#define CPU_ARM 1
#else
#define CPU_ARM 0
#endif

#ifdef __x86_64__
#define CPU_X86_64 1
#else
#define CPU_X86_64 0
#endif

#endif
