package com.pitchcoach.features.pitchmeter

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import androidx.annotation.RawRes
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import java.io.ByteArrayOutputStream
import kotlin.math.abs

class GuitarReferenceTonePlayer(
    private val sample: GuitarReferenceSample,
    private val outputSampleRate: Int = OUTPUT_SAMPLE_RATE,
) {
    suspend fun play(frequencyHz: Float) {
        val minBufferSize = AudioTrack.getMinBufferSize(
            outputSampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        val bufferSize = maxOf(minBufferSize, BUFFER_SAMPLES * BYTES_PER_SAMPLE)
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build(),
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(outputSampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
            )
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        val renderer = GuitarSampleRenderer(
            sample = sample,
            targetFrequencyHz = frequencyHz,
            outputSampleRate = outputSampleRate,
        )
        val buffer = ShortArray(BUFFER_SAMPLES)
        try {
            track.play()
            while (currentCoroutineContext().isActive) {
                renderer.render(buffer)
                track.write(buffer, 0, buffer.size)
            }
        } finally {
            runCatching { track.stop() }
            track.release()
        }
    }

    private class GuitarSampleRenderer(
        private val sample: GuitarReferenceSample,
        private val targetFrequencyHz: Float,
        outputSampleRate: Int,
    ) {
        private val sourceStep = sample.sampleRate.toDouble() / outputSampleRate *
            targetFrequencyHz / sample.baseFrequencyHz
        private val gapSamples = (outputSampleRate * PLUCK_GAP_SECONDS).toInt()
        private var sourcePosition = 0.0
        private var gapPosition = 0

        fun render(output: ShortArray) {
            for (index in output.indices) {
                output[index] = nextSample()
            }
        }

        private fun nextSample(): Short {
            if (gapPosition > 0) {
                gapPosition -= 1
                return 0
            }

            if (sourcePosition >= sample.samples.lastIndex) {
                sourcePosition = 0.0
                gapPosition = gapSamples
                return 0
            }

            val current = sourcePosition.toInt()
            val fraction = sourcePosition - current
            val left = sample.samples[current]
            val right = sample.samples.getOrElse(current + 1) { left }
            sourcePosition += sourceStep

            val value = (left + (right - left) * fraction).toFloat()
            return (value.coerceIn(-1f, 1f) * Short.MAX_VALUE).toInt().toShort()
        }
    }

    companion object {
        private const val OUTPUT_SAMPLE_RATE = 44_100
        private const val BUFFER_SAMPLES = 1_024
        private const val BYTES_PER_SAMPLE = 2
        private const val PLUCK_GAP_SECONDS = 0.28
    }
}

data class GuitarReferenceSample(
    val samples: FloatArray,
    val sampleRate: Int,
    val baseFrequencyHz: Float,
) {
    companion object {
        fun fromRawResource(
            context: Context,
            @RawRes resourceId: Int,
            baseFrequencyHz: Float = 196f,
        ): GuitarReferenceSample {
            val bytes = context.resources.openRawResource(resourceId).use { input ->
                val output = ByteArrayOutputStream()
                input.copyTo(output)
                output.toByteArray()
            }
            return WavPcm16Mono.decode(bytes, baseFrequencyHz)
        }
    }
}

private object WavPcm16Mono {
    fun decode(bytes: ByteArray, baseFrequencyHz: Float): GuitarReferenceSample {
        require(bytes.size > 44) { "WAV file is too small." }
        require(String(bytes, 0, 4) == "RIFF") { "Expected RIFF WAV." }
        require(String(bytes, 8, 4) == "WAVE") { "Expected WAVE file." }

        var offset = 12
        var sampleRate = 0
        var channels = 0
        var bitsPerSample = 0
        var dataOffset = -1
        var dataSize = 0

        while (offset + 8 <= bytes.size) {
            val chunkId = String(bytes, offset, 4)
            val chunkSize = bytes.readIntLittleEndian(offset + 4)
            val chunkDataOffset = offset + 8
            when (chunkId) {
                "fmt " -> {
                    channels = bytes.readShortLittleEndian(chunkDataOffset + 2)
                    sampleRate = bytes.readIntLittleEndian(chunkDataOffset + 4)
                    bitsPerSample = bytes.readShortLittleEndian(chunkDataOffset + 14)
                }
                "data" -> {
                    dataOffset = chunkDataOffset
                    dataSize = chunkSize
                }
            }
            offset = chunkDataOffset + chunkSize + (chunkSize % 2)
        }

        require(sampleRate > 0) { "WAV sample rate missing." }
        require(channels == 1) { "Only mono WAV samples are supported." }
        require(bitsPerSample == 16) { "Only PCM 16-bit WAV samples are supported." }
        require(dataOffset >= 0 && dataSize > 0) { "WAV data chunk missing." }

        val sampleCount = dataSize / 2
        val full = FloatArray(sampleCount) { index ->
            bytes.readShortLittleEndian(dataOffset + index * 2) / 32768f
        }
        val start = findAttackStart(full, sampleRate)
        val end = minOf(full.size, start + (sampleRate * PLUCK_SECONDS).toInt())
        return GuitarReferenceSample(
            samples = full.copyOfRange(start, end).fadeEdges(sampleRate),
            sampleRate = sampleRate,
            baseFrequencyHz = baseFrequencyHz,
        )
    }

    private fun findAttackStart(samples: FloatArray, sampleRate: Int): Int {
        val threshold = 0.025f
        val firstLoud = samples.indexOfFirst { sample -> abs(sample) >= threshold }
            .takeIf { it >= 0 }
            ?: 0
        return (firstLoud - sampleRate / 80).coerceAtLeast(0)
    }

    private fun FloatArray.fadeEdges(sampleRate: Int): FloatArray {
        val fadeSamples = minOf(size / 10, sampleRate / 120)
        if (fadeSamples <= 1) return this
        for (index in 0 until fadeSamples) {
            val gain = index.toFloat() / fadeSamples
            this[index] *= gain
            this[lastIndex - index] *= gain
        }
        return this
    }

    private fun ByteArray.readShortLittleEndian(offset: Int): Int {
        val value = (this[offset].toInt() and 0xFF) or
            (this[offset + 1].toInt() shl 8)
        return value.toShort().toInt()
    }

    private fun ByteArray.readIntLittleEndian(offset: Int): Int {
        return (this[offset].toInt() and 0xFF) or
            ((this[offset + 1].toInt() and 0xFF) shl 8) or
            ((this[offset + 2].toInt() and 0xFF) shl 16) or
            ((this[offset + 3].toInt() and 0xFF) shl 24)
    }

    private const val PLUCK_SECONDS = 2.2
}
