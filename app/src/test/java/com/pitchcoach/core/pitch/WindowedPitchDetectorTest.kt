package com.pitchcoach.core.pitch

import com.pitchcoach.core.audio.AudioFrame
import com.pitchcoach.core.audio.VolumeAnalyzer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WindowedPitchDetectorTest {
    @Test
    fun waitsForEnoughSamplesBeforeDetecting() {
        val delegate = CapturingDetector()
        val detector = WindowedPitchDetector(
            delegate = delegate,
            analysisWindowSamples = 8,
            minimumSamplesBeforeDetect = 6,
        )

        val first = detector.detect(frame(floatArrayOf(1f, 2f, 3f, 4f)))
        val second = detector.detect(frame(floatArrayOf(5f, 6f)))

        assertFalse(first.isVoiced)
        assertTrue(second.isVoiced)
        assertEquals(1, delegate.detectCount)
        assertEquals(listOf(1f, 2f, 3f, 4f, 5f, 6f), delegate.lastSamples!!.toList())
    }

    @Test
    fun keepsOnlyLatestSamplesInAnalysisWindow() {
        val delegate = CapturingDetector()
        val detector = WindowedPitchDetector(
            delegate = delegate,
            analysisWindowSamples = 6,
            minimumSamplesBeforeDetect = 6,
        )

        detector.detect(frame(floatArrayOf(1f, 2f, 3f, 4f)))
        detector.detect(frame(floatArrayOf(5f, 6f, 7f, 8f)))

        assertEquals(listOf(3f, 4f, 5f, 6f, 7f, 8f), delegate.lastSamples!!.toList())
    }

    @Test
    fun resetClearsPreviousWindowAndDelegate() {
        val delegate = CapturingDetector()
        val detector = WindowedPitchDetector(
            delegate = delegate,
            analysisWindowSamples = 6,
            minimumSamplesBeforeDetect = 6,
        )

        detector.detect(frame(floatArrayOf(1f, 2f, 3f, 4f, 5f, 6f)))
        detector.reset()
        val afterReset = detector.detect(frame(floatArrayOf(7f, 8f, 9f)))

        assertTrue(delegate.wasReset)
        assertFalse(afterReset.isVoiced)
        assertEquals(1, delegate.detectCount)
    }

    private fun frame(samples: FloatArray): AudioFrame {
        return AudioFrame(
            samples = samples,
            sampleRate = 44_100,
            timestampMs = 0L,
            rms = VolumeAnalyzer.rms(samples),
        )
    }

    private class CapturingDetector : PitchDetector {
        var detectCount = 0
        var wasReset = false
        var lastSamples: FloatArray? = null

        override fun detect(frame: AudioFrame): PitchResult {
            detectCount++
            lastSamples = frame.samples
            return PitchResult(
                frequencyHz = 440f,
                confidence = 0.99f,
                volumeRms = frame.rms,
                isVoiced = true,
                timestampMs = frame.timestampMs,
            )
        }

        override fun reset() {
            wasReset = true
        }
    }
}
