package com.pitchcoach.core.pitch

import kotlin.math.ln
import kotlin.math.abs
import kotlin.math.sqrt

class PitchSmoother(
    private val maxHistorySize: Int = 9,
    private val minConfidence: Float = 0.55f,
    private val maxDroppedFrames: Int = 8,
    private val maxSmoothJumpCents: Float = 250f,
    private val correctOctaveErrors: Boolean = false,
) {
    private val frequencyHistory = ArrayDeque<Float>()
    private var lastSmoothedResult: PitchResult? = null
    private var droppedFrames = 0

    fun smooth(result: PitchResult): PitchResult {
        val frequency = result.frequencyHz
        if (!result.isVoiced || frequency == null || !frequency.isFinite()) {
            reset()
            return result.copy(frequencyHz = null, isVoiced = false)
        }

        if (result.confidence < minConfidence) {
            droppedFrames++
            val fallback = lastSmoothedResult
            return if (fallback != null && droppedFrames <= maxDroppedFrames) {
                fallback.copy(
                    confidence = result.confidence,
                    volumeRms = result.volumeRms,
                    timestampMs = result.timestampMs,
                )
            } else {
                reset()
                result.copy(frequencyHz = null, isVoiced = false)
            }
        }

        droppedFrames = 0
        val correctedFrequency = if (correctOctaveErrors) {
            correctLikelyOctaveError(frequency)
        } else {
            frequency
        }
        clearHistoryForLargePitchJump(correctedFrequency)
        frequencyHistory.addLast(correctedFrequency)
        while (frequencyHistory.size > maxHistorySize) {
            frequencyHistory.removeFirst()
        }

        val medianFrequency = median(frequencyHistory)
        return result.copy(frequencyHz = medianFrequency, isVoiced = true).also {
            lastSmoothedResult = it
        }
    }

    fun stabilityScore(): Float {
        if (frequencyHistory.size < 2) return 0f

        val medianFrequency = median(frequencyHistory)
        val centsOffsets = frequencyHistory.map { frequency ->
            1200f * log2(frequency / medianFrequency)
        }
        val mean = centsOffsets.average().toFloat()
        val variance = centsOffsets
            .map { offset -> (offset - mean) * (offset - mean) }
            .average()
            .toFloat()
        val standardDeviation = sqrt(variance)

        return ((1f - standardDeviation / STABILITY_CENTS_WINDOW).coerceIn(0f, 1f) * 100f)
    }

    fun reset() {
        frequencyHistory.clear()
        lastSmoothedResult = null
        droppedFrames = 0
    }

    private fun correctLikelyOctaveError(frequencyHz: Float): Float {
        if (frequencyHistory.isEmpty()) return frequencyHz

        val reference = median(frequencyHistory)
        val candidates = listOf(frequencyHz / 2f, frequencyHz, frequencyHz * 2f)
            .filter { it in MIN_REASONABLE_FREQUENCY..MAX_REASONABLE_FREQUENCY }

        if (candidates.isEmpty()) return frequencyHz

        return candidates
            .minBy { candidate -> abs(centsDistance(candidate, reference)) }
    }

    private fun clearHistoryForLargePitchJump(frequencyHz: Float) {
        if (frequencyHistory.isEmpty()) return

        val reference = median(frequencyHistory)
        if (abs(centsDistance(frequencyHz, reference)) > maxSmoothJumpCents) {
            frequencyHistory.clear()
        }
    }

    private fun centsDistance(a: Float, b: Float): Float = 1200f * log2(a / b)

    private fun median(values: Collection<Float>): Float {
        val sorted = values.sorted()
        val middle = sorted.size / 2
        return if (sorted.size % 2 == 0) {
            (sorted[middle - 1] + sorted[middle]) / 2f
        } else {
            sorted[middle]
        }
    }

    private fun log2(value: Float): Float = (ln(value.toDouble()) / ln(2.0)).toFloat()

    companion object {
        private const val STABILITY_CENTS_WINDOW = 35f
        private const val MIN_REASONABLE_FREQUENCY = 50f
        private const val MAX_REASONABLE_FREQUENCY = 2_000f
    }
}
