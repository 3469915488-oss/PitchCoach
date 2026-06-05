package com.pitchcoach.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "practice_sessions")
data class PracticeSessionEntity(
    @PrimaryKey val id: String,
    val type: String,
    val startedAt: Long,
    val endedAt: Long,
    val averageAbsCents: Float,
    val flatRate: Float,
    val sharpRate: Float,
    val stabilityScore: Float,
    val noteRangeLow: Int?,
    val noteRangeHigh: Int?,
    val totalFrameCount: Int,
    val voicedFrameCount: Int,
)
