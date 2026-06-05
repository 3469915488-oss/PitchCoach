package com.pitchcoach.core.audio

import kotlin.math.sqrt

object VolumeAnalyzer {
    fun rms(samples: FloatArray): Float {
        if (samples.isEmpty()) return 0f

        var sumSquares = 0.0
        for (sample in samples) {
            if (sample.isFinite()) {
                sumSquares += sample * sample
            }
        }

        return sqrt(sumSquares / samples.size).toFloat().coerceIn(0f, 1f)
    }
}
