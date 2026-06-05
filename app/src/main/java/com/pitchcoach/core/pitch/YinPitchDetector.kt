package com.pitchcoach.core.pitch

import com.pitchcoach.core.audio.AudioFrame
import kotlin.math.max
import kotlin.math.min

class YinPitchDetector(
    private val minFrequencyHz: Float = 50f,
    private val maxFrequencyHz: Float = 2_200f,
    private val threshold: Float = 0.15f,
    private val silenceRmsThreshold: Float = 0.008f,
    private val minConfidence: Float = 0.55f,
) : PitchDetector {
    override fun detect(frame: AudioFrame): PitchResult {
        if (frame.samples.size < MIN_FRAME_SIZE || frame.rms < silenceRmsThreshold) {
            return frame.unvoiced()
        }

        val maxTau = min(frame.samples.size / 2, (frame.sampleRate / minFrequencyHz).toInt())
        val minTau = max(2, (frame.sampleRate / maxFrequencyHz).toInt())
        if (minTau >= maxTau) return frame.unvoiced()

        val difference = difference(frame.samples, maxTau)
        val cumulative = cumulativeMeanNormalizedDifference(difference, maxTau)
        val initialTau = absoluteThreshold(cumulative, minTau, maxTau) ?: bestTau(cumulative, minTau, maxTau)
        val tau = correctLikelyOctaveDoubling(cumulative, initialTau, maxTau)
        val confidence = (1f - cumulative[tau]).coerceIn(0f, 1f)
        val preciseTau = parabolicInterpolation(cumulative, tau, maxTau)
        val frequencyHz = frame.sampleRate / preciseTau
        val validPitch = frequencyHz.isFinite() && frequencyHz in minFrequencyHz..maxFrequencyHz
        val voiced = validPitch && confidence >= minConfidence

        return PitchResult(
            frequencyHz = if (voiced) frequencyHz else null,
            confidence = confidence,
            volumeRms = frame.rms,
            isVoiced = voiced,
            timestampMs = frame.timestampMs,
        )
    }

    private fun difference(samples: FloatArray, maxTau: Int): FloatArray {
        val difference = FloatArray(maxTau + 1)
        for (tau in 1..maxTau) {
            var sum = 0f
            val limit = samples.size - tau
            for (index in 0 until limit) {
                val delta = samples[index] - samples[index + tau]
                sum += delta * delta
            }
            difference[tau] = sum
        }
        return difference
    }

    private fun cumulativeMeanNormalizedDifference(difference: FloatArray, maxTau: Int): FloatArray {
        val cumulative = FloatArray(maxTau + 1)
        cumulative[0] = 1f
        var runningSum = 0f
        for (tau in 1..maxTau) {
            runningSum += difference[tau]
            cumulative[tau] = if (runningSum == 0f) {
                1f
            } else {
                difference[tau] * tau / runningSum
            }
        }
        return cumulative
    }

    private fun absoluteThreshold(cumulative: FloatArray, minTau: Int, maxTau: Int): Int? {
        var tau = minTau
        while (tau <= maxTau) {
            if (cumulative[tau] < threshold) {
                while (tau + 1 <= maxTau && cumulative[tau + 1] < cumulative[tau]) {
                    tau++
                }
                return tau
            }
            tau++
        }
        return null
    }

    private fun bestTau(cumulative: FloatArray, minTau: Int, maxTau: Int): Int {
        var bestTau = minTau
        var bestValue = cumulative[minTau]
        for (tau in minTau + 1..maxTau) {
            if (cumulative[tau] < bestValue) {
                bestTau = tau
                bestValue = cumulative[tau]
            }
        }
        return bestTau
    }

    private fun parabolicInterpolation(cumulative: FloatArray, tau: Int, maxTau: Int): Float {
        if (tau <= 1 || tau >= maxTau) return tau.toFloat()

        val left = cumulative[tau - 1]
        val center = cumulative[tau]
        val right = cumulative[tau + 1]
        val denominator = 2f * (left - 2f * center + right)
        if (denominator == 0f) return tau.toFloat()

        return (tau + (left - right) / denominator).coerceAtLeast(1f)
    }

    private fun correctLikelyOctaveDoubling(cumulative: FloatArray, tau: Int, maxTau: Int): Int {
        val doubledTau = tau * 2
        if (doubledTau > maxTau) return tau

        val tauValue = cumulative[tau]
        val doubledTauValue = localMinimumNear(cumulative, doubledTau, maxTau).let { candidate ->
            cumulative[candidate]
        }

        if (tauValue < OCTAVE_DOUBLING_SUSPECT_THRESHOLD) return tau
        if (doubledTauValue >= threshold) return tau
        if (doubledTauValue > tauValue * OCTAVE_DOUBLING_IMPROVEMENT_RATIO) return tau

        return localMinimumNear(cumulative, doubledTau, maxTau)
    }

    private fun localMinimumNear(cumulative: FloatArray, center: Int, maxTau: Int): Int {
        val start = max(2, center - OCTAVE_SEARCH_RADIUS)
        val end = min(maxTau, center + OCTAVE_SEARCH_RADIUS)
        var bestTau = center.coerceIn(start, end)
        var bestValue = cumulative[bestTau]
        for (candidate in start..end) {
            if (cumulative[candidate] < bestValue) {
                bestTau = candidate
                bestValue = cumulative[candidate]
            }
        }
        return bestTau
    }

    private fun AudioFrame.unvoiced(): PitchResult {
        return PitchResult(
            frequencyHz = null,
            confidence = 0f,
            volumeRms = rms,
            isVoiced = false,
            timestampMs = timestampMs,
        )
    }

    companion object {
        private const val MIN_FRAME_SIZE = 128
        private const val OCTAVE_DOUBLING_SUSPECT_THRESHOLD = 0.02f
        private const val OCTAVE_DOUBLING_IMPROVEMENT_RATIO = 0.25f
        private const val OCTAVE_SEARCH_RADIUS = 3
    }
}
