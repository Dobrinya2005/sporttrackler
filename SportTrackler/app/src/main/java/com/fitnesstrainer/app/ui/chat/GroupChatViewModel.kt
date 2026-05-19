package com.fitnesstrainer.app.ui.chat

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitnesstrainer.app.App
import com.fitnesstrainer.app.BuildConfig
import com.fitnesstrainer.app.data.model.GroupDto
import com.fitnesstrainer.app.data.model.GroupMessageDto
import com.fitnesstrainer.app.data.model.SendGroupMessageRequest
import com.microsoft.signalr.HubConnection
import com.microsoft.signalr.HubConnectionBuilder
import com.microsoft.signalr.HubConnectionState
import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class GroupChatViewModel : ViewModel() {

    private val api          = App.instance.apiService
    private val tokenStorage = App.instance.tokenStorage

    private val _messages = MutableLiveData<List<GroupMessageDto>>(emptyList())
    val messages: LiveData<List<GroupMessageDto>> = _messages

    private val _group = MutableLiveData<GroupDto?>()
    val group: LiveData<GroupDto?> = _group

    private var hubConnection: HubConnection? = null
    var groupId: Int = 0
        private set

    fun init(groupId: Int) {
        this.groupId = groupId
        loadMessages()
        viewModelScope.launch { connectSignalR() }
    }

    fun loadMessages() {
        viewModelScope.launch {
            try {
                val resp = api.getGroupMessages(groupId)
                if (resp.isSuccessful) {
                    _messages.value = resp.body()?.reversed() ?: emptyList()
                }
            } catch (_: Exception) {}
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            try {
                val resp = api.sendGroupMessage(groupId, SendGroupMessageRequest(text))
                if (resp.isSuccessful) resp.body()?.let { appendMessage(it) }
            } catch (_: Exception) {}
        }
    }

    fun appendMessage(msg: GroupMessageDto) {
        val current = _messages.value?.toMutableList() ?: mutableListOf()
        // Избегаем дублей — сообщение уже могло прийти через SignalR
        if (current.any { it.messageId == msg.messageId }) return
        current.add(msg)
        _messages.postValue(current)
    }

    private suspend fun connectSignalR() {
        val token   = tokenStorage.getAccessToken() ?: return
        val baseUrl = BuildConfig.BASE_URL.removeSuffix("/")

        try { hubConnection?.stop() } catch (_: Exception) {}

        hubConnection = HubConnectionBuilder
            .create("$baseUrl/hubs/chat")
            .withAccessTokenProvider(Single.just(token))
            .build()

        hubConnection?.on("ReceiveGroupMessage", { msg: GroupMessageDto ->
            if (msg.groupId == groupId) appendMessage(msg)
        }, GroupMessageDto::class.java)

        withContext(Dispatchers.IO) {
            try {
                hubConnection?.start()?.blockingAwait(10, TimeUnit.SECONDS)
                hubConnection?.invoke("JoinGroup", groupId)?.blockingAwait(5, TimeUnit.SECONDS)
            } catch (_: Exception) {}
        }
    }

    override fun onCleared() {
        super.onCleared()
        try { hubConnection?.stop() } catch (_: Exception) {}
    }
}
