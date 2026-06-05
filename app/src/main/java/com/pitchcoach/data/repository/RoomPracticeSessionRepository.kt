package com.pitchcoach.data.repository

import androidx.room.withTransaction
import com.pitchcoach.core.audio.AudioFrame
import com.pitchcoach.core.music.CentsCalculator
import com.pitchcoach.core.pitch.PitchAnalysisState
import com.pitchcoach.core.session.NoteEventBuilder
import com.pitchcoach.core.session.NoteEventSnapshot
import com.pitchcoach.core.session.PitchFrameSnapshot
import com.pitchcoach.core.session.PracticeSessionSummarizer
import com.pitchcoach.core.session.PracticeSessionSummary
import com.pitchcoach.data.database.NoteEventEntity
import com.pitchcoach.data.database.PitchCoachDatabase
import com.pitchcoach.data.database.PitchFrameEntity
import com.pitchcoach.data.database.PracticeSessionEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class RoomPracticeSessionRepository(
    private val database: PitchCoachDatabase,
    private val clock: () -> Long = { System.currentTimeMillis() },
) : PracticeSessionRepository {
    private val dao = database.practiceSessionDao()

    override suspend fun startSession(type: String): String {
        val id = UUID.randomUUID().toString()
        val now = clock()
        dao.insertSession(
            PracticeSessionEntity(
                id = id,
                type = type,
                startedAt = now,
                endedAt = now,
                averageAbsCents = 0f,
                flatRate = 0f,
                sharpRate = 0f,
                stabilityScore = 0f,
                noteRangeLow = null,
                noteRangeHigh = null,
                totalFrameCount = 0,
                voicedFrameCount = 0,
            )
        )
        return id
    }

    override suspend fun appendFrames(sessionId: String, frames: List<PitchFrameSnapshot>) {
        if (frames.isEmpty()) return
        dao.insertFrames(frames.map { it.toEntity(sessionId) })
    }

    override suspend fun appendAnalysisFrame(sessionId: String, state: PitchAnalysisState) {
        val note = state.note
        appendFrames(
            sessionId = sessionId,
            frames = listOf(
                PitchFrameSnapshot(
                    timestampMs = state.timestampMs,
                    frequencyHz = state.frequencyHz,
                    midi = note?.let { it.midi + it.cents / 100f },
                    cents = note?.cents,
                    confidence = state.confidence,
                    volumeRms = state.volumeRms,
                    isVoiced = state.isVoiced,
                )
            )
        )
    }

    override suspend fun appendAudioFrame(sessionId: String, frame: AudioFrame) = Unit

    override suspend fun finishSession(sessionId: String): PracticeSessionSummary {
        val session = dao.getSession(sessionId)
            ?: error("Practice session not found: $sessionId")
        val frames = getFrames(sessionId)
        val summary = PracticeSessionSummarizer.summarize(
            sessionId = sessionId,
            type = session.type,
            startedAt = session.startedAt,
            endedAt = clock(),
            frames = frames,
        )
        val events = NoteEventBuilder.build(frames).mapIndexed { index, event ->
            event.toEntity(sessionId = sessionId, index = index)
        }

        database.withTransaction {
            dao.updateSession(
                session.copy(
                    endedAt = summary.endedAt,
                    averageAbsCents = summary.averageAbsCents,
                    flatRate = summary.flatRate,
                    sharpRate = summary.sharpRate,
                    stabilityScore = summary.stabilityScore,
                    noteRangeLow = summary.noteRangeLow,
                    noteRangeHigh = summary.noteRangeHigh,
                    totalFrameCount = summary.totalFrameCount,
                    voicedFrameCount = summary.voicedFrameCount,
                )
            )
            dao.deleteNoteEventsForSession(sessionId)
            dao.insertNoteEvents(events)
        }

        return summary
    }

    override fun observeSessions(): Flow<List<PracticeSessionEntity>> = dao.observeSessions()

    override suspend fun deleteSession(sessionId: String) {
        dao.deleteSession(sessionId)
    }

    override suspend fun getAudioFilePath(sessionId: String): String? = null

    override suspend fun getFrames(sessionId: String): List<PitchFrameSnapshot> {
        return dao.getFramesForSession(sessionId).map { it.toSnapshot() }
    }

    override suspend fun getNoteEvents(sessionId: String): List<NoteEventEntity> {
        return dao.getNoteEventsForSession(sessionId)
    }

    private fun PitchFrameSnapshot.toEntity(sessionId: String): PitchFrameEntity {
        return PitchFrameEntity(
            id = UUID.randomUUID().toString(),
            sessionId = sessionId,
            timestampMs = timestampMs,
            frequencyHz = frequencyHz,
            midi = midi ?: frequencyHz?.let(CentsCalculator::frequencyToMidi),
            cents = cents,
            confidence = confidence,
            volumeRms = volumeRms,
            isVoiced = isVoiced,
        )
    }

    private fun PitchFrameEntity.toSnapshot(): PitchFrameSnapshot {
        return PitchFrameSnapshot(
            timestampMs = timestampMs,
            frequencyHz = frequencyHz,
            midi = midi,
            cents = cents,
            confidence = confidence,
            volumeRms = volumeRms,
            isVoiced = isVoiced,
        )
    }

    private fun NoteEventSnapshot.toEntity(sessionId: String, index: Int): NoteEventEntity {
        return NoteEventEntity(
            id = "${sessionId}_note_${index.toString().padStart(4, '0')}",
            sessionId = sessionId,
            targetMidi = targetMidi,
            actualAvgMidi = actualAvgMidi,
            avgCents = avgCents,
            maxAbsCents = maxAbsCents,
            durationMs = durationMs,
            stableDurationMs = stableDurationMs,
            attackCents = attackCents,
            sustainCents = sustainCents,
            releaseCents = releaseCents,
            problemTagsJson = problemTags.toJsonArrayString(),
        )
    }

    private fun List<String>.toJsonArrayString(): String {
        return joinToString(prefix = "[", postfix = "]") { value ->
            "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
        }
    }
}
