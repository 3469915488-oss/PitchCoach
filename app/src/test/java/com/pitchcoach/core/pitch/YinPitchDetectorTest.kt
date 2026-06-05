package com.pitchcoach.core.pitch

import com.pitchcoach.core.audio.AudioFrame
import com.pitchcoach.core.audio.VolumeAnalyzer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

class YinPitchDetectorTest {
    private val detector = YinPitchDetector()

    @Test
    fun detectsA4SineWave() {
        val frame = sineFrame(frequencyHz = 440f)

        val result = detector.detect(frame)

        assertTrue(result.isVoiced)
        assertEquals(440f, result.frequencyHz!!, 2f)
        assertTrue(result.confidence > 0.8f)
    }

    @Test
    fun returnsUnvoicedForSilence() {
        val samples = FloatArray(4_096)
        val frame = AudioFrame(
            samples = samples,
            sampleRate = SAMPLE_RATE,
            timestampMs = 0L,
            rms = VolumeAnalyzer.rms(samples),
        )

        val result = detector.detect(frame)

        assertFalse(result.isVoiced)
    }

    @Test
    fun separatesC4AndC5Octaves() {
        val c4 = detector.detect(sineFrame(frequencyHz = 261.63f))
        val c5 = detector.detect(sineFrame(frequencyHz = 523.25f))

        assertTrue(c4.isVoiced)
        assertTrue(c5.isVoiced)
        assertEquals(261.63f, c4.frequencyHz!!, 3f)
        assertEquals(523.25f, c5.frequencyHz!!, 3f)
    }

    @Test
    fun detectsC6AbovePreviousOneKilohertzLimit() {
        val result = detector.detect(sineFrame(frequencyHz = 1046.5f))

        assertTrue(result.isVoiced)
        assertEquals(1046.5f, result.frequencyHz!!, 8f)
    }

    @Test
    fun detectsLowGuitarStringRange() {
        val result = detector.detect(sineFrame(frequencyHz = 82.41f))

        assertTrue(result.isVoiced)
        assertEquals(82.41f, result.frequencyHz!!, 2f)
    }

    @Test
    fun correctsLikelyOctaveDoublingForWeakFundamentalC4() {
        val frame = harmonicFrame(
            fundamentalHz = 261.63f,
            fundamentalAmplitude = 0.05f,
            secondHarmonicAmplitude = 0.6f,
            thirdHarmonicAmplitude = 0.1f,
        )

        val result = detector.detect(frame)

        assertTrue(result.isVoiced)
        assertEquals(261.63f, result.frequencyHz!!, 4f)
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

    private fun harmonicFrame(
        fundamentalHz: Float,
        fundamentalAmplitude: Float,
        secondHarmonicAmplitude: Float,
        thirdHarmonicAmplitude: Float,
    ): AudioFrame {
        val samples = FloatArray(4_096) { index ->
            (
                sin(2.0 * PI * fundamentalHz * index / SAMPLE_RATE) * fundamentalAmplitude +
                    sin(2.0 * PI * fundamentalHz * 2.0 * index / SAMPLE_RATE) * secondHarmonicAmplitude +
                    sin(2.0 * PI * fundamentalHz * 3.0 * index / SAMPLE_RATE) * thirdHarmonicAmplitude
                ).toFloat()
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
