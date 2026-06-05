package com.pitchcoach.features.pitchmeter

import com.pitchcoach.core.music.NoteEstimate
import com.pitchcoach.core.pitch.PitchAnalysisState
import com.pitchcoach.core.pitch.PitchDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PitchDisplayStabilizerTest {
    @Test
    fun holdsLastPitchThroughBriefUnvoicedGap() {
        val stabilizer = PitchDisplayStabilizer(holdAfterUnvoicedMs = 1_400L)

        stabilizer.stabilize(voiced(timestampMs = 0L, midi = 69, cents = -8f))
        val held = stabilizer.stabilize(unvoiced(timestampMs = 500L))

        assertEquals(69, held.analysis.note?.midi)
        assertTrue(held.isHolding)
        assertFalse(held.analysis.isSilent)
    }

    @Test
    fun clearsAfterHoldWindowExpires() {
        val stabilizer = PitchDisplayStabilizer(holdAfterUnvoicedMs = 1_400L)

        stabilizer.stabilize(voiced(timestampMs = 0L, midi = 69))
        val cleared = stabilizer.stabilize(unvoiced(timestampMs = 1_500L))

        assertEquals(null, cleared.analysis.note)
        assertFalse(cleared.isHolding)
    }

    @Test
    fun requiresConsecutiveFramesBeforeSwitchingDisplayedNote() {
        val stabilizer = PitchDisplayStabilizer(minFramesForNoteSwitch = 2)

        stabilizer.stabilize(voiced(timestampMs = 0L, midi = 69))
        val firstSwitchFrame = stabilizer.stabilize(voiced(timestampMs = 100L, midi = 70))
        val secondSwitchFrame = stabilizer.stabilize(voiced(timestampMs = 200L, midi = 70))

        assertEquals(69, firstSwitchFrame.analysis.note?.midi)
        assertTrue(firstSwitchFrame.isHolding)
        assertEquals(70, secondSwitchFrame.analysis.note?.midi)
        assertFalse(secondSwitchFrame.isHolding)
    }

    private fun voiced(
        timestampMs: Long,
        midi: Int,
        cents: Float = 0f,
    ): PitchAnalysisState {
        return PitchAnalysisState(
            timestampMs = timestampMs,
            frequencyHz = 440f,
            note = NoteEstimate(
                noteName = "A",
                octave = 4,
                midi = midi,
                cents = cents,
                frequencyHz = 440f,
            ),
            confidence = 0.9f,
            volumeRms = 0.1f,
            isSilent = false,
            isVoiced = true,
            direction = PitchDirection.IN_TUNE,
            stabilityScore = 90f,
        )
    }

    private fun unvoiced(timestampMs: Long): PitchAnalysisState {
        return PitchAnalysisState(
            timestampMs = timestampMs,
            frequencyHz = null,
            note = null,
            confidence = 0f,
            volumeRms = 0f,
            isSilent = true,
            isVoiced = false,
            direction = PitchDirection.SILENT,
            stabilityScore = 0f,
        )
    }
}
