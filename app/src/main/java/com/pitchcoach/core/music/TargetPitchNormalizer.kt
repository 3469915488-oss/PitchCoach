package com.pitchcoach.core.music

import kotlin.math.abs
import kotlin.math.pow

object TargetPitchNormalizer {
    fun centsToTargetOctave(
        actualFrequencyHz: Float,
        targetFrequencyHz: Float,
        maxOctaveShift: Int = DEFAULT_MAX_OCTAVE_SHIFT,
    ): Float? {
        val normalizedFrequency = normalizeToTargetOctave(
            actualFrequencyHz = actualFrequencyHz,
            targetFrequencyHz = targetFrequencyHz,
            maxOctaveShift = maxOctaveShift,
        ) ?: return null
        return CentsCalculator.centsBetween(normalizedFrequency, targetFrequencyHz)
    }

    fun normalizeToTargetOctave(
        actualFrequencyHz: Float,
        targetFrequencyHz: Float,
        maxOctaveShift: Int = DEFAULT_MAX_OCTAVE_SHIFT,
    ): Float? {
        if (actualFrequencyHz <= 0f || targetFrequencyHz <= 0f) return null
        if (!actualFrequencyHz.isFinite() || !targetFrequencyHz.isFinite()) return null

        return (-maxOctaveShift..maxOctaveShift)
            .map { octaveShift -> actualFrequencyHz * 2.0.pow(octaveShift).toFloat() }
            .minByOrNull { candidate ->
                abs(CentsCalculator.centsBetween(candidate, targetFrequencyHz) ?: Float.MAX_VALUE)
            }
    }

    private const val DEFAULT_MAX_OCTAVE_SHIFT = 3
}
