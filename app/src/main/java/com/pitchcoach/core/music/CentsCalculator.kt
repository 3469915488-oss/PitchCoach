package com.pitchcoach.core.music

import kotlin.math.ln
import kotlin.math.pow

object CentsCalculator {
    private const val A4_FREQUENCY = 440.0
    private const val A4_MIDI = 69.0
    private const val SEMITONES_PER_OCTAVE = 12.0
    private const val CENTS_PER_OCTAVE = 1200.0

    fun frequencyToMidi(frequencyHz: Float): Float? {
        if (frequencyHz <= 0f || !frequencyHz.isFinite()) return null
        val midi = A4_MIDI + SEMITONES_PER_OCTAVE * log2(frequencyHz / A4_FREQUENCY)
        return midi.toFloat()
    }

    fun frequencyForMidi(midi: Int): Float {
        return (A4_FREQUENCY * 2.0.pow((midi - A4_MIDI) / SEMITONES_PER_OCTAVE)).toFloat()
    }

    fun centsBetween(actualFrequencyHz: Float, targetFrequencyHz: Float): Float? {
        if (actualFrequencyHz <= 0f || targetFrequencyHz <= 0f) return null
        if (!actualFrequencyHz.isFinite() || !targetFrequencyHz.isFinite()) return null
        return (CENTS_PER_OCTAVE * log2(actualFrequencyHz.toDouble() / targetFrequencyHz.toDouble())).toFloat()
    }

    private fun log2(value: Double): Double = ln(value) / ln(2.0)
}
