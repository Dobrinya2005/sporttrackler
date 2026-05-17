package com.fitnesstrainer.app.ui.measurements

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitnesstrainer.app.App
import com.fitnesstrainer.app.data.local.db.toEntity
import com.fitnesstrainer.app.data.local.db.toResponse
import com.fitnesstrainer.app.data.model.MeasurementRequest
import com.fitnesstrainer.app.data.model.MeasurementResponse
import com.fitnesstrainer.app.util.toUserMessage
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

sealed class MeasurementsState {
    object Loading : MeasurementsState()
    data class Success(val list: List<MeasurementResponse>) : MeasurementsState()
    data class Error(val message: String) : MeasurementsState()
}

class MeasurementsViewModel : ViewModel() {

    private val api            = App.instance.apiService
    private val tokenStorage   = App.instance.tokenStorage
    private val networkMonitor = App.instance.networkMonitor
    private val dao            = App.instance.database.measurementDao()

    private val _state = MutableLiveData<MeasurementsState>()
    val state: LiveData<MeasurementsState> = _state

    private val _saved = MutableLiveData<Boolean>()
    val saved: LiveData<Boolean> = _saved

    private var targetClientId = -1
    var isOwnData = true
        private set

    init {
        viewModelScope.launch {
            networkMonitor.isOnline.collectLatest { online ->
                if (online && _state.value is MeasurementsState.Error) load()
            }
        }
    }

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

            // Показываем кеш мгновенно
            val cached = dao.getAll(targetClientId).map { it.toResponse() }
            if (cached.isNotEmpty()) {
                _state.value = MeasurementsState.Success(cached)
            }

            try {
                val response = api.getMeasurementHistory(targetClientId)
                if (response.isSuccessful) {
                    val fresh = response.body() ?: emptyList()
                    dao.deleteAll(targetClientId)
                    dao.insertAll(fresh.map { it.toEntity(targetClientId) })
                    _state.value = MeasurementsState.Success(fresh)
                } else if (cached.isEmpty()) {
                    _state.value = MeasurementsState.Error("Ошибка загрузки замеров")
                }
            } catch (e: Exception) {
                if (cached.isEmpty()) {
                    _state.value = MeasurementsState.Error(e.toUserMessage())
                }
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
                _state.value = MeasurementsState.Error(e.toUserMessage())
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
