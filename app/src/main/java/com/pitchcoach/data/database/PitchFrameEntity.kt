package com.pitchcoach.data.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "pitch_frames",
    foreignKeys = [
        ForeignKey(
            entity = PracticeSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [
        Index("sessionId"),
        Index(value = ["sessionId", "timestampMs"]),
    ],
)
data class PitchFrameEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val timestampMs: Long,
    val frequencyHz: Float?,
    val midi: Float?,
    val cents: Float?,
    val confidence: Float,
    val volumeRms: Float,
    val isVoiced: Boolean,
)
