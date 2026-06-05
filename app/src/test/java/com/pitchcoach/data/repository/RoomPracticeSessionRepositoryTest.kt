package com.pitchcoach.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.pitchcoach.core.session.PitchFrameSnapshot
import com.pitchcoach.data.database.PitchCoachDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RoomPracticeSessionRepositoryTest {
    private lateinit var database: PitchCoachDatabase
    private lateinit var repository: RoomPracticeSessionRepository
    private var now = 1_000L

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, PitchCoachDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = RoomPracticeSessionRepository(database) { now }
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun persistsFramesAndFinishesSessionWithSummaryAndNoteEvents() = runTest {
        val sessionId = repository.startSession("pitch_meter")
        now = 2_000L

        repository.appendFrames(
            sessionId,
            listOf(
                frame(timestampMs = 0L, midi = 60f, cents = -40f),
                frame(timestampMs = 40L, midi = 60f, cents = -20f),
                frame(timestampMs = 80L, midi = 60f, cents = -10f),
                frame(timestampMs = 120L, midi = 61f, cents = 15f),
            ),
        )

        val summary = repository.finishSession(sessionId)
        val sessions = repository.observeSessions().first()
        val events = repository.getNoteEvents(sessionId)

        assertEquals(sessionId, summary.sessionId)
        assertEquals(4, summary.voicedFrameCount)
        assertEquals(21.25f, summary.averageAbsCents, 0.001f)
        assertEquals(1, sessions.size)
        assertEquals(summary.averageAbsCents, sessions.first().averageAbsCents, 0.001f)
        assertEquals(2, events.size)
        assertTrue(events.first().problemTagsJson.contains("attack_flat"))
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
}
