package com.fitnesstrainer.app.ui.food

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitnesstrainer.app.App
import com.fitnesstrainer.app.data.model.DailySummaryResponse
import com.fitnesstrainer.app.data.model.DiaryEntryRequest
import com.fitnesstrainer.app.data.model.FoodProduct
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate

sealed class FoodDiaryState {
    object Loading : FoodDiaryState()
    data class Success(val summary: DailySummaryResponse) : FoodDiaryState()
    data class Error(val message: String) : FoodDiaryState()
}

sealed class SearchState {
    object Idle : SearchState()
    object Loading : SearchState()
    data class Results(val list: List<FoodProduct>) : SearchState()
}

class FoodDiaryViewModel : ViewModel() {

    private val api          = App.instance.apiService
    private val tokenStorage = App.instance.tokenStorage

    private val _state = MutableLiveData<FoodDiaryState>()
    val state: LiveData<FoodDiaryState> = _state

    private val _searchState = MutableLiveData<SearchState>(SearchState.Idle)
    val searchState: LiveData<SearchState> = _searchState

    private val _currentDate = MutableLiveData(LocalDate.now())
    val currentDate: LiveData<LocalDate> = _currentDate

    private var searchJob: Job? = null
    private var userId = -1

    init {
        viewModelScope.launch {
            userId = tokenStorage.getUserId()
            loadDiary()
        }
    }

    fun loadDiary(date: LocalDate = _currentDate.value ?: LocalDate.now()) {
        _currentDate.value = date
        viewModelScope.launch {
            _state.value = FoodDiaryState.Loading
            try {
                val response = api.getDailySummary(userId, date.toString())
                if (response.isSuccessful) {
                    _state.value = FoodDiaryState.Success(response.body()!!)
                } else {
                    _state.value = FoodDiaryState.Error("Нет данных")
                }
            } catch (e: Exception) {
                _state.value = FoodDiaryState.Error("Нет подключения к серверу")
            }
        }
    }

    fun previousDay() { loadDiary((_currentDate.value ?: LocalDate.now()).minusDays(1)) }
    fun nextDay()     { loadDiary((_currentDate.value ?: LocalDate.now()).plusDays(1)) }

    fun searchFood(query: String) {
        if (query.isBlank()) { _searchState.value = SearchState.Idle; return }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(400)
            _searchState.value = SearchState.Loading
            try {
                val response = api.searchFood(query)
                _searchState.value = if (response.isSuccessful)
                    SearchState.Results(response.body() ?: emptyList())
                else SearchState.Idle
            } catch (_: Exception) { _searchState.value = SearchState.Idle }
        }
    }

    fun addToMeal(productId: Int, weightG: Double, mealType: String) {
        viewModelScope.launch {
            try {
                val date    = _currentDate.value ?: LocalDate.now()
                val request = DiaryEntryRequest(date.toString(), mealType, productId, weightG)
                val resp    = api.addDiaryEntry(request)
                if (resp.isSuccessful) loadDiary()
            } catch (_: Exception) {}
        }
    }

    fun deleteEntry(diaryId: Int) {
        viewModelScope.launch {
            try {
                api.deleteDiaryEntry(diaryId)
                loadDiary()
            } catch (_: Exception) {}
        }
    }

    fun clearSearch() { _searchState.value = SearchState.Idle }
}
