package com.pitchcoach.core.session

data class NoteEventSnapshot(
    val targetMidi: Int?,
    val actualAvgMidi: Float?,
    val avgCents: Float,
    val maxAbsCents: Float,
    val durationMs: Long,
    val stableDurationMs: Long,
    val attackCents: Float?,
    val sustainCents: Float?,
    val releaseCents: Float?,
    val problemTags: List<String>,
)
