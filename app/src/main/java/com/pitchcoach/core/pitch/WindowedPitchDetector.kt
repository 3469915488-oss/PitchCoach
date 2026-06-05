package com.pitchcoach.core.pitch

import com.pitchcoach.core.audio.AudioFrame
import com.pitchcoach.core.audio.VolumeAnalyzer

class WindowedPitchDetector(
    private val delegate: PitchDetector = HybridPitchDetector(),
    private val analysisWindowSamples: Int = DEFAULT_ANALYSIS_WINDOW_SAMPLES,
    private val minimumSamplesBeforeDetect: Int = DEFAULT_MINIMUM_SAMPLES_BEFORE_DETECT,
) : PitchDetector {
    private val sampleWindow = FloatArray(analysisWindowSamples)
    private var sampleCount = 0
    private var sampleRate: Int? = null

    override fun detect(frame: AudioFrame): PitchResult {
        if (frame.samples.isEmpty()) return frame.unvoiced()
        if (sampleRate != frame.sampleRate) {
            reset()
            sampleRate = frame.sampleRate
        }

        append(frame.samples)
        if (sampleCount < minimumSamplesBeforeDetect) return frame.unvoiced()

        val window = currentWindow()
        return delegate.detect(
            frame.copy(
                samples = window,
                rms = VolumeAnalyzer.rms(window),
            ),
        )
    }

    override fun reset() {
        sampleCount = 0
        sampleRate = null
        delegate.reset()
    }

    private fun append(samples: FloatArray) {
        when {
            samples.size >= analysisWindowSamples -> {
                samples.copyInto(
                    destination = sampleWindow,
                    destinationOffset = 0,
                    startIndex = samples.size - analysisWindowSamples,
                    endIndex = samples.size,
                )
                sampleCount = analysisWindowSamples
            }
            sampleCount + samples.size <= analysisWindowSamples -> {
                samples.copyInto(sampleWindow, destinationOffset = sampleCount)
                sampleCount += samples.size
            }
            else -> {
                val overflow = sampleCount + samples.size - analysisWindowSamples
                sampleWindow.copyInto(
                    destination = sampleWindow,
                    destinationOffset = 0,
                    startIndex = overflow,
                    endIndex = sampleCount,
                )
                samples.copyInto(sampleWindow, destinationOffset = sampleCount - overflow)
                sampleCount = analysisWindowSamples
            }
        }
    }

    private fun currentWindow(): FloatArray {
        return if (sampleCount == analysisWindowSamples) {
            sampleWindow.copyOf()
        } else {
            sampleWindow.copyOfRange(0, sampleCount)
        }
    }

    private fun AudioFrame.unvoiced(): PitchResult {
        return PitchResult(
            frequencyHz = null,
            confidence = 0f,
            volumeRms = rms,
            isVoiced = false,
            timestampMs = timestampMs,
        )
    }

    companion object {
        const val DEFAULT_ANALYSIS_WINDOW_SAMPLES = 8_192
        const val DEFAULT_MINIMUM_SAMPLES_BEFORE_DETECT = 6_144
    }
}
