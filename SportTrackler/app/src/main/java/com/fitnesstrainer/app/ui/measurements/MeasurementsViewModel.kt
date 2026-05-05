package com.fitnesstrainer.app.ui.measurements

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitnesstrainer.app.App
import com.fitnesstrainer.app.data.model.MeasurementRequest
import com.fitnesstrainer.app.data.model.MeasurementResponse
import kotlinx.coroutines.launch

sealed class MeasurementsState {
    object Loading : MeasurementsState()
    data class Success(val list: List<MeasurementResponse>) : MeasurementsState()
    data class Error(val message: String) : MeasurementsState()
}

class MeasurementsViewModel : ViewModel() {

    private val api          = App.instance.apiService
    private val tokenStorage = App.instance.tokenStorage

    private val _state = MutableLiveData<MeasurementsState>()
    val state: LiveData<MeasurementsState> = _state

    private val _saved = MutableLiveData<Boolean>()
    val saved: LiveData<Boolean> = _saved

    private var targetClientId = -1
    var isOwnData = true
        private set

    fun init(clientId: Int) {
        viewModelScope.launch {
            val myId = tokenStorage.getUserId()
            targetClientId = if (clientId == -1) myId else clientId
            isOwnData      = (clientId == -1 || clientId == myId)
            load()
        }
    }

    fun load() {
        if (targetClientId == -1) return
        viewModelScope.launch {
            _state.value = MeasurementsState.Loading
            try {
                val response = api.getMeasurementHistory(targetClientId)
                if (response.isSuccessful) {
                    _state.value = MeasurementsState.Success(response.body() ?: emptyList())
                } else {
                    _state.value = MeasurementsState.Error("Ошибка загрузки замеров")
                }
            } catch (e: Exception) {
                _state.value = MeasurementsState.Error("Нет подключения к серверу")
            }
        }
    }

    fun addMeasurement(request: MeasurementRequest) {
        viewModelScope.launch {
            try {
                val response = api.addMeasurement(request)
                if (response.isSuccessful) {
                    _saved.value = true
                    load()
                }
            } catch (e: Exception) {
                _state.value = MeasurementsState.Error("Ошибка сохранения")
            }
        }
    }

    fun delete(id: Int) {
        viewModelScope.launch {
            try {
                api.deleteMeasurement(id)
                load()
            } catch (_: Exception) {}
        }
    }
}
