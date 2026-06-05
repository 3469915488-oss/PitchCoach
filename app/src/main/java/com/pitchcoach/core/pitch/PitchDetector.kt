package com.pitchcoach.core.pitch

import com.pitchcoach.core.audio.AudioFrame

interface PitchDetector {
    fun detect(frame: AudioFrame): PitchResult

    fun reset() = Unit
}
