package com.fitnesstrainer.app.ui.trainer

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitnesstrainer.app.App
import com.fitnesstrainer.app.data.model.MeasurementResponse
import com.fitnesstrainer.app.util.toUserMessage
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

sealed class ClientDetailState {
    object Loading : ClientDetailState()
    data class Success(val latest: MeasurementResponse?) : ClientDetailState()
    data class Error(val message: String) : ClientDetailState()
}

class ClientDetailViewModel : ViewModel() {

    private val api            = App.instance.apiService
    private val networkMonitor = App.instance.networkMonitor

    private val _state = MutableLiveData<ClientDetailState>()
    val state: LiveData<ClientDetailState> = _state

    private var lastClientId = -1

    init {
        viewModelScope.launch {
            networkMonitor.isOnline.collectLatest { online ->
                if (online && _state.value is ClientDetailState.Error && lastClientId != -1) {
                    loadLatest(lastClientId)
                }
            }
        }
    }

    fun loadLatest(clientId: Int) {
        lastClientId = clientId
        viewModelScope.launch {
            _state.value = ClientDetailState.Loading
            try {
                val response = api.getLatestMeasurement(clientId)
                _state.value = if (response.isSuccessful)
                    ClientDetailState.Success(response.body())
                else
                    ClientDetailState.Success(null)
            } catch (e: Exception) {
                _state.value = ClientDetailState.Error(e.toUserMessage())
            }
        }
    }
}
