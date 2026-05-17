package com.fitnesstrainer.app.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [MeasurementEntity::class, DailySummaryEntity::class, GoalEntity::class, StepEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun measurementDao(): MeasurementDao
    abstract fun dailySummaryDao(): DailySummaryDao
    abstract fun goalDao(): GoalDao
    abstract fun stepDao(): StepDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null
        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "sporttrackler.db"
                ).build().also { INSTANCE = it }
            }
    }
}
