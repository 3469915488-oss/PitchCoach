package com.pitchcoach.core.audio

import android.Manifest
import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.os.Build
import android.os.SystemClock
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.max

class AudioRecordAudioRecorder(
    private val sampleRate: Int = DEFAULT_SAMPLE_RATE,
    private val frameSizeInSamples: Int = DEFAULT_FRAME_SIZE,
    private val audioSources: List<Int> = AudioSourceSelector.defaultSources(),
) : AudioRecorder {
    @Volatile
    private var activeRecord: AudioRecord? = null

    @Volatile
    private var shouldRecord: Boolean = false

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    override fun start(): Flow<AudioFrame> = callbackFlow {
        val producerScope = this
        val bufferSizeBytes = resolveBufferSizeBytes()
        val record = createAudioRecord(bufferSizeBytes)
        if (record == null) {
            close(IllegalStateException("Unable to initialize AudioRecord"))
            return@callbackFlow
        }

        activeRecord = record
        shouldRecord = true

        val readJob = launch(Dispatchers.IO) {
            val buffer = ShortArray(frameSizeInSamples)
            try {
                record.startRecording()
                while (isActive && shouldRecord) {
                    val readCount = read(record, buffer)
                    if (readCount > 0) {
                        val samples = FloatArray(readCount) { index ->
                            (buffer[index] / SHORT_NORMALIZATION).coerceIn(-1f, 1f)
                        }
                        trySend(
                            AudioFrame(
                                samples = samples,
                                sampleRate = sampleRate,
                                timestampMs = SystemClock.elapsedRealtime(),
                                rms = VolumeAnalyzer.rms(samples),
                            )
                        )
                    } else if (readCount == AudioRecord.ERROR_INVALID_OPERATION) {
                        producerScope.close(IllegalStateException("AudioRecord read failed: invalid operation"))
                    }
                }
            } finally {
                release(record)
            }
        }

        awaitClose {
            shouldRecord = false
            readJob.cancel()
            release(record)
        }
    }

    override fun stop() {
        shouldRecord = false
        activeRecord?.let(::release)
    }

    private fun resolveBufferSizeBytes(): Int {
        val minBufferSize = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        val frameBytes = frameSizeInSamples * BYTES_PER_PCM_16_SAMPLE
        return max(minBufferSize, frameBytes * 2)
    }

    @SuppressLint("MissingPermission")
    private fun createAudioRecord(bufferSizeBytes: Int): AudioRecord? {
        for (source in audioSources) {
            val record = runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    AudioRecord.Builder()
                        .setAudioSource(source)
                        .setAudioFormat(
                            AudioFormat.Builder()
                                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                                .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                                .setSampleRate(sampleRate)
                                .build()
                        )
                        .setBufferSizeInBytes(bufferSizeBytes)
                        .build()
                } else {
                    @Suppress("DEPRECATION")
                    AudioRecord(
                        source,
                        sampleRate,
                        AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        bufferSizeBytes,
                    )
                }
            }.getOrNull()

            if (record?.state == AudioRecord.STATE_INITIALIZED) {
                return record
            }
            record?.release()
        }
        return null
    }

    private fun read(record: AudioRecord, buffer: ShortArray): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            record.read(buffer, 0, buffer.size, AudioRecord.READ_BLOCKING)
        } else {
            record.read(buffer, 0, buffer.size)
        }
    }

    private fun release(record: AudioRecord) {
        if (activeRecord === record) {
            activeRecord = null
        }
        runCatching {
            if (record.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                record.stop()
            }
        }
        runCatching { record.release() }
    }

    companion object {
        const val DEFAULT_SAMPLE_RATE = 44_100
        const val DEFAULT_FRAME_SIZE = 2_048
        private const val BYTES_PER_PCM_16_SAMPLE = 2
        private const val SHORT_NORMALIZATION = 32768f
    }
}
