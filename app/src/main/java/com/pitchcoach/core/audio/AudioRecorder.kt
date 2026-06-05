package com.pitchcoach.core.audio

import kotlinx.coroutines.flow.Flow

interface AudioRecorder {
    fun start(): Flow<AudioFrame>

    fun stop()
}
