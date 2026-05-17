package com.fitnesstrainer.app.data.local.db

import androidx.room.*

@Entity(tableName = "steps", primaryKeys = ["userId", "date"])
data class StepEntity(
    val userId: Int,
    val date: String,
    val steps: Int
)

@Dao
interface StepDao {
    @Query("SELECT * FROM steps WHERE userId = :userId AND date = :date LIMIT 1")
    suspend fun get(userId: Int, date: String): StepEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: StepEntity)
}
