package com.pitchcoach.core.audio

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VolumeAndSilenceTest {
    private val silenceDetector = SilenceDetector()

    @Test
    fun zeroSamplesAreSilent() {
        val samples = FloatArray(2_048)
        val frame = samples.toFrame()

        assertTrue(frame.rms == 0f)
        assertTrue(silenceDetector.isSilent(frame))
    }

    @Test
    fun lowLevelNoiseStaysSilent() {
        val samples = FloatArray(2_048) { if (it % 2 == 0) 0.004f else -0.004f }
        val frame = samples.toFrame()

        assertTrue(silenceDetector.isSilent(frame))
        assertFalse(silenceDetector.isClearlyVoiced(frame))
    }

    @Test
    fun clearSignalIsVoiced() {
        val samples = FloatArray(2_048) { if (it % 2 == 0) 0.1f else -0.1f }
        val frame = samples.toFrame()

        assertFalse(silenceDetector.isSilent(frame))
        assertTrue(silenceDetector.isClearlyVoiced(frame))
    }

    private fun FloatArray.toFrame(): AudioFrame {
        return AudioFrame(
            samples = this,
            sampleRate = 44_100,
            timestampMs = 0L,
            rms = VolumeAnalyzer.rms(this),
        )
    }
}
