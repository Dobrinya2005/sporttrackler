package com.fitnesstrainer.app.ui.goals

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitnesstrainer.app.App
import com.fitnesstrainer.app.data.local.db.GoalEntity
import com.fitnesstrainer.app.data.local.db.GoalType
import kotlinx.coroutines.launch
import java.time.LocalDate

data class GoalProgress(
    val type: GoalType,
    val label: String,
    val unit: String,
    val target: Double,
    val current: Double?,
    val startValue: Double = 0.0
) {
    // Направление: true = цель на увеличение (набор), false = на уменьшение (сброс)
    private val isIncreasing: Boolean get() = target > startValue

    val percent: Int get() {
        if (current == null || startValue == target) return 0
        return if (isIncreasing) {
            ((current - startValue) / (target - startValue) * 100).toInt().coerceIn(0, 100)
        } else {
            ((startValue - current) / (startValue - target) * 100).toInt().coerceIn(0, 100)
        }
    }

    val isDone: Boolean get() = current != null &&
        if (isIncreasing) current >= target else current <= target
}

class GoalsViewModel : ViewModel() {

    private val dao           = App.instance.database.goalDao()
    private val measureDao    = App.instance.database.measurementDao()
    private val tokenStorage  = App.instance.tokenStorage

    private val _goals = MutableLiveData<List<GoalProgress>>()
    val goals: LiveData<List<GoalProgress>> = _goals

    private var userId = -1

    fun load() {
        viewModelScope.launch {
            userId = tokenStorage.getUserId()
            val stored    = dao.getAll(userId)
            val latest    = measureDao.getAll(userId).firstOrNull()
            _goals.value  = stored.map { entity ->
                val type = GoalType.valueOf(entity.type)
                val cur  = latest?.let { m -> when (type) {
                    GoalType.WEIGHT    -> m.weightKg
                    GoalType.BODY_FAT  -> m.bodyFatPercent
                    GoalType.CHEST     -> m.chestCm
                    GoalType.WAIST     -> m.waistCm
                    GoalType.HIPS      -> m.hipsCm
                    GoalType.BICEP     -> m.bicepCm
                }}
                GoalProgress(
                    type       = type,
                    label      = labelFor(type),
                    unit       = unitFor(type),
                    target     = entity.targetValue,
                    current    = cur,
                    startValue = entity.startValue
                )
            }
        }
    }

    fun setGoal(type: GoalType, value: Double) {
        viewModelScope.launch {
            val latest = measureDao.getAll(userId).firstOrNull()
            val currentValue = latest?.let { m -> when (type) {
                GoalType.WEIGHT   -> m.weightKg
                GoalType.BODY_FAT -> m.bodyFatPercent
                GoalType.CHEST    -> m.chestCm
                GoalType.WAIST    -> m.waistCm
                GoalType.HIPS     -> m.hipsCm
                GoalType.BICEP    -> m.bicepCm
            }} ?: 0.0
            dao.insert(GoalEntity(
                type        = type.name,
                userId      = userId,
                targetValue = value,
                createdAt   = LocalDate.now().toString(),
                startValue  = currentValue
            ))
            load()
        }
    }

    fun deleteGoal(type: GoalType) {
        viewModelScope.launch {
            dao.delete(userId, type.name)
            load()
        }
    }

    private fun labelFor(type: GoalType) = when (type) {
        GoalType.WEIGHT   -> "Вес"
        GoalType.BODY_FAT -> "% жира"
        GoalType.CHEST    -> "Грудь"
        GoalType.WAIST    -> "Талия"
        GoalType.HIPS     -> "Бёдра"
        GoalType.BICEP    -> "Бицепс"
    }

    private fun unitFor(type: GoalType) = when (type) {
        GoalType.BODY_FAT -> "%"
        else              -> "см".takeIf { type != GoalType.WEIGHT } ?: "кг"
    }
}
