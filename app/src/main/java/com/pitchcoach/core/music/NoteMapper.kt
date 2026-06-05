package com.pitchcoach.core.music

import kotlin.math.roundToInt

object NoteMapper {
    private val noteNames = arrayOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")

    fun estimate(frequencyHz: Float): NoteEstimate? {
        val midiFloat = CentsCalculator.frequencyToMidi(frequencyHz) ?: return null
        val nearestMidi = midiFloat.roundToInt()
        val cents = (midiFloat - nearestMidi) * 100f

        return NoteEstimate(
            noteName = noteName(nearestMidi),
            octave = octave(nearestMidi),
            midi = nearestMidi,
            cents = cents,
            frequencyHz = frequencyHz,
        )
    }

    fun noteNameWithOctave(midi: Int): String = "${noteName(midi)}${octave(midi)}"

    fun noteName(midi: Int): String = noteNames[Math.floorMod(midi, 12)]

    fun octave(midi: Int): Int = (midi / 12) - 1
}
