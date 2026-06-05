package com.pitchcoach.core.pitch

import com.pitchcoach.core.audio.AudioFrame

class FakePitchDetector(
    private val frequencyHz: Float = 440f,
    private val confidence: Float = 0.98f,
    private val minRms: Float = 0.015f,
) : PitchDetector {
    override fun detect(frame: AudioFrame): PitchResult {
        val voiced = frame.rms >= minRms
        return PitchResult(
            frequencyHz = if (voiced) frequencyHz else null,
            confidence = if (voiced) confidence else 0f,
            volumeRms = frame.rms,
            isVoiced = voiced,
            timestampMs = frame.timestampMs,
        )
    }
}
