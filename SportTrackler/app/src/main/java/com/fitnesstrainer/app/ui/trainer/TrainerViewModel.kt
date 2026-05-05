package com.fitnesstrainer.app.ui.trainer

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitnesstrainer.app.App
import com.fitnesstrainer.app.data.model.ClientProfile
import kotlinx.coroutines.launch

sealed class TrainerState {
    object Loading : TrainerState()
    data class Success(val clients: List<ClientProfile>) : TrainerState()
    data class Error(val message: String) : TrainerState()
}

class TrainerViewModel : ViewModel() {

    private val api          = App.instance.apiService
    private val tokenStorage = App.instance.tokenStorage

    private val _state = MutableLiveData<TrainerState>()
    val state: LiveData<TrainerState> = _state

    private val _trainerName = MutableLiveData<String>()
    val trainerName: LiveData<String> = _trainerName

    init {
        loadName()
        loadClients()
    }

    private fun loadName() {
        viewModelScope.launch {
            val first = tokenStorage.getFirstName() ?: ""
            val last  = tokenStorage.getLastName()  ?: ""
            _trainerName.value = "$first $last"
        }
    }

    fun loadClients() {
        viewModelScope.launch {
            _state.value = TrainerState.Loading
            try {
                val response = api.getMyClients()
                if (response.isSuccessful) {
                    _state.value = TrainerState.Success(response.body() ?: emptyList())
                } else {
                    _state.value = TrainerState.Error("Ошибка загрузки клиентов")
                }
            } catch (e: Exception) {
                _state.value = TrainerState.Error("Нет подключения к серверу")
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            try { api.logout() } catch (_: Exception) {}
            tokenStorage.clearAuth()
        }
    }
}
