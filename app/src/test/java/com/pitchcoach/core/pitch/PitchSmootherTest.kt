package com.pitchcoach.core.pitch

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PitchSmootherTest {
    @Test
    fun clearsImmediatelyOnSilence() {
        val smoother = PitchSmoother()
        smoother.smooth(voiced(440f, timestampMs = 1L))

        val silent = smoother.smooth(
            PitchResult(
                frequencyHz = null,
                confidence = 0f,
                volumeRms = 0f,
                isVoiced = false,
                timestampMs = 2L,
            )
        )

        assertFalse(silent.isVoiced)
        assertEquals(0f, smoother.stabilityScore(), 0.001f)
    }

    @Test
    fun holdsBriefLowConfidenceFramesThenDrops() {
        val smoother = PitchSmoother(maxDroppedFrames = 2)
        smoother.smooth(voiced(440f, timestampMs = 1L))

        assertTrue(smoother.smooth(voiced(441f, confidence = 0.2f, timestampMs = 2L)).isVoiced)
        assertTrue(smoother.smooth(voiced(442f, confidence = 0.2f, timestampMs = 3L)).isVoiced)
        assertFalse(smoother.smooth(voiced(443f, confidence = 0.2f, timestampMs = 4L)).isVoiced)
    }

    @Test
    fun smoothsMedianFrequency() {
        val smoother = PitchSmoother(maxHistorySize = 3)

        smoother.smooth(voiced(440f, timestampMs = 1L))
        smoother.smooth(voiced(442f, timestampMs = 2L))
        val result = smoother.smooth(voiced(441f, timestampMs = 3L))

        assertEquals(441f, result.frequencyHz!!, 0.01f)
        assertTrue(smoother.stabilityScore() > 80f)
    }

    @Test
    fun correctsLikelyOctaveJump() {
        val smoother = PitchSmoother(maxHistorySize = 3, correctOctaveErrors = true)

        smoother.smooth(voiced(440f, timestampMs = 1L))
        val result = smoother.smooth(voiced(880f, timestampMs = 2L))

        assertEquals(440f, result.frequencyHz!!, 1f)
    }

    @Test
    fun preservesRealOctaveJumpByDefault() {
        val smoother = PitchSmoother(maxHistorySize = 3)

        smoother.smooth(voiced(261.63f, timestampMs = 1L))
        val result = smoother.smooth(voiced(523.25f, timestampMs = 2L))

        assertEquals(523.25f, result.frequencyHz!!, 1f)
    }

    private fun voiced(
        frequencyHz: Float,
        confidence: Float = 0.95f,
        timestampMs: Long,
    ): PitchResult {
        return PitchResult(
            frequencyHz = frequencyHz,
            confidence = confidence,
            volumeRms = 0.1f,
            isVoiced = true,
            timestampMs = timestampMs,
        )
    }
}
