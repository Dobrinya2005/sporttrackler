package com.fitnesstrainer.app.ui.trainer

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitnesstrainer.app.App
import com.fitnesstrainer.app.data.model.ClientProfile
import com.fitnesstrainer.app.util.toUserMessage
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class TrainerStats(
    val clientCount: Int,
    val averageRating: Double?,
    val reviewCount: Int
)

sealed class TrainerState {
    object Loading : TrainerState()
    data class Success(val clients: List<ClientProfile>) : TrainerState()
    data class Error(val message: String) : TrainerState()
}

class TrainerViewModel : ViewModel() {

    private val api            = App.instance.apiService
    private val tokenStorage   = App.instance.tokenStorage
    private val networkMonitor = App.instance.networkMonitor

    private val _state = MutableLiveData<TrainerState>()
    val state: LiveData<TrainerState> = _state

    private val _trainerName = MutableLiveData<String>()
    val trainerName: LiveData<String> = _trainerName

    private val _stats = MutableLiveData<TrainerStats>()
    val stats: LiveData<TrainerStats> = _stats

    init {
        loadName()
        loadClients()
        loadStats()
        viewModelScope.launch {
            networkMonitor.isOnline.collectLatest { online ->
                if (online && _state.value is TrainerState.Error) {
                    loadClients()
                    loadStats()
                }
            }
        }
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
                    val clients = response.body() ?: emptyList()
                    _state.value = TrainerState.Success(clients)
                    _stats.value = _stats.value?.copy(clientCount = clients.size)
                        ?: TrainerStats(clients.size, null, 0)
                } else {
                    _state.value = TrainerState.Error("Ошибка загрузки клиентов")
                }
            } catch (e: Exception) {
                _state.value = TrainerState.Error(e.toUserMessage())
            }
        }
    }

    private fun loadStats() {
        viewModelScope.launch {
            try {
                val trainerId = tokenStorage.getUserId()
                val resp = api.getTrainerReviews(trainerId)
                if (resp.isSuccessful) {
                    val body = resp.body()
                    val count = body?.reviews?.size ?: 0
                    val avg   = body?.averageRating
                    _stats.value = _stats.value?.copy(averageRating = avg, reviewCount = count)
                        ?: TrainerStats(0, avg, count)
                }
            } catch (_: Exception) {}
        }
    }

    fun logout() {
        viewModelScope.launch {
            try { api.logout() } catch (_: Exception) {}
            tokenStorage.clearAuth()
            tokenStorage.clearPin()
        }
    }
}

