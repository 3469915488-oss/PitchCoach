package com.pitchcoach.core.pitch

import com.pitchcoach.core.audio.AudioFrame
import kotlin.math.max
import kotlin.math.min

class MpmPitchDetector(
    private val minFrequencyHz: Float = 50f,
    private val maxFrequencyHz: Float = 2_200f,
    private val silenceRmsThreshold: Float = 0.008f,
    private val minClarity: Float = 0.72f,
    private val peakCutoffRatio: Float = 0.93f,
) : PitchDetector {
    override fun detect(frame: AudioFrame): PitchResult {
        if (frame.samples.size < MIN_FRAME_SIZE || frame.rms < silenceRmsThreshold) {
            return frame.unvoiced()
        }

        val maxTau = min(frame.samples.size - 2, (frame.sampleRate / minFrequencyHz).toInt())
        val minTau = max(2, (frame.sampleRate / maxFrequencyHz).toInt())
        if (minTau >= maxTau) return frame.unvoiced()

        val samples = removeDc(frame.samples)
        val nsdf = normalizedSquareDifference(samples, maxTau)
        val tau = estimateTau(nsdf, minTau, maxTau) ?: return frame.unvoiced()
        val clarity = nsdf[tau].coerceIn(0f, 1f)
        val preciseTau = parabolicInterpolation(nsdf, tau, maxTau)
        val frequencyHz = frame.sampleRate / preciseTau
        val validPitch = frequencyHz.isFinite() && frequencyHz in minFrequencyHz..maxFrequencyHz
        val voiced = validPitch && clarity >= minClarity

        return PitchResult(
            frequencyHz = if (voiced) frequencyHz else null,
            confidence = clarity,
            volumeRms = frame.rms,
            isVoiced = voiced,
            timestampMs = frame.timestampMs,
        )
    }

    private fun removeDc(samples: FloatArray): FloatArray {
        var sum = 0.0
        for (sample in samples) {
            if (sample.isFinite()) sum += sample
        }
        val mean = (sum / samples.size).toFloat()
        return FloatArray(samples.size) { index ->
            (samples[index] - mean).takeIf { it.isFinite() } ?: 0f
        }
    }

    private fun normalizedSquareDifference(samples: FloatArray, maxTau: Int): FloatArray {
        val nsdf = FloatArray(maxTau + 1)
        nsdf[0] = 1f
        for (tau in 1..maxTau) {
            var acf = 0.0
            var divisor = 0.0
            val limit = samples.size - tau
            for (index in 0 until limit) {
                val current = samples[index]
                val shifted = samples[index + tau]
                acf += current * shifted
                divisor += current * current + shifted * shifted
            }
            nsdf[tau] = if (divisor <= EPSILON) {
                0f
            } else {
                (2.0 * acf / divisor).toFloat().coerceIn(-1f, 1f)
            }
        }
        return nsdf
    }

    private fun estimateTau(nsdf: FloatArray, minTau: Int, maxTau: Int): Int? {
        val candidates = mutableListOf<Int>()
        var tau = minTau
        while (tau < maxTau - 1 && nsdf[tau] > 0f) {
            tau++
        }
        while (tau < maxTau - 1) {
            while (tau < maxTau - 1 && nsdf[tau] <= 0f) {
                tau++
            }
            if (tau >= maxTau - 1) break

            var bestTau = tau
            var bestValue = nsdf[tau]
            while (tau < maxTau - 1 && nsdf[tau] > 0f) {
                if (nsdf[tau] > bestValue) {
                    bestTau = tau
                    bestValue = nsdf[tau]
                }
                tau++
            }
            if (bestValue > 0f) {
                candidates.add(bestTau)
            }
        }
        if (candidates.isEmpty()) return null

        val bestClarity = candidates.maxOf { candidate -> nsdf[candidate] }
        val cutoff = max(minClarity, bestClarity * peakCutoffRatio)
        return candidates.firstOrNull { candidate -> nsdf[candidate] >= cutoff }
            ?: candidates.maxBy { candidate -> nsdf[candidate] }
    }

    private fun parabolicInterpolation(nsdf: FloatArray, tau: Int, maxTau: Int): Float {
        if (tau <= 1 || tau >= maxTau) return tau.toFloat()

        val left = nsdf[tau - 1]
        val center = nsdf[tau]
        val right = nsdf[tau + 1]
        val denominator = 2f * (left - 2f * center + right)
        if (denominator == 0f) return tau.toFloat()

        return (tau + (left - right) / denominator).coerceAtLeast(1f)
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
        private const val EPSILON = 1.0e-12
    }
}
