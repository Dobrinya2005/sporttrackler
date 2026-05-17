package com.fitnesstrainer.app.ui.workout

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitnesstrainer.app.App
import com.fitnesstrainer.app.data.model.ExerciseRequest
import com.fitnesstrainer.app.data.model.WorkoutDayRequest
import com.fitnesstrainer.app.data.model.WorkoutPlanCreateRequest
import kotlinx.coroutines.launch

sealed class CreatePlanState {
    object Idle : CreatePlanState()
    object Loading : CreatePlanState()
    object Success : CreatePlanState()
    data class Error(val message: String) : CreatePlanState()
}

class CreateWorkoutPlanViewModel : ViewModel() {

    private val api = App.instance.apiService

    private val _state = MutableLiveData<CreatePlanState>(CreatePlanState.Idle)
    val state: LiveData<CreatePlanState> = _state

    fun createPlan(
        clientId: Int,
        title: String,
        description: String?,
        startDate: String?,
        endDate: String?,
        days: List<WorkoutDayRequest>
    ) {
        if (title.isBlank()) {
            _state.value = CreatePlanState.Error("Укажите название плана")
            return
        }
        if (days.isEmpty()) {
            _state.value = CreatePlanState.Error("Добавьте хотя бы один день тренировки")
            return
        }

        viewModelScope.launch {
            _state.value = CreatePlanState.Loading
            try {
                val request = WorkoutPlanCreateRequest(
                    clientId    = clientId,
                    title       = title.trim(),
                    description = description?.trim()?.ifBlank { null },
                    startDate   = startDate,
                    endDate     = endDate,
                    days        = days
                )
                val response = api.createWorkoutPlan(request)
                if (response.isSuccessful) {
                    _state.value = CreatePlanState.Success
                } else {
                    _state.value = CreatePlanState.Error("Ошибка сохранения: ${response.code()}")
                }
            } catch (e: Exception) {
                _state.value = CreatePlanState.Error(e.message ?: "Ошибка соединения")
            }
        }
    }
}
