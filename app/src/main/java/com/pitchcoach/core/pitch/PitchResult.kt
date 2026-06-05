package com.pitchcoach.core.pitch

data class PitchResult(
    val frequencyHz: Float?,
    val confidence: Float,
    val volumeRms: Float,
    val isVoiced: Boolean,
    val timestampMs: Long,
)
