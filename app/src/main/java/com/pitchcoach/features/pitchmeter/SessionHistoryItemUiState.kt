package com.pitchcoach.features.pitchmeter

import com.pitchcoach.core.music.NoteMapper
import com.pitchcoach.data.database.PracticeSessionEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class SessionHistoryItemUiState(
    val id: String,
    val startedAtText: String,
    val durationText: String,
    val averageAbsCentsText: String,
    val stabilityText: String,
    val rangeText: String,
    val voicedFramesText: String,
    val isPlaying: Boolean = false,
) {
    companion object {
        private val dateFormatter = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())

        fun fromEntity(
            entity: PracticeSessionEntity,
            isPlaying: Boolean = false,
        ): SessionHistoryItemUiState {
            val durationSeconds = ((entity.endedAt - entity.startedAt) / 1000L).coerceAtLeast(0L)
            val low = entity.noteRangeLow
            val high = entity.noteRangeHigh
            val rangeText = when {
                low != null && high != null ->
                    "${NoteMapper.noteNameWithOctave(low)}-${NoteMapper.noteNameWithOctave(high)}"
                else -> "--"
            }

            return SessionHistoryItemUiState(
                id = entity.id,
                startedAtText = dateFormatter.format(Date(entity.startedAt)),
                durationText = if (durationSeconds >= 60L) {
                    "${durationSeconds / 60L} 分 ${durationSeconds % 60L} 秒"
                } else {
                    "$durationSeconds 秒"
                },
                averageAbsCentsText = "%.0f cents".format(entity.averageAbsCents),
                stabilityText = "%.0f%%".format(entity.stabilityScore),
                rangeText = rangeText,
                voicedFramesText = "${entity.voicedFrameCount}/${entity.totalFrameCount}",
                isPlaying = isPlaying,
            )
        }
    }
}
