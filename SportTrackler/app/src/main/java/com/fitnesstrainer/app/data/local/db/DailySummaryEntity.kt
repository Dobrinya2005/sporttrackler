package com.fitnesstrainer.app.data.local.db

import androidx.room.*

@Entity(tableName = "daily_summary", primaryKeys = ["userId", "date"])
data class DailySummaryEntity(
    val userId: Int,
    val date: String,
    val totalCalories: Double,
    val totalProtein: Double,
    val totalFat: Double,
    val totalCarbs: Double,
    val calorieGoal: Int?
)

@Dao
interface DailySummaryDao {
    @Query("SELECT * FROM daily_summary WHERE userId = :userId AND date = :date LIMIT 1")
    suspend fun get(userId: Int, date: String): DailySummaryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: DailySummaryEntity)
}
