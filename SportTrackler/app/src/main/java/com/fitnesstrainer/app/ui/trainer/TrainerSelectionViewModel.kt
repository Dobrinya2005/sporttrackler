package com.fitnesstrainer.app.ui.trainer

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitnesstrainer.app.App
import com.fitnesstrainer.app.data.model.TrainerListItem
import com.fitnesstrainer.app.util.toUserMessage
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

sealed class TrainerSelectionState {
    object Loading : TrainerSelectionState()
    data class Success(val trainers: List<TrainerListItem>) : TrainerSelectionState()
    data class Error(val message: String) : TrainerSelectionState()
}

sealed class SelectResult {
    object Idle    : SelectResult()
    object Success : SelectResult()
    data class Error(val message: String) : SelectResult()
}

class TrainerSelectionViewModel : ViewModel() {

    private val api            = App.instance.apiService
    private val networkMonitor = App.instance.networkMonitor

    private val _state = MutableLiveData<TrainerSelectionState>()
    val state: LiveData<TrainerSelectionState> = _state

    private val _selectResult = MutableLiveData<SelectResult>(SelectResult.Idle)
    val selectResult: LiveData<SelectResult> = _selectResult

    init {
        loadTrainers()
        viewModelScope.launch {
            networkMonitor.isOnline.collectLatest { online ->
                if (online && _state.value is TrainerSelectionState.Error) loadTrainers()
            }
        }
    }

    fun loadTrainers() {
        viewModelScope.launch {
            _state.value = TrainerSelectionState.Loading
            try {
                val response = api.getTrainerList()
                _state.value = if (response.isSuccessful)
                    TrainerSelectionState.Success(response.body() ?: emptyList())
                else
                    TrainerSelectionState.Error("Ошибка загрузки тренеров")
            } catch (e: Exception) {
                _state.value = TrainerSelectionState.Error(e.toUserMessage())
            }
        }
    }

    fun selectTrainer(trainerId: Int) {
        viewModelScope.launch {
            try {
                val response = api.selectTrainer(trainerId)
                _selectResult.value = if (response.isSuccessful)
                    SelectResult.Success
                else
                    SelectResult.Error("Не удалось выбрать тренера")
            } catch (e: Exception) {
                _selectResult.value = SelectResult.Error(e.toUserMessage())
            }
        }
    }

    fun selectTrainerByCode(code: String) {
        viewModelScope.launch {
            try {
                val response = api.selectTrainerByCode(code.trim().uppercase())
                _selectResult.value = if (response.isSuccessful)
                    SelectResult.Success
                else
                    SelectResult.Error("Тренер с таким кодом не найден")
            } catch (e: Exception) {
                _selectResult.value = SelectResult.Error(e.toUserMessage())
            }
        }
    }

    fun clearSelectResult() { _selectResult.value = SelectResult.Idle }
}
