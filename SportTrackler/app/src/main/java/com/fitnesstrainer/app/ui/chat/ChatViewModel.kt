package com.fitnesstrainer.app.ui.chat

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitnesstrainer.app.App
import com.fitnesstrainer.app.BuildConfig
import com.fitnesstrainer.app.data.model.MessageDto
import com.fitnesstrainer.app.data.model.SendMessageRequest
import com.microsoft.signalr.HubConnection
import com.microsoft.signalr.HubConnectionBuilder
import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class ChatViewModel : ViewModel() {

    private val api          = App.instance.apiService
    private val tokenStorage = App.instance.tokenStorage

    private val _messages = MutableLiveData<List<MessageDto>>(emptyList())
    val messages: LiveData<List<MessageDto>> = _messages

    private val _myUserId = MutableLiveData(-1)
    val myUserId: LiveData<Int> = _myUserId

    private var hubConnection: HubConnection? = null
    private var contactId = -1

    fun init(contactId: Int) {
        this.contactId = contactId
        viewModelScope.launch {
            _myUserId.value = tokenStorage.getUserId()
            loadMessages()
            connectSignalR()
        }
    }

    fun loadMessages() {
        viewModelScope.launch {
            try {
                val response = api.getMessages(contactId)
                if (response.isSuccessful) {
                    _messages.value = response.body() ?: emptyList()
                    api.markRead(contactId)
                }
            } catch (_: Exception) {}
        }
    }

    private suspend fun connectSignalR() {
        val token   = tokenStorage.getAccessToken() ?: return
        val baseUrl = BuildConfig.BASE_URL.removeSuffix("/")

        hubConnection = HubConnectionBuilder
            .create("$baseUrl/hubs/chat")
            .withAccessTokenProvider(Single.just(token))
            .build()

        hubConnection?.on("ReceiveMessage", { dto: MessageDto ->
            val list = _messages.value?.toMutableList() ?: mutableListOf()
            list.add(dto)
            _messages.postValue(list)
        }, MessageDto::class.java)

        withContext(Dispatchers.IO) {
            try {
                hubConnection?.start()?.blockingAwait(10, TimeUnit.SECONDS)
            } catch (_: Exception) {}
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            try {
                val response = api.sendMessage(SendMessageRequest(contactId, text))
                if (response.isSuccessful) {
                    val list = _messages.value?.toMutableList() ?: mutableListOf()
                    response.body()?.let { list.add(it) }
                    _messages.value = list
                }
            } catch (_: Exception) {}
        }
    }

    override fun onCleared() {
        super.onCleared()
        try { hubConnection?.stop() } catch (_: Exception) {}
    }
}
