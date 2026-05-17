package com.fitnesstrainer.app.data.local.db

import androidx.room.*
import com.fitnesstrainer.app.data.model.MeasurementResponse

@Entity(tableName = "measurements")
data class MeasurementEntity(
    @PrimaryKey val id: Int,
    val userId: Int,
    val measuredAt: String,
    val weightKg: Double?,
    val heightCm: Double?,
    val chestCm: Double?,
    val waistCm: Double?,
    val hipsCm: Double?,
    val neckCm: Double?,
    val bicepCm: Double?,
    val forearmCm: Double?,
    val thighCm: Double?,
    val calfCm: Double?,
    val bodyFatPercent: Double?,
    val muscleMassKg: Double?,
    val bmi: Double?,
    val notes: String?,
    val createdAt: String
)

fun MeasurementResponse.toEntity(userId: Int) = MeasurementEntity(
    id             = measurementId,
    userId         = userId,
    measuredAt     = measuredAt,
    weightKg       = weightKg,
    heightCm       = heightCm,
    chestCm        = chestCm,
    waistCm        = waistCm,
    hipsCm         = hipsCm,
    neckCm         = neckCm,
    bicepCm        = bicepCm,
    forearmCm      = forearmCm,
    thighCm        = thighCm,
    calfCm         = calfCm,
    bodyFatPercent = bodyFatPercent,
    muscleMassKg   = muscleMassKg,
    bmi            = bmi,
    notes          = notes,
    createdAt      = createdAt
)

fun MeasurementEntity.toResponse() = MeasurementResponse(
    measurementId  = id,
    clientId       = userId,
    measuredAt     = measuredAt,
    weightKg       = weightKg,
    heightCm       = heightCm,
    chestCm        = chestCm,
    waistCm        = waistCm,
    hipsCm         = hipsCm,
    neckCm         = neckCm,
    bicepCm        = bicepCm,
    forearmCm      = forearmCm,
    thighCm        = thighCm,
    calfCm         = calfCm,
    bodyFatPercent = bodyFatPercent,
    muscleMassKg   = muscleMassKg,
    bmi            = bmi,
    notes          = notes,
    createdAt      = createdAt
)

@Dao
interface MeasurementDao {
    @Query("SELECT * FROM measurements WHERE userId = :userId ORDER BY measuredAt DESC")
    suspend fun getAll(userId: Int): List<MeasurementEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<MeasurementEntity>)

    @Query("DELETE FROM measurements WHERE userId = :userId")
    suspend fun deleteAll(userId: Int)
}
