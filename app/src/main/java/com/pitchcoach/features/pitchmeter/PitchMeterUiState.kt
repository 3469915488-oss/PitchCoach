package com.pitchcoach.features.pitchmeter

import com.pitchcoach.core.pitch.PitchAnalysisState
import com.pitchcoach.core.pitch.PitchDirection

data class PitchMeterUiState(
    val noteLabel: String = "等待声音",
    val frequencyText: String = "-- Hz",
    val centsText: String = "-- cents",
    val centsValue: Float? = null,
    val directionText: String = "等待声音",
    val confidencePercent: Int = 0,
    val volumePercent: Int = 0,
    val stabilityPercent: Int = 0,
    val isListening: Boolean = false,
    val isPaused: Boolean = false,
    val isHoldingPitch: Boolean = false,
    val practiceMode: PitchPracticeMode = PitchPracticeMode.CHROMATIC,
    val inTuneRangeCents: Float = 10f,
    val selectedGuitarTuningId: String = "standard",
    val selectedGuitarTuningName: String = "标准音",
    val selectedGuitarTuningNotes: String = "E2 A2 D3 G3 B3 E4",
    val selectedGuitarStringNumber: Int = 6,
    val selectedGuitarStringLabel: String = "E2",
    val selectedGuitarStringFrequencyText: String = "82.4 Hz",
    val isReferenceTonePlaying: Boolean = false,
    val guitarTunings: List<GuitarTuningOptionUiState> = emptyList(),
    val guitarStrings: List<GuitarStringUiState> = emptyList(),
    val centsTrail: List<Float?> = emptyList(),
    val lastSummaryText: String? = null,
) {
    companion object {
        fun fromAnalysis(
            state: PitchAnalysisState,
            isListening: Boolean,
            isPaused: Boolean = false,
            isHoldingPitch: Boolean = false,
            practiceMode: PitchPracticeMode = PitchPracticeMode.CHROMATIC,
            inTuneRangeCents: Float = 10f,
            noteLabelOverride: String? = null,
            centsValueOverride: Float? = null,
            directionTextOverride: String? = null,
            selectedGuitarTuningId: String = "standard",
            selectedGuitarTuningName: String = "标准音",
            selectedGuitarTuningNotes: String = "E2 A2 D3 G3 B3 E4",
            selectedGuitarStringNumber: Int = 6,
            selectedGuitarStringLabel: String = "E2",
            selectedGuitarStringFrequencyText: String = "82.4 Hz",
            isReferenceTonePlaying: Boolean = false,
            guitarTunings: List<GuitarTuningOptionUiState> = emptyList(),
            guitarStrings: List<GuitarStringUiState> = emptyList(),
            centsTrail: List<Float?> = emptyList(),
        ): PitchMeterUiState {
            val note = state.note
            val noteLabel = when {
                state.isSilent -> "等待声音"
                note != null -> "${note.noteName}${note.octave}"
                else -> "--"
            }
            val centsValue = centsValueOverride ?: note?.cents

            return PitchMeterUiState(
                noteLabel = noteLabelOverride ?: noteLabel,
                frequencyText = state.frequencyHz?.let { "%.1f Hz".format(it) } ?: "-- Hz",
                centsText = centsValue?.let { "%+.0f cents".format(it) } ?: "-- cents",
                centsValue = centsValue,
                directionText = directionTextOverride ?: if (isHoldingPitch && note != null) {
                    "保持上一音"
                } else {
                    state.direction.toDisplayText()
                },
                confidencePercent = (state.confidence.coerceIn(0f, 1f) * 100f).toInt(),
                volumePercent = (state.volumeRms.coerceIn(0f, 1f) * 100f).toInt(),
                stabilityPercent = state.stabilityScore.coerceIn(0f, 100f).toInt(),
                isListening = isListening,
                isPaused = isPaused,
                isHoldingPitch = isHoldingPitch,
                practiceMode = practiceMode,
                inTuneRangeCents = inTuneRangeCents,
                selectedGuitarTuningId = selectedGuitarTuningId,
                selectedGuitarTuningName = selectedGuitarTuningName,
                selectedGuitarTuningNotes = selectedGuitarTuningNotes,
                selectedGuitarStringNumber = selectedGuitarStringNumber,
                selectedGuitarStringLabel = selectedGuitarStringLabel,
                selectedGuitarStringFrequencyText = selectedGuitarStringFrequencyText,
                isReferenceTonePlaying = isReferenceTonePlaying,
                guitarTunings = guitarTunings,
                guitarStrings = guitarStrings,
                centsTrail = centsTrail,
            )
        }

        private fun PitchDirection.toDisplayText(): String = when (this) {
            PitchDirection.SILENT -> "等待声音"
            PitchDirection.UNKNOWN -> "声音再清楚一点"
            PitchDirection.FLAT -> "稍微抬高一点"
            PitchDirection.SHARP -> "稍微放低一点"
            PitchDirection.IN_TUNE -> "已经很接近了"
        }
    }
}

data class GuitarTuningOptionUiState(
    val id: String,
    val name: String,
    val shortName: String,
)

data class GuitarStringUiState(
    val number: Int,
    val noteLabel: String,
    val frequencyText: String,
    val cents: Float?,
    val isActive: Boolean,
    val isSelected: Boolean,
)
