package com.reelvoice.video

import android.graphics.Bitmap
import android.graphics.Canvas
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLExt
import android.opengl.EGLSurface
import android.opengl.GLES20
import android.opengl.GLUtils
import android.opengl.Matrix
import android.view.Surface
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.min

/**
 * End-to-end on-device MP4 encoder for the waveform-video export feature.
 *
 * Renders each frame with [WaveformFrameRenderer] onto an offscreen [Bitmap], uploads it
 * to an OpenGL texture, draws a fullscreen quad into the H.264 encoder's input surface
 * (EGL + MediaCodec COLOR_FormatSurface path), and muxes alongside a freshly AAC-encoded
 * copy of the source PCM. Output is a single MP4 file ready to share.
 *
 * Why render-to-surface (not YUV-buffer input): the EGL path lets the device's H.264
 * encoder do RGB→YUV in hardware — faster and lower CPU than a Kotlin color-conversion.
 */
class WaveformVideoEncoder(
    private val sampler: WaveformWindowSampler,
    private val renderer: WaveformFrameRenderer,
    private val width: Int = DEFAULT_WIDTH,
    private val height: Int = DEFAULT_HEIGHT,
    private val fps: Int = DEFAULT_FPS,
    private val videoBitrate: Int = DEFAULT_VIDEO_BITRATE,
    private val audioBitrate: Int = DEFAULT_AUDIO_BITRATE,
    private val barCount: Int = DEFAULT_BAR_COUNT
) {

    fun interface Progress {
        fun onProgress(fraction: Float)
    }

    /**
     * Encode to [output]. Blocks the caller thread. Caller is expected to run this on a
     * worker coroutine / executor — on the main thread this would freeze the UI for the
     * full encode duration (typically 0.3× – 1× of wall-clock audio length).
     */
    fun encode(output: File, progress: Progress? = null) {
        val durationSec = sampler.durationSeconds
        if (durationSec <= 0f) throw IllegalStateException("empty audio — nothing to encode")
        val totalFrames = (durationSec * fps).toInt().coerceAtLeast(1)

        val videoFormat = MediaFormat.createVideoFormat(MIME_VIDEO, width, height).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            setInteger(MediaFormat.KEY_BIT_RATE, videoBitrate)
            setInteger(MediaFormat.KEY_FRAME_RATE, fps)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 2)
        }
        val videoEncoder = MediaCodec.createEncoderByType(MIME_VIDEO).apply {
            configure(videoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        }
        val inputSurface = videoEncoder.createInputSurface()
        val egl = EglCore(inputSurface)

        val audioFormat = MediaFormat.createAudioFormat(MIME_AUDIO, sampler.sampleRate, 1).apply {
            setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            setInteger(MediaFormat.KEY_BIT_RATE, audioBitrate)
            setInteger(MediaFormat.KEY_CHANNEL_MASK, android.media.AudioFormat.CHANNEL_IN_MONO)
            setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16 * 1024)
        }
        val audioEncoder = MediaCodec.createEncoderByType(MIME_AUDIO).apply {
            configure(audioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        }

        val muxer = MediaMuxer(output.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        var videoTrackIdx = -1
        var audioTrackIdx = -1
        var muxerStarted = false
        val pendingVideo = mutableListOf<PendingSample>()
        val pendingAudio = mutableListOf<PendingSample>()

        fun maybeStartMuxer() {
            if (!muxerStarted && videoTrackIdx >= 0 && audioTrackIdx >= 0) {
                muxer.start()
                muxerStarted = true
                for (s in pendingVideo) muxer.writeSampleData(videoTrackIdx, s.data, s.info)
                for (s in pendingAudio) muxer.writeSampleData(audioTrackIdx, s.data, s.info)
                pendingVideo.clear()
                pendingAudio.clear()
            }
        }

        fun drain(encoder: MediaCodec, isVideo: Boolean, endOfStream: Boolean) {
            val bufInfo = MediaCodec.BufferInfo()
            while (true) {
                val idx = encoder.dequeueOutputBuffer(bufInfo, if (endOfStream) 10_000L else 0L)
                when {
                    idx == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                        if (!endOfStream) return
                    }
                    idx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        if (isVideo) {
                            videoTrackIdx = muxer.addTrack(encoder.outputFormat)
                        } else {
                            audioTrackIdx = muxer.addTrack(encoder.outputFormat)
                        }
                        maybeStartMuxer()
                    }
                    idx >= 0 -> {
                        val out = encoder.getOutputBuffer(idx)
                            ?: throw IllegalStateException("null output buffer at $idx")
                        if ((bufInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                            bufInfo.size = 0
                        }
                        if (bufInfo.size > 0) {
                            out.position(bufInfo.offset)
                            out.limit(bufInfo.offset + bufInfo.size)
                            val track = if (isVideo) videoTrackIdx else audioTrackIdx
                            if (muxerStarted && track >= 0) {
                                muxer.writeSampleData(track, out, bufInfo)
                            } else {
                                val copy = ByteBuffer.allocate(bufInfo.size)
                                copy.put(out)
                                copy.flip()
                                val infoCopy = MediaCodec.BufferInfo().apply {
                                    set(0, bufInfo.size, bufInfo.presentationTimeUs, bufInfo.flags)
                                }
                                if (isVideo) pendingVideo.add(PendingSample(copy, infoCopy))
                                else pendingAudio.add(PendingSample(copy, infoCopy))
                            }
                        }
                        encoder.releaseOutputBuffer(idx, false)
                        if ((bufInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) return
                    }
                }
            }
        }

        fun feedAudio(endOfStream: Boolean) {
            if (audioCursor >= sampler.samples.size && !endOfStream) return
            val idx = audioEncoder.dequeueInputBuffer(10_000L)
            if (idx < 0) return
            val inBuf = audioEncoder.getInputBuffer(idx)!!
            inBuf.clear()
            val capacityShorts = inBuf.capacity() / 2
            val remaining = sampler.samples.size - audioCursor
            val take = min(capacityShorts, remaining)
            if (take > 0) {
                val tmp = ByteBuffer.allocate(take * 2).order(ByteOrder.LITTLE_ENDIAN)
                var i = 0
                while (i < take) {
                    tmp.putShort(sampler.samples[audioCursor + i])
                    i++
                }
                tmp.flip()
                inBuf.put(tmp)
            }
            val ptsUs = (audioCursor * 1_000_000L) / sampler.sampleRate
            audioCursor += take
            val eos = remaining <= capacityShorts
            val flags = if (eos) MediaCodec.BUFFER_FLAG_END_OF_STREAM else 0
            audioEncoder.queueInputBuffer(idx, 0, take * 2, ptsUs, flags)
        }

        try {
            videoEncoder.start()
            audioEncoder.start()

            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            var frameIndex = 0
            while (frameIndex < totalFrames) {
                val t = frameIndex.toFloat() / fps
                val bars = sampler.sampleFrame(t, barCount)
                renderer.render(canvas, bars)
                egl.uploadAndDraw(bitmap)
                val ptsNs = (frameIndex.toLong() * 1_000_000_000L) / fps
                egl.setPresentationTime(ptsNs)
                egl.swapBuffers()

                drain(videoEncoder, isVideo = true, endOfStream = false)
                feedAudio(endOfStream = false)
                drain(audioEncoder, isVideo = false, endOfStream = false)

                frameIndex++
                progress?.onProgress(frameIndex.toFloat() / totalFrames)
            }
            videoEncoder.signalEndOfInputStream()
            while (audioCursor < sampler.samples.size) {
                feedAudio(endOfStream = false)
                drain(audioEncoder, isVideo = false, endOfStream = false)
            }
            drain(videoEncoder, isVideo = true, endOfStream = true)
            drain(audioEncoder, isVideo = false, endOfStream = true)
            bitmap.recycle()
        } finally {
            try { videoEncoder.stop() } catch (_: Exception) {}
            try { videoEncoder.release() } catch (_: Exception) {}
            try { audioEncoder.stop() } catch (_: Exception) {}
            try { audioEncoder.release() } catch (_: Exception) {}
            try { egl.release() } catch (_: Exception) {}
            try { inputSurface.release() } catch (_: Exception) {}
            if (muxerStarted) {
                try { muxer.stop() } catch (_: Exception) {}
            }
            try { muxer.release() } catch (_: Exception) {}
        }
    }

    private var audioCursor: Int = 0

    private data class PendingSample(val data: ByteBuffer, val info: MediaCodec.BufferInfo)

    /**
     * Minimal EGL14 wrapper that binds a MediaCodec input [Surface] and renders an RGBA
     * [Bitmap] each frame via a fullscreen textured quad. Intentionally scoped to what
     * this encoder needs — no framebuffers, no off-screen targets, no antialiasing.
     */
    private class EglCore(surface: Surface) {
        private val display: EGLDisplay
        private val context: EGLContext
        private val eglSurface: EGLSurface
        private val program: Int
        private val positionHandle: Int
        private val texCoordHandle: Int
        private val mvpHandle: Int
        private val texHandle: Int
        private val textureId: IntArray = IntArray(1)
        private val mvpMatrix = FloatArray(16)
        private val vertexBuffer: FloatBuffer
        private val texBuffer: FloatBuffer

        init {
            display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
            check(display !== EGL14.EGL_NO_DISPLAY) { "eglGetDisplay failed" }
            val version = IntArray(2)
            check(EGL14.eglInitialize(display, version, 0, version, 1)) { "eglInitialize failed" }

            val attribs = intArrayOf(
                EGL14.EGL_RED_SIZE, 8,
                EGL14.EGL_GREEN_SIZE, 8,
                EGL14.EGL_BLUE_SIZE, 8,
                EGL14.EGL_ALPHA_SIZE, 8,
                EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                EGLExt.EGL_RECORDABLE_ANDROID, 1,
                EGL14.EGL_NONE
            )
            val configs = arrayOfNulls<EGLConfig>(1)
            val numConfigs = IntArray(1)
            check(EGL14.eglChooseConfig(display, attribs, 0, configs, 0, 1, numConfigs, 0)) { "eglChooseConfig failed" }

            val ctxAttribs = intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE)
            context = EGL14.eglCreateContext(display, configs[0], EGL14.EGL_NO_CONTEXT, ctxAttribs, 0)
            check(context !== EGL14.EGL_NO_CONTEXT) { "eglCreateContext failed" }

            val surfAttribs = intArrayOf(EGL14.EGL_NONE)
            eglSurface = EGL14.eglCreateWindowSurface(display, configs[0], surface, surfAttribs, 0)
            check(eglSurface !== EGL14.EGL_NO_SURFACE) { "eglCreateWindowSurface failed" }

            check(EGL14.eglMakeCurrent(display, eglSurface, eglSurface, context)) { "eglMakeCurrent failed" }

            val vs = compileShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER)
            val fs = compileShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER)
            program = GLES20.glCreateProgram().also { p ->
                GLES20.glAttachShader(p, vs)
                GLES20.glAttachShader(p, fs)
                GLES20.glLinkProgram(p)
                val status = IntArray(1)
                GLES20.glGetProgramiv(p, GLES20.GL_LINK_STATUS, status, 0)
                check(status[0] == GLES20.GL_TRUE) { "GL program link failed: ${GLES20.glGetProgramInfoLog(p)}" }
            }
            GLES20.glDeleteShader(vs)
            GLES20.glDeleteShader(fs)

            positionHandle = GLES20.glGetAttribLocation(program, "aPosition")
            texCoordHandle = GLES20.glGetAttribLocation(program, "aTexCoord")
            mvpHandle = GLES20.glGetUniformLocation(program, "uMVP")
            texHandle = GLES20.glGetUniformLocation(program, "uTex")

            GLES20.glGenTextures(1, textureId, 0)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId[0])
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)

            Matrix.setIdentityM(mvpMatrix, 0)

            vertexBuffer = ByteBuffer.allocateDirect(QUAD_VERTICES.size * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer().apply {
                    put(QUAD_VERTICES).position(0)
                }
            texBuffer = ByteBuffer.allocateDirect(QUAD_TEX_COORDS.size * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer().apply {
                    put(QUAD_TEX_COORDS).position(0)
                }
        }

        fun uploadAndDraw(bitmap: Bitmap) {
            GLES20.glViewport(0, 0, bitmap.width, bitmap.height)
            GLES20.glClearColor(0f, 0f, 0f, 1f)
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

            GLES20.glUseProgram(program)
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId[0])
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
            GLES20.glUniform1i(texHandle, 0)
            GLES20.glUniformMatrix4fv(mvpHandle, 1, false, mvpMatrix, 0)

            GLES20.glEnableVertexAttribArray(positionHandle)
            GLES20.glVertexAttribPointer(positionHandle, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer)
            GLES20.glEnableVertexAttribArray(texCoordHandle)
            GLES20.glVertexAttribPointer(texCoordHandle, 2, GLES20.GL_FLOAT, false, 0, texBuffer)

            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

            GLES20.glDisableVertexAttribArray(positionHandle)
            GLES20.glDisableVertexAttribArray(texCoordHandle)
        }

        fun setPresentationTime(nsecs: Long) {
            EGLExt.eglPresentationTimeANDROID(display, eglSurface, nsecs)
        }

        fun swapBuffers(): Boolean = EGL14.eglSwapBuffers(display, eglSurface)

        fun release() {
            if (display !== EGL14.EGL_NO_DISPLAY) {
                EGL14.eglMakeCurrent(display, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
                EGL14.eglDestroySurface(display, eglSurface)
                EGL14.eglDestroyContext(display, context)
                EGL14.eglTerminate(display)
            }
        }

        private fun compileShader(type: Int, src: String): Int {
            val id = GLES20.glCreateShader(type)
            GLES20.glShaderSource(id, src)
            GLES20.glCompileShader(id)
            val status = IntArray(1)
            GLES20.glGetShaderiv(id, GLES20.GL_COMPILE_STATUS, status, 0)
            check(status[0] == GLES20.GL_TRUE) { "Shader compile failed: ${GLES20.glGetShaderInfoLog(id)}" }
            return id
        }

        companion object {
            private val QUAD_VERTICES = floatArrayOf(
                -1f, -1f,
                 1f, -1f,
                -1f,  1f,
                 1f,  1f
            )
            // Flip vertically — Android bitmap's (0,0) is top-left, GL's is bottom-left.
            private val QUAD_TEX_COORDS = floatArrayOf(
                0f, 1f,
                1f, 1f,
                0f, 0f,
                1f, 0f
            )
            private const val VERTEX_SHADER = """
                uniform mat4 uMVP;
                attribute vec4 aPosition;
                attribute vec2 aTexCoord;
                varying vec2 vTexCoord;
                void main() {
                    gl_Position = uMVP * aPosition;
                    vTexCoord = aTexCoord;
                }
            """
            private const val FRAGMENT_SHADER = """
                precision mediump float;
                uniform sampler2D uTex;
                varying vec2 vTexCoord;
                void main() {
                    gl_FragColor = texture2D(uTex, vTexCoord);
                }
            """
        }
    }

    companion object {
        const val DEFAULT_WIDTH = 1080
        const val DEFAULT_HEIGHT = 1920
        const val DEFAULT_FPS = 30
        const val DEFAULT_VIDEO_BITRATE = 6_000_000
        const val DEFAULT_AUDIO_BITRATE = 128_000
        const val DEFAULT_BAR_COUNT = 60
        private const val MIME_VIDEO = MediaFormat.MIMETYPE_VIDEO_AVC
        private const val MIME_AUDIO = MediaFormat.MIMETYPE_AUDIO_AAC
    }
}
