package com.pitchcoach.data.repository

import com.pitchcoach.core.audio.AudioFrame
import com.pitchcoach.core.session.PitchFrameSnapshot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class FilePracticeSessionRepositoryTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private var now = 1_000L

    @Test
    fun savesSessionAsFileAndBuildsSummaryFromFileFrames() = runTest {
        val repository = FilePracticeSessionRepository(temporaryFolder.newFolder("sessions")) { now }
        val sessionId = repository.startSession("pitch_meter")
        now = 3_400L

        repository.appendFrames(
            sessionId,
            listOf(
                frame(timestampMs = 0L, midi = 60f, cents = -30f),
                frame(timestampMs = 100L, midi = 60f, cents = -10f),
                frame(timestampMs = 200L, midi = 60f, cents = 10f),
                frame(timestampMs = 300L, midi = 61f, cents = 30f),
            ),
        )
        repository.appendAudioFrame(sessionId, audioFrame())

        val summary = repository.finishSession(sessionId)
        val sessions = repository.observeSessions().first()
        val frames = repository.getFrames(sessionId)
        val events = repository.getNoteEvents(sessionId)
        val audioFile = java.io.File(repository.getAudioFilePath(sessionId)!!)

        assertEquals(4, frames.size)
        assertEquals(20f, summary.averageAbsCents, 0.001f)
        assertEquals(4, summary.voicedFrameCount)
        assertEquals(1, sessions.size)
        assertEquals(summary.averageAbsCents, sessions.first().averageAbsCents, 0.001f)
        assertEquals(2, events.size)
        assertTrue(repository.sessionFilePath(sessionId).endsWith(".pitchcoach.csv"))
        assertTrue(audioFile.exists())
        assertTrue(audioFile.length() > 44L)
    }

    @Test
    fun deleteSessionRemovesSavedFileAndHistoryItem() = runTest {
        val repository = FilePracticeSessionRepository(temporaryFolder.newFolder("sessions")) { now }
        val sessionId = repository.startSession("pitch_meter")
        now = 2_000L

        repository.appendFrames(sessionId, listOf(frame(timestampMs = 0L, midi = 60f, cents = 0f)))
        repository.appendAudioFrame(sessionId, audioFrame())
        repository.finishSession(sessionId)
        val audioPath = repository.getAudioFilePath(sessionId)!!

        repository.deleteSession(sessionId)

        assertTrue(repository.observeSessions().first().isEmpty())
        assertTrue(!java.io.File(repository.sessionFilePath(sessionId)).exists())
        assertTrue(!java.io.File(audioPath).exists())
    }

    private fun frame(timestampMs: Long, midi: Float, cents: Float): PitchFrameSnapshot {
        return PitchFrameSnapshot(
            timestampMs = timestampMs,
            frequencyHz = 440f,
            midi = midi,
            cents = cents,
            confidence = 0.9f,
            volumeRms = 0.1f,
            isVoiced = true,
        )
    }

    private fun audioFrame(): AudioFrame {
        return AudioFrame(
            samples = FloatArray(512) { index -> if (index % 2 == 0) 0.2f else -0.2f },
            sampleRate = 44_100,
            timestampMs = 0L,
            rms = 0.2f,
        )
    }
}
