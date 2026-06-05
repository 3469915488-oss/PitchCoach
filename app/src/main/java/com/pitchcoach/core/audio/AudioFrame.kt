package com.pitchcoach.core.audio

data class AudioFrame(
    val samples: FloatArray,
    val sampleRate: Int,
    val timestampMs: Long,
    val rms: Float,
)
