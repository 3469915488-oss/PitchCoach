package com.pitchcoach.core.pitch

import com.pitchcoach.core.audio.AudioFrame
import com.pitchcoach.core.audio.VolumeAnalyzer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

class HybridPitchDetectorTest {
    @Test
    fun detectsHighCWithoutOctaveClamp() {
        val result = HybridPitchDetector().detect(sineFrame(frequencyHz = 1046.5f))

        assertTrue(result.isVoiced)
        assertEquals(1046.5f, result.frequencyHz!!, 8f)
    }

    @Test
    fun keepsGuitarLowEInRange() {
        val result = HybridPitchDetector().detect(sineFrame(frequencyHz = 82.41f))

        assertTrue(result.isVoiced)
        assertEquals(82.41f, result.frequencyHz!!, 2f)
    }

    @Test
    fun choosesLowerOctaveWhenAlgorithmsDisagreeByOctaveAndLowerIsReliable() {
        val detector = HybridPitchDetector(
            yinDetector = staticDetector(523.25f, confidence = 0.84f),
            mpmDetector = staticDetector(261.63f, confidence = 0.80f),
        )

        val result = detector.detect(sineFrame(frequencyHz = 261.63f))

        assertTrue(result.isVoiced)
        assertEquals(261.63f, result.frequencyHz!!, 0.01f)
    }

    @Test
    fun keepsHigherConfidenceCandidateWhenLowerOctaveIsWeak() {
        val detector = HybridPitchDetector(
            yinDetector = staticDetector(1046.5f, confidence = 0.95f),
            mpmDetector = staticDetector(523.25f, confidence = 0.60f),
        )

        val result = detector.detect(sineFrame(frequencyHz = 1046.5f))

        assertTrue(result.isVoiced)
        assertEquals(1046.5f, result.frequencyHz!!, 0.01f)
    }

    @Test
    fun rejectsHardNonOctaveDisagreementWhenNeitherAlgorithmClearlyWins() {
        val detector = HybridPitchDetector(
            yinDetector = staticDetector(440f, confidence = 0.84f),
            mpmDetector = staticDetector(493.88f, confidence = 0.78f),
        )

        val result = detector.detect(sineFrame(frequencyHz = 440f))

        assertTrue(!result.isVoiced)
    }

    private fun staticDetector(frequencyHz: Float, confidence: Float): PitchDetector {
        return object : PitchDetector {
            override fun detect(frame: AudioFrame): PitchResult {
                return PitchResult(
                    frequencyHz = frequencyHz,
                    confidence = confidence,
                    volumeRms = frame.rms,
                    isVoiced = true,
                    timestampMs = frame.timestampMs,
                )
            }
        }
    }

    private fun sineFrame(frequencyHz: Float): AudioFrame {
        val samples = FloatArray(4_096) { index ->
            (sin(2.0 * PI * frequencyHz * index / SAMPLE_RATE) * 0.6).toFloat()
        }
        return AudioFrame(
            samples = samples,
            sampleRate = SAMPLE_RATE,
            timestampMs = 0L,
            rms = VolumeAnalyzer.rms(samples),
        )
    }

    companion object {
        private const val SAMPLE_RATE = 44_100
    }
}
