cmake_minimum_required(VERSION 3.22.1)
project(rnnoise-lib)

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -O3 -fPIC -std=c++17")
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -O3 -fPIC")

# Upstream Xiph RNNoise (see https://github.com/xiph/rnnoise ) + JNI shim
add_library(
    rnnoise-lib
    SHARED
    rnnoise_jni.cpp
    rnnoise/src/denoise.c
    rnnoise/src/rnn.c
    rnnoise/src/rnn_data.c
    rnnoise/src/rnn_reader.c
    rnnoise/src/pitch.c
    rnnoise/src/kiss_fft.c
    rnnoise/src/celt_lpc.c
)

include_directories(
    ${CMAKE_CURRENT_SOURCE_DIR}/rnnoise/include
    ${CMAKE_CURRENT_SOURCE_DIR}/rnnoise/src
)

find_library(
    log-lib
    log
)

target_link_libraries(
    rnnoise-lib
    ${log-lib}
)
