package com.pitchcoach.core.session

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PracticeSessionSummarizerTest {
    @Test
    fun summarizesOnlyVoicedReliableFrames() {
        val summary = PracticeSessionSummarizer.summarize(
            sessionId = "s1",
            type = "pitch_meter",
            startedAt = 1_000L,
            endedAt = 2_000L,
            frames = listOf(
                frame(midi = 60f, cents = -10f),
                frame(midi = 60.2f, cents = 20f),
                frame(midi = 61f, cents = 0f),
                frame(midi = null, cents = null, isVoiced = false),
                frame(midi = 62f, cents = 80f, confidence = 0.2f),
            ),
        )

        assertEquals(5, summary.totalFrameCount)
        assertEquals(3, summary.voicedFrameCount)
        assertEquals(10f, summary.averageAbsCents, 0.001f)
        assertEquals(1f / 3f, summary.flatRate, 0.001f)
        assertEquals(1f / 3f, summary.sharpRate, 0.001f)
        assertEquals(60, summary.noteRangeLow)
        assertEquals(61, summary.noteRangeHigh)
        assertTrue(summary.stabilityScore > 50f)
    }

    private fun frame(
        midi: Float?,
        cents: Float?,
        confidence: Float = 0.9f,
        isVoiced: Boolean = true,
    ): PitchFrameSnapshot {
        return PitchFrameSnapshot(
            timestampMs = 0L,
            frequencyHz = 440f,
            midi = midi,
            cents = cents,
            confidence = confidence,
            volumeRms = 0.1f,
            isVoiced = isVoiced,
        )
    }
}
