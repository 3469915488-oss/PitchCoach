package com.pitchcoach.core.music

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CentsCalculatorTest {
    @Test
    fun convertsFrequencyToMidi() {
        assertEquals(69f, CentsCalculator.frequencyToMidi(440f)!!, 0.001f)
        assertEquals(60f, CentsCalculator.frequencyToMidi(261.62558f)!!, 0.01f)
    }

    @Test
    fun convertsMidiToFrequency() {
        assertEquals(440f, CentsCalculator.frequencyForMidi(69), 0.001f)
        assertEquals(261.62558f, CentsCalculator.frequencyForMidi(60), 0.01f)
    }

    @Test
    fun calculatesCentsBetweenFrequencies() {
        assertEquals(100f, CentsCalculator.centsBetween(466.16376f, 440f)!!, 0.05f)
        assertEquals(-100f, CentsCalculator.centsBetween(440f, 466.16376f)!!, 0.05f)
    }

    @Test
    fun rejectsInvalidCentsInputs() {
        assertNull(CentsCalculator.frequencyToMidi(0f))
        assertNull(CentsCalculator.centsBetween(0f, 440f))
        assertNull(CentsCalculator.centsBetween(440f, 0f))
    }
}
