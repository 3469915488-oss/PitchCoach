package com.pitchcoach.core.music

data class NoteEstimate(
    val noteName: String,
    val octave: Int,
    val midi: Int,
    val cents: Float,
    val frequencyHz: Float,
)
