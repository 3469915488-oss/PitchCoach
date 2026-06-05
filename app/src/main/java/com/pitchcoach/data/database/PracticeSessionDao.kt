package com.pitchcoach.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PracticeSessionDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSession(session: PracticeSessionEntity)

    @Update
    suspend fun updateSession(session: PracticeSessionEntity)

    @Query("SELECT * FROM practice_sessions WHERE id = :sessionId LIMIT 1")
    suspend fun getSession(sessionId: String): PracticeSessionEntity?

    @Query("SELECT * FROM practice_sessions ORDER BY startedAt DESC")
    fun observeSessions(): Flow<List<PracticeSessionEntity>>

    @Query("DELETE FROM practice_sessions WHERE id = :sessionId")
    suspend fun deleteSession(sessionId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFrames(frames: List<PitchFrameEntity>)

    @Query("SELECT * FROM pitch_frames WHERE sessionId = :sessionId ORDER BY timestampMs ASC")
    suspend fun getFramesForSession(sessionId: String): List<PitchFrameEntity>

    @Query("DELETE FROM note_events WHERE sessionId = :sessionId")
    suspend fun deleteNoteEventsForSession(sessionId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNoteEvents(events: List<NoteEventEntity>)

    @Query("SELECT * FROM note_events WHERE sessionId = :sessionId ORDER BY id ASC")
    suspend fun getNoteEventsForSession(sessionId: String): List<NoteEventEntity>
}
