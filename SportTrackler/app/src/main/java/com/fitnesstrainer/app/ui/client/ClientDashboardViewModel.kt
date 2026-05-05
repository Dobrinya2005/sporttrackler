package com.fitnesstrainer.app.ui.client

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitnesstrainer.app.App
import com.fitnesstrainer.app.data.model.DailySummaryResponse
import com.fitnesstrainer.app.data.model.MeasurementResponse
import kotlinx.coroutines.launch
import java.time.LocalDate

data class DashboardData(
    val firstName: String,
    val latest: MeasurementResponse?,
    val todaySummary: DailySummaryResponse?
)

sealed class DashboardState {
    object Loading : DashboardState()
    data class Success(val data: DashboardData) : DashboardState()
    data class Error(val message: String) : DashboardState()
}

class ClientDashboardViewModel : ViewModel() {

    private val api          = App.instance.apiService
    private val tokenStorage = App.instance.tokenStorage

    private val _state = MutableLiveData<DashboardState>()
    val state: LiveData<DashboardState> = _state

    private val _navigateToChat = MutableLiveData<Pair<Int, String>?>()
    val navigateToChat: LiveData<Pair<Int, String>?> = _navigateToChat

    fun load() {
        viewModelScope.launch {
            _state.value = DashboardState.Loading
            try {
                val userId    = tokenStorage.getUserId()
                val firstName = tokenStorage.getFirstName() ?: ""
                val today     = LocalDate.now().toString()

                val latestResp  = api.getLatestMeasurement(userId)
                val summaryResp = api.getDailySummary(userId, today)

                _state.value = DashboardState.Success(
                    DashboardData(
                        firstName   = firstName,
                        latest      = if (latestResp.isSuccessful) latestResp.body() else null,
                        todaySummary = if (summaryResp.isSuccessful) summaryResp.body() else null
                    )
                )
            } catch (e: Exception) {
                _state.value = DashboardState.Error("Нет подключения к серверу")
            }
        }
    }

    fun openChat() {
        viewModelScope.launch {
            try {
                val response = api.getMyTrainer()
                if (response.isSuccessful) {
                    val t = response.body()!!
                    _navigateToChat.value = Pair(t.userId, "${t.firstName} ${t.lastName}")
                }
            } catch (_: Exception) {}
        }
    }

    fun clearNavigation() {
        _navigateToChat.value = null
    }

    fun logout() {
        viewModelScope.launch {
            try { api.logout() } catch (_: Exception) {}
            tokenStorage.clearAuth()
        }
    }
}
