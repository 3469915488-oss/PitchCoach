package com.pitchcoach.core.pitch

import com.pitchcoach.core.audio.AudioFrame
import com.pitchcoach.core.audio.VolumeAnalyzer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

class MpmPitchDetectorTest {
    private val detector = MpmPitchDetector()

    @Test
    fun detectsA4SineWave() {
        val result = detector.detect(sineFrame(frequencyHz = 440f))

        assertTrue(result.isVoiced)
        assertEquals(440f, result.frequencyHz!!, 2f)
        assertTrue(result.confidence > 0.9f)
    }

    @Test
    fun detectsC6WithoutFallingToC5() {
        val result = detector.detect(sineFrame(frequencyHz = 1046.5f))

        assertTrue(result.isVoiced)
        assertEquals(1046.5f, result.frequencyHz!!, 8f)
    }

    @Test
    fun detectsGuitarLowE() {
        val result = detector.detect(sineFrame(frequencyHz = 82.41f))

        assertTrue(result.isVoiced)
        assertEquals(82.41f, result.frequencyHz!!, 2f)
    }

    @Test
    fun returnsUnvoicedForLowVolume() {
        val result = detector.detect(sineFrame(frequencyHz = 440f, amplitude = 0.001f))

        assertFalse(result.isVoiced)
    }

    private fun sineFrame(frequencyHz: Float, amplitude: Float = 0.6f): AudioFrame {
        val samples = FloatArray(4_096) { index ->
            (sin(2.0 * PI * frequencyHz * index / SAMPLE_RATE) * amplitude).toFloat()
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
