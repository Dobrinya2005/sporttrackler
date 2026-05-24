package com.fitnesstrainer.app.ui.client

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitnesstrainer.app.App
import com.fitnesstrainer.app.data.model.AddReviewRequest
import com.fitnesstrainer.app.util.toUserMessage
import com.fitnesstrainer.app.data.model.DailySummaryResponse
import com.fitnesstrainer.app.data.model.MeasurementResponse
import com.fitnesstrainer.app.data.model.NewsItem
import com.fitnesstrainer.app.data.model.TrainerInfo
import com.fitnesstrainer.app.data.model.UserProfile
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.collectLatest
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

    private val api            = App.instance.apiService
    private val tokenStorage   = App.instance.tokenStorage
    private val networkMonitor = App.instance.networkMonitor

    private val _state = MutableLiveData<DashboardState>()
    val state: LiveData<DashboardState> = _state

    private val _news = MutableLiveData<List<NewsItem>>(emptyList())
    val news: LiveData<List<NewsItem>> = _news

    init {
        viewModelScope.launch {
            networkMonitor.isOnline.collectLatest { online ->
                if (online && _state.value is DashboardState.Error) {
                    load()
                }
            }
        }
    }

    private val _navigateToChat = MutableLiveData<Pair<Int, String>?>()
    val navigateToChat: LiveData<Pair<Int, String>?> = _navigateToChat

    private val _chatError = MutableLiveData<String?>()
    val chatError: LiveData<String?> = _chatError

    private val _myTrainer = MutableLiveData<TrainerInfo?>(null)
    val myTrainer: LiveData<TrainerInfo?> = _myTrainer

    private val _avatarUrl = MutableLiveData<String?>(null)
    val avatarUrl: LiveData<String?> = _avatarUrl

    fun load() {
        viewModelScope.launch {
            _state.value = DashboardState.Loading
            try {
                val userId    = tokenStorage.getUserId()
                val firstName = tokenStorage.getFirstName() ?: ""
                val today     = LocalDate.now().toString()

                val latestDeferred  = async { api.getLatestMeasurement(userId) }
                val summaryDeferred = async { api.getDailySummary(userId, today) }
                val avatarDeferred  = async { api.getMe() }
                val trainerDeferred = async { api.getMyTrainer() }

                val latestResp  = latestDeferred.await()
                val summaryResp = summaryDeferred.await()
                val avatarResp  = avatarDeferred.await()
                val trainerResp = trainerDeferred.await()

                if (avatarResp.isSuccessful) _avatarUrl.value = avatarResp.body()?.avatarUrl
                if (trainerResp.isSuccessful) _myTrainer.value = trainerResp.body()

                val rawSummary = if (summaryResp.isSuccessful) summaryResp.body() else null
                val todaySummary = rawSummary?.let { s ->
                    if (s.calorieGoal == null) {
                        val weight = if (latestResp.isSuccessful) latestResp.body()?.weightKg else null
                        val estimated = weight?.let { ((it * 33) / 50).toInt() * 50 }
                        s.copy(calorieGoal = estimated)
                    } else s
                }

                _state.value = DashboardState.Success(
                    DashboardData(
                        firstName    = firstName,
                        latest       = if (latestResp.isSuccessful) latestResp.body() else null,
                        todaySummary = todaySummary
                    )
                )
            } catch (e: Exception) {
                _state.value = DashboardState.Error(e.toUserMessage())
            }
        }
    }

    fun loadAvatar() {
        viewModelScope.launch {
            try {
                val resp = api.getMe()
                if (resp.isSuccessful) _avatarUrl.value = resp.body()?.avatarUrl
            } catch (_: Exception) {}
        }
    }

    fun loadTrainer() {
        viewModelScope.launch {
            try {
                val response = api.getMyTrainer()
                if (response.isSuccessful) _myTrainer.value = response.body()
            } catch (_: Exception) {}
        }
    }

    fun openChat() {
        viewModelScope.launch {
            try {
                val trainer = _myTrainer.value
                    ?: api.getMyTrainer().body()
                if (trainer == null) {
                    _chatError.value = "Тренер не назначен. Отсканируйте QR-код тренера для подключения."
                    return@launch
                }
                _navigateToChat.value = Pair(trainer.userId, "${trainer.firstName} ${trainer.lastName}")
            } catch (_: Exception) {
                _chatError.value = "Не удалось подключиться к тренеру"
            }
        }
    }

    fun clearChatError() { _chatError.value = null }

    fun submitReview(trainerId: Int, rating: Int, comment: String?) {
        viewModelScope.launch {
            try {
                api.addReview(AddReviewRequest(trainerId, rating, comment))
            } catch (_: Exception) {}
        }
    }

    fun clearNavigation() {
        _navigateToChat.value = null
    }

    fun loadNews() {
        viewModelScope.launch {
            try {
                val resp = api.getNews()
                if (resp.isSuccessful) _news.value = resp.body() ?: emptyList()
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

