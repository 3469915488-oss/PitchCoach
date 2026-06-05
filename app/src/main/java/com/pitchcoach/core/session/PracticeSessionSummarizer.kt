package com.pitchcoach.core.session

import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

object PracticeSessionSummarizer {
    fun summarize(
        sessionId: String,
        type: String,
        startedAt: Long,
        endedAt: Long,
        frames: List<PitchFrameSnapshot>,
        minConfidence: Float = 0.45f,
    ): PracticeSessionSummary {
        val voicedFrames = frames.filter { frame ->
            frame.isVoiced &&
                frame.cents != null &&
                frame.midi != null &&
                frame.confidence >= minConfidence
        }
        val centsValues = voicedFrames.mapNotNull { it.cents }
        val midiValues = voicedFrames.mapNotNull { it.midi?.roundToInt() }

        val averageAbsCents = centsValues.takeIf { it.isNotEmpty() }
            ?.map { abs(it) }
            ?.average()
            ?.toFloat()
            ?: 0f

        val flatRate = centsValues.rate { it < -DIRECTION_TOLERANCE_CENTS }
        val sharpRate = centsValues.rate { it > DIRECTION_TOLERANCE_CENTS }
        val stabilityScore = stabilityScore(centsValues)

        return PracticeSessionSummary(
            sessionId = sessionId,
            type = type,
            startedAt = startedAt,
            endedAt = endedAt,
            averageAbsCents = averageAbsCents,
            flatRate = flatRate,
            sharpRate = sharpRate,
            stabilityScore = stabilityScore,
            noteRangeLow = midiValues.minOrNull(),
            noteRangeHigh = midiValues.maxOrNull(),
            totalFrameCount = frames.size,
            voicedFrameCount = voicedFrames.size,
        )
    }

    private fun List<Float>.rate(predicate: (Float) -> Boolean): Float {
        if (isEmpty()) return 0f
        return count(predicate).toFloat() / size
    }

    private fun stabilityScore(centsValues: List<Float>): Float {
        if (centsValues.size < 2) return 0f

        val mean = centsValues.average().toFloat()
        val variance = centsValues
            .map { cents -> (cents - mean) * (cents - mean) }
            .average()
            .toFloat()

        val standardDeviation = sqrt(variance)
        return ((1f - standardDeviation / STABILITY_CENTS_WINDOW).coerceIn(0f, 1f) * 100f)
    }

    private const val DIRECTION_TOLERANCE_CENTS = 5f
    private const val STABILITY_CENTS_WINDOW = 50f
}
