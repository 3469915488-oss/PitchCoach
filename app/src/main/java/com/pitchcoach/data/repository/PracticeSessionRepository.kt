package com.pitchcoach.data.repository

import com.pitchcoach.core.pitch.PitchAnalysisState
import com.pitchcoach.core.audio.AudioFrame
import com.pitchcoach.core.session.PitchFrameSnapshot
import com.pitchcoach.core.session.PracticeSessionSummary
import com.pitchcoach.data.database.NoteEventEntity
import com.pitchcoach.data.database.PracticeSessionEntity
import kotlinx.coroutines.flow.Flow

interface PracticeSessionRepository {
    suspend fun startSession(type: String): String

    suspend fun appendFrames(sessionId: String, frames: List<PitchFrameSnapshot>)

    suspend fun appendAnalysisFrame(sessionId: String, state: PitchAnalysisState)

    suspend fun appendAudioFrame(sessionId: String, frame: AudioFrame)

    suspend fun finishSession(sessionId: String): PracticeSessionSummary

    fun observeSessions(): Flow<List<PracticeSessionEntity>>

    suspend fun deleteSession(sessionId: String)

    suspend fun getAudioFilePath(sessionId: String): String?

    suspend fun getFrames(sessionId: String): List<PitchFrameSnapshot>

    suspend fun getNoteEvents(sessionId: String): List<NoteEventEntity>
}
