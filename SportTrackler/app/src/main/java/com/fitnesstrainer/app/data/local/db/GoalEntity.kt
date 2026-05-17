package com.fitnesstrainer.app.data.local.db

import androidx.room.*

enum class GoalType {
    WEIGHT, BODY_FAT, CHEST, WAIST, HIPS, BICEP
}

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey val type: String,
    val userId: Int,
    val targetValue: Double,
    val createdAt: String
)

@Dao
interface GoalDao {
    @Query("SELECT * FROM goals WHERE userId = :userId")
    suspend fun getAll(userId: Int): List<GoalEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(goal: GoalEntity)

    @Query("DELETE FROM goals WHERE userId = :userId AND type = :type")
    suspend fun delete(userId: Int, type: String)
}
