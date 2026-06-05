package com.pitchcoach.core.session

data class PracticeSessionSummary(
    val sessionId: String,
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
