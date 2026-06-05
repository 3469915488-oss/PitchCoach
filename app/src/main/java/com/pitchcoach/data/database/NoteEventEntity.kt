package com.pitchcoach.data.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "note_events",
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
    ],
)
data class NoteEventEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val targetMidi: Int?,
    val actualAvgMidi: Float?,
    val avgCents: Float,
    val maxAbsCents: Float,
    val durationMs: Long,
    val stableDurationMs: Long,
    val attackCents: Float?,
    val sustainCents: Float?,
    val releaseCents: Float?,
    val problemTagsJson: String,
)
