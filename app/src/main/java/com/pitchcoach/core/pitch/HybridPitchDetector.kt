package com.pitchcoach.core.pitch

import com.pitchcoach.core.audio.AudioFrame
import kotlin.math.abs
import kotlin.math.ln

class HybridPitchDetector(
    private val yinDetector: PitchDetector = YinPitchDetector(),
    private val mpmDetector: PitchDetector = MpmPitchDetector(),
) : PitchDetector {
    override fun detect(frame: AudioFrame): PitchResult {
        val yin = yinDetector.detect(frame)
        val mpm = mpmDetector.detect(frame)

        val yinFrequency = yin.frequencyHz
        val mpmFrequency = mpm.frequencyHz

        if (!yin.isVoiced || yinFrequency == null) return mpm.takeIf { it.isVoiced } ?: yin
        if (!mpm.isVoiced || mpmFrequency == null) return yin

        val distance = abs(centsDistance(yinFrequency, mpmFrequency))
        if (distance <= AGREEMENT_CENTS) {
            return higherConfidence(yin, mpm)
        }

        if (isOctaveApart(yinFrequency, mpmFrequency)) {
            val lower = if (yinFrequency < mpmFrequency) yin else mpm
            val higher = if (yinFrequency < mpmFrequency) mpm else yin
            if (lower.confidence >= OCTAVE_LOWER_CONFIDENCE &&
                lower.confidence + OCTAVE_CONFIDENCE_MARGIN >= higher.confidence
            ) {
                return lower
            }
            return higherConfidence(yin, mpm)
        }

        val confidenceGap = abs(yin.confidence - mpm.confidence)
        if (distance >= HARD_DISAGREEMENT_CENTS && confidenceGap < REQUIRED_CONFIDENCE_GAP) {
            return frame.unvoiced(confidence = minOf(yin.confidence, mpm.confidence) * DISAGREEMENT_CONFIDENCE_SCALE)
        }

        return higherConfidence(yin, mpm)
    }

    override fun reset() {
        yinDetector.reset()
        mpmDetector.reset()
    }

    private fun higherConfidence(first: PitchResult, second: PitchResult): PitchResult {
        return if (first.confidence >= second.confidence) first else second
    }

    private fun isOctaveApart(firstHz: Float, secondHz: Float): Boolean {
        val distance = abs(centsDistance(firstHz, secondHz))
        return abs(distance - CENTS_PER_OCTAVE) <= OCTAVE_TOLERANCE_CENTS
    }

    private fun centsDistance(firstHz: Float, secondHz: Float): Float {
        return CENTS_PER_OCTAVE * log2(firstHz / secondHz)
    }

    private fun log2(value: Float): Float = (ln(value.toDouble()) / ln(2.0)).toFloat()

    private fun AudioFrame.unvoiced(confidence: Float): PitchResult {
        return PitchResult(
            frequencyHz = null,
            confidence = confidence.coerceIn(0f, 1f),
            volumeRms = rms,
            isVoiced = false,
            timestampMs = timestampMs,
        )
    }

    companion object {
        private const val AGREEMENT_CENTS = 35f
        private const val CENTS_PER_OCTAVE = 1_200f
        private const val OCTAVE_TOLERANCE_CENTS = 55f
        private const val OCTAVE_LOWER_CONFIDENCE = 0.78f
        private const val OCTAVE_CONFIDENCE_MARGIN = 0.08f
        private const val HARD_DISAGREEMENT_CENTS = 90f
        private const val REQUIRED_CONFIDENCE_GAP = 0.18f
        private const val DISAGREEMENT_CONFIDENCE_SCALE = 0.45f
    }
}
