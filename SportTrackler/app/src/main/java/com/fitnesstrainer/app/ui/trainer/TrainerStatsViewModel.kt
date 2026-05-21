package com.fitnesstrainer.app.ui.trainer

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitnesstrainer.app.App
import com.fitnesstrainer.app.data.model.ClientProfile
import com.fitnesstrainer.app.data.model.MeasurementResponse
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch

data class ClientWeight(
    val client: ClientProfile,
    val latestMeasurement: MeasurementResponse?
)

data class TrainerStatsData(
    val clientCount: Int,
    val avgRating: Double?,
    val reviewCount: Int,
    val ratingDistribution: Map<Int, Int>,
    val clientWeights: List<ClientWeight>
)

sealed class StatsState {
    object Loading : StatsState()
    data class Success(val data: TrainerStatsData) : StatsState()
    data class Error(val message: String) : StatsState()
}

class TrainerStatsViewModel : ViewModel() {

    private val api          = App.instance.apiService
    private val tokenStorage = App.instance.tokenStorage

    private val _state = MutableLiveData<StatsState>()
    val state: LiveData<StatsState> = _state

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.value = StatsState.Loading
            try {
                val trainerId = tokenStorage.getUserId()

                val clientsDeferred = async { api.getMyClients() }
                val reviewsDeferred = async { api.getTrainerReviews(trainerId) }

                val clientsResp = clientsDeferred.await()
                val reviewsResp = reviewsDeferred.await()

                val clients = if (clientsResp.isSuccessful) clientsResp.body() ?: emptyList() else emptyList()
                val reviewsBody = if (reviewsResp.isSuccessful) reviewsResp.body() else null

                val ratingDist = mutableMapOf(1 to 0, 2 to 0, 3 to 0, 4 to 0, 5 to 0)
                reviewsBody?.reviews?.forEach { r ->
                    ratingDist[r.rating] = (ratingDist[r.rating] ?: 0) + 1
                }

                val weightJobs = clients.map { client ->
                    async {
                        val resp = try { api.getLatestMeasurement(client.userId) } catch (_: Exception) { null }
                        ClientWeight(client, if (resp?.isSuccessful == true) resp.body() else null)
                    }
                }
                val clientWeights = weightJobs.awaitAll()

                _state.value = StatsState.Success(
                    TrainerStatsData(
                        clientCount        = clients.size,
                        avgRating          = reviewsBody?.averageRating,
                        reviewCount        = reviewsBody?.reviews?.size ?: 0,
                        ratingDistribution = ratingDist,
                        clientWeights      = clientWeights.sortedByDescending { it.latestMeasurement?.weightKg }
                    )
                )
            } catch (e: Exception) {
                _state.value = StatsState.Error("Ошибка загрузки: ${e.message}")
            }
        }
    }
}
