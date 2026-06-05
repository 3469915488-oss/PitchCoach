package com.pitchcoach.core.music

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NoteMapperTest {
    @Test
    fun mapsA4ToMidi69WithZeroCents() {
        val note = NoteMapper.estimate(440f)

        requireNotNull(note)
        assertEquals("A", note.noteName)
        assertEquals(4, note.octave)
        assertEquals(69, note.midi)
        assertEquals(0f, note.cents, 0.01f)
    }

    @Test
    fun mapsC4ReferenceFrequency() {
        val note = NoteMapper.estimate(261.62558f)

        requireNotNull(note)
        assertEquals("C", note.noteName)
        assertEquals(4, note.octave)
        assertEquals(60, note.midi)
        assertEquals(0f, note.cents, 0.05f)
    }

    @Test
    fun rejectsInvalidFrequencies() {
        assertNull(NoteMapper.estimate(0f))
        assertNull(NoteMapper.estimate(-440f))
    }
}
