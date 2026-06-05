package com.pitchcoach.core.audio

class SilenceDetector(
    private val rmsThreshold: Float = 0.008f,
    private val voicedThreshold: Float = 0.015f,
) {
    fun isSilent(frame: AudioFrame): Boolean = frame.samples.isEmpty() || frame.rms < rmsThreshold

    fun isClearlyVoiced(frame: AudioFrame): Boolean = frame.rms >= voicedThreshold
}
