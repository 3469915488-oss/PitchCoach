package com.pitchcoach.core.audio

import android.media.MediaRecorder
import android.os.Build

object AudioSourceSelector {
    fun defaultSources(): List<Int> = buildList {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            add(MediaRecorder.AudioSource.UNPROCESSED)
        }
        add(MediaRecorder.AudioSource.MIC)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            add(MediaRecorder.AudioSource.VOICE_PERFORMANCE)
        }
    }
}
