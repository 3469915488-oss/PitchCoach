package com.pitchcoach.core.music

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TargetPitchNormalizerTest {
    @Test
    fun foldsDetectedHarmonicToSelectedGuitarStringOctave() {
        val cents = TargetPitchNormalizer.centsToTargetOctave(
            actualFrequencyHz = CentsCalculator.frequencyForMidi(52),
            targetFrequencyHz = CentsCalculator.frequencyForMidi(40),
        )

        assertEquals(0f, cents!!, 0.01f)
    }

    @Test
    fun preservesDetuneAfterOctaveFold() {
        val target = CentsCalculator.frequencyForMidi(40)
        val detectedHarmonicFiveCentsSharp = target * 2f * 1.0028923f

        val cents = TargetPitchNormalizer.centsToTargetOctave(
            actualFrequencyHz = detectedHarmonicFiveCentsSharp,
            targetFrequencyHz = target,
        )

        assertEquals(5f, cents!!, 0.2f)
    }

    @Test
    fun returnsNullForInvalidFrequencies() {
        assertNull(TargetPitchNormalizer.centsToTargetOctave(0f, 82.41f))
        assertNull(TargetPitchNormalizer.centsToTargetOctave(82.41f, -1f))
    }
}
