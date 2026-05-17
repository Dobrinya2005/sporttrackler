package com.fitnesstrainer.app.ui.workout

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitnesstrainer.app.App
import com.fitnesstrainer.app.data.model.WorkoutPlan
import com.fitnesstrainer.app.util.toUserMessage
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

sealed class WorkoutState {
    object Loading : WorkoutState()
    data class Success(val plans: List<WorkoutPlan>) : WorkoutState()
    data class Error(val message: String) : WorkoutState()
}

class WorkoutPlanViewModel : ViewModel() {

    private val api            = App.instance.apiService
    private val tokenStorage   = App.instance.tokenStorage
    private val networkMonitor = App.instance.networkMonitor

    private val _state = MutableLiveData<WorkoutState>()
    val state: LiveData<WorkoutState> = _state

    private var targetClientId = -1

    init {
        viewModelScope.launch {
            networkMonitor.isOnline.collectLatest { online ->
                if (online && _state.value is WorkoutState.Error) load()
            }
        }
    }

    fun init(clientId: Int) {
        viewModelScope.launch {
            val myId = tokenStorage.getUserId()
            targetClientId = if (clientId == -1) myId else clientId
            load()
        }
    }

    fun load() {
        if (targetClientId == -1) return
        viewModelScope.launch {
            _state.value = WorkoutState.Loading
            try {
                val response = api.getWorkoutPlans(targetClientId)
                if (response.isSuccessful) {
                    _state.value = WorkoutState.Success(response.body() ?: emptyList())
                } else {
                    _state.value = WorkoutState.Error("Ошибка загрузки плана")
                }
            } catch (e: Exception) {
                _state.value = WorkoutState.Error(e.toUserMessage())
            }
        }
    }
}
