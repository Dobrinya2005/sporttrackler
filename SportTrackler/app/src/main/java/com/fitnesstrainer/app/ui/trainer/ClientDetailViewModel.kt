package com.fitnesstrainer.app.ui.trainer

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitnesstrainer.app.App
import com.fitnesstrainer.app.data.model.MeasurementResponse
import kotlinx.coroutines.launch

sealed class ClientDetailState {
    object Loading : ClientDetailState()
    data class Success(val latest: MeasurementResponse?) : ClientDetailState()
    data class Error(val message: String) : ClientDetailState()
}

class ClientDetailViewModel : ViewModel() {

    private val api = App.instance.apiService

    private val _state = MutableLiveData<ClientDetailState>()
    val state: LiveData<ClientDetailState> = _state

    fun loadLatest(clientId: Int) {
        viewModelScope.launch {
            _state.value = ClientDetailState.Loading
            try {
                val response = api.getLatestMeasurement(clientId)
                _state.value = if (response.isSuccessful)
                    ClientDetailState.Success(response.body())
                else
                    ClientDetailState.Success(null)
            } catch (e: Exception) {
                _state.value = ClientDetailState.Error("Нет подключения к серверу")
            }
        }
    }
}
