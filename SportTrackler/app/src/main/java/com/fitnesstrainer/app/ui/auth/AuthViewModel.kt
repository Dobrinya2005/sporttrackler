package com.fitnesstrainer.app.ui.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitnesstrainer.app.App
import com.fitnesstrainer.app.data.model.LoginRequest
import com.fitnesstrainer.app.data.model.RegisterRequest
import com.fitnesstrainer.app.util.toUserMessage
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val role: String, val devCode: String? = null) : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel : ViewModel() {

    private val api          = App.instance.apiService
    private val tokenStorage = App.instance.tokenStorage

    private val _state = MutableLiveData<AuthState>(AuthState.Idle)
    val state: LiveData<AuthState> = _state

    fun resetState() { _state.value = AuthState.Idle }

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _state.value = AuthState.Error("Заполните все поля")
            return
        }
        viewModelScope.launch {
            _state.value = AuthState.Loading
            try {
                val response = api.login(LoginRequest(email.trim(), password))
                if (response.isSuccessful) {
                    val body = response.body()!!
                    tokenStorage.saveAuth(
                        body.accessToken, body.refreshToken,
                        body.userId, body.role,
                        body.firstName, body.lastName,
                        body.email, body.avatarUrl
                    )
                    registerFcmToken()
                    _state.value = AuthState.Success(body.role)
                } else {
                    _state.value = AuthState.Error("Неверный email или пароль")
                }
            } catch (e: Exception) {
                _state.value = AuthState.Error(e.toUserMessage())
            }
        }
    }

    private suspend fun registerFcmToken() {
        try {
            val token = FirebaseMessaging.getInstance().token.await()
            api.registerFcmToken(mapOf("deviceToken" to token))
        } catch (_: Exception) {}
    }

    fun register(
        firstName: String, lastName: String,
        email: String, password: String,
        role: String, phone: String?,
        trainerCode: String? = null,
        weightKg: Double? = null,
        heightCm: Double? = null,
        gender: String? = null
    ) {
        if (firstName.isBlank() || lastName.isBlank() || email.isBlank() || password.isBlank()) {
            _state.value = AuthState.Error("Заполните все обязательные поля")
            return
        }
        if (password.length < 6) {
            _state.value = AuthState.Error("Пароль минимум 6 символов")
            return
        }
        if (role == "Trainer" && trainerCode.isNullOrBlank()) {
            _state.value = AuthState.Error("Введите код тренера")
            return
        }
        viewModelScope.launch {
            _state.value = AuthState.Loading
            try {
                val response = api.register(
                    RegisterRequest(firstName.trim(), lastName.trim(),
                        email.trim(), password, role, phone?.trim(), trainerCode?.trim())
                )
                if (response.isSuccessful) {
                    _state.value = AuthState.Success(role, response.body()?.devCode)
                } else {
                    val msg = try { response.errorBody()?.string() } catch (_: Exception) { null }
                    _state.value = AuthState.Error(when {
                        msg?.contains("код", ignoreCase = true) == true ||
                        msg?.contains("Trainer", ignoreCase = true) == true -> "Неверный код тренера"
                        response.code() == 409 ||
                        response.code() == 400 ||
                        msg?.contains("exist", ignoreCase = true) == true ||
                        msg?.contains("занят", ignoreCase = true) == true -> "Данный email уже занят"
                        else -> "Ошибка регистрации (${response.code()})"
                    })
                }
            } catch (e: Exception) {
                _state.value = AuthState.Error(e.toUserMessage())
            }
        }
    }
}
