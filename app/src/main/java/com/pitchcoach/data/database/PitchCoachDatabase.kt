package com.pitchcoach.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        PracticeSessionEntity::class,
        PitchFrameEntity::class,
        NoteEventEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class PitchCoachDatabase : RoomDatabase() {
    abstract fun practiceSessionDao(): PracticeSessionDao

    companion object {
        fun create(context: Context): PitchCoachDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                PitchCoachDatabase::class.java,
                "pitchcoach.db",
            ).build()
        }
    }
}
