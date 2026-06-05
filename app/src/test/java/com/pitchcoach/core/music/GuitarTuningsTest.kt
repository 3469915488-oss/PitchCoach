package com.pitchcoach.core.music

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GuitarTuningsTest {
    @Test
    fun standardTuningUsesExpectedStringNotes() {
        val notes = GuitarTunings.standard.strings.map { it.displayName }

        assertEquals(listOf("E2", "A2", "D3", "G3", "B3", "E4"), notes)
    }

    @Test
    fun halfStepDownUsesFlatTargets() {
        val notes = GuitarTunings.halfStepDown.strings.map { it.displayName }

        assertEquals(listOf("Eb2", "Ab2", "Db3", "Gb3", "Bb3", "Eb4"), notes)
    }

    @Test
    fun matchesClosestStringTargetInSelectedTuning() {
        val match = GuitarTunings.match(110f, GuitarTunings.standard)

        assertEquals(5, match?.target?.number)
        assertEquals("A2", match?.target?.displayName)
        assertTrue(kotlin.math.abs(match!!.cents) < 0.1f)
    }
}
