package com.pitchcoach.core.pitch

import com.pitchcoach.core.music.NoteEstimate

data class PitchAnalysisState(
    val timestampMs: Long,
    val frequencyHz: Float?,
    val note: NoteEstimate?,
    val confidence: Float,
    val volumeRms: Float,
    val isSilent: Boolean,
    val isVoiced: Boolean,
    val direction: PitchDirection,
    val stabilityScore: Float,
)

enum class PitchDirection {
    SILENT,
    UNKNOWN,
    FLAT,
    SHARP,
    IN_TUNE,
}
