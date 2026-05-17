package com.fitnesstrainer.app.ui.admin

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitnesstrainer.app.App
import com.fitnesstrainer.app.data.model.*
import kotlinx.coroutines.launch

class AdminViewModel : ViewModel() {

    private val api get() = App.instance.apiService

    private val _trainers = MutableLiveData<List<AdminTrainerItem>>()
    val trainers: LiveData<List<AdminTrainerItem>> = _trainers

    private val _reviews = MutableLiveData<List<AdminReviewItem>>()
    val reviews: LiveData<List<AdminReviewItem>> = _reviews

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _success = MutableLiveData<String?>()
    val success: LiveData<String?> = _success

    fun loadTrainers() {
        viewModelScope.launch {
            _loading.value = true
            try {
                val r = api.adminGetTrainers()
                if (r.isSuccessful) _trainers.value = r.body() ?: emptyList()
                else _error.value = "Ошибка загрузки тренеров"
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _loading.value = false
            }
        }
    }

    fun createTrainer(firstName: String, lastName: String, email: String, password: String) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val r = api.adminCreateTrainer(CreateTrainerRequest(firstName, lastName, email, password))
                if (r.isSuccessful) {
                    _success.value = "Тренер создан"
                    loadTrainers()
                } else {
                    _error.value = "Ошибка: ${r.errorBody()?.string()}"
                }
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _loading.value = false
            }
        }
    }

    fun toggleBlock(trainerId: Int) {
        viewModelScope.launch {
            try {
                val r = api.adminToggleBlock(trainerId)
                if (r.isSuccessful) {
                    loadTrainers()
                } else {
                    _error.value = "Ошибка"
                }
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun deleteTrainer(trainerId: Int) {
        viewModelScope.launch {
            try {
                val r = api.adminDeleteTrainer(trainerId)
                if (r.isSuccessful) {
                    _success.value = "Тренер удалён"
                    loadTrainers()
                } else {
                    _error.value = "Ошибка удаления"
                }
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun loadReviews(trainerId: Int) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val r = api.adminGetReviews(trainerId)
                if (r.isSuccessful) _reviews.value = r.body() ?: emptyList()
                else _error.value = "Ошибка загрузки отзывов"
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _loading.value = false
            }
        }
    }

    fun deleteReview(reviewId: Int, trainerId: Int) {
        viewModelScope.launch {
            try {
                val r = api.adminDeleteReview(reviewId)
                if (r.isSuccessful) {
                    _success.value = "Отзыв удалён"
                    loadReviews(trainerId)
                } else {
                    _error.value = "Ошибка"
                }
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun replyReview(reviewId: Int, reply: String, trainerId: Int) {
        viewModelScope.launch {
            try {
                val r = api.adminReplyReview(reviewId, ReplyRequest(reply))
                if (r.isSuccessful) {
                    _success.value = "Ответ сохранён"
                    loadReviews(trainerId)
                } else {
                    _error.value = "Ошибка"
                }
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun clearMessages() {
        _error.value = null
        _success.value = null
    }
}
