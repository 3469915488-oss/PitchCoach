package com.pitchcoach.core.session

data class PitchFrameSnapshot(
    val timestampMs: Long,
    val frequencyHz: Float?,
    val midi: Float?,
    val cents: Float?,
    val confidence: Float,
    val volumeRms: Float,
    val isVoiced: Boolean,
)
