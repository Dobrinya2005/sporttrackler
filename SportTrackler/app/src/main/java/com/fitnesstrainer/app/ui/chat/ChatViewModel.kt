package com.fitnesstrainer.app.ui.chat

import android.media.MediaRecorder
import android.os.Build
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
import com.microsoft.signalr.HubConnectionState
import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.util.concurrent.TimeUnit

class ChatViewModel : ViewModel() {

    private val api            = App.instance.apiService
    private val tokenStorage   = App.instance.tokenStorage
    private val networkMonitor = App.instance.networkMonitor

    private val _messages = MutableLiveData<List<MessageDto>>(emptyList())
    val messages: LiveData<List<MessageDto>> = _messages

    private val _myUserId = MutableLiveData(-1)
    val myUserId: LiveData<Int> = _myUserId

    private val _contactProfile = MutableLiveData<com.fitnesstrainer.app.data.model.ClientProfile?>(null)
    val contactProfile: LiveData<com.fitnesstrainer.app.data.model.ClientProfile?> = _contactProfile

    private val _connectionState = MutableLiveData(false)
    val connectionState: LiveData<Boolean> = _connectionState

    data class ContactStatus(val isOnline: Boolean, val lastSeen: String?)
    private val _contactStatus = MutableLiveData<ContactStatus>()
    val contactStatus: LiveData<ContactStatus> = _contactStatus

    private val _uploadError = MutableLiveData<String?>(null)
    val uploadError: LiveData<String?> = _uploadError

    private var hubConnection: HubConnection? = null
    private var contactId = -1

    private var mediaRecorder: MediaRecorder? = null
    private var audioFile: File? = null

    fun init(contactId: Int) {
        this.contactId = contactId
        viewModelScope.launch {
            _myUserId.value = tokenStorage.getUserId()
            loadMessages()
            loadContactProfile()
            connectSignalR()
            observeNetwork()
        }
    }

    private fun observeNetwork() {
        viewModelScope.launch {
            networkMonitor.isOnline.collectLatest { online ->
                if (online) {
                    val state = hubConnection?.connectionState
                    if (state == null || state == HubConnectionState.DISCONNECTED) {
                        loadMessages()
                        connectSignalR()
                    }
                } else {
                    _connectionState.postValue(false)
                }
            }
        }
    }

    fun loadMessages() {
        viewModelScope.launch {
            try {
                val response = api.getMessages(contactId)
                if (response.isSuccessful) {
                    _messages.value = (response.body() ?: emptyList()).reversed()
                    api.markRead(contactId)
                }
            } catch (_: Exception) {}
        }
    }

    private fun loadContactProfile() {
        viewModelScope.launch {
            try {
                val resp = api.getMyClients()
                if (resp.isSuccessful) {
                    val match = resp.body()?.firstOrNull { it.userId == contactId }
                    if (match != null) _contactProfile.value = match
                }
            } catch (_: Exception) {}
        }
    }

    private suspend fun connectSignalR() {
        val token   = tokenStorage.getAccessToken() ?: return
        val baseUrl = BuildConfig.BASE_URL.removeSuffix("/")

        try { hubConnection?.stop() } catch (_: Exception) {}

        hubConnection = HubConnectionBuilder
            .create("$baseUrl/hubs/chat")
            .withAccessTokenProvider(Single.just(token))
            .build()

        hubConnection?.on("ReceiveMessage", { dto: MessageDto ->
            val list = _messages.value?.toMutableList() ?: mutableListOf()
            list.add(dto)
            _messages.postValue(list)
        }, MessageDto::class.java)

        hubConnection?.on("UserStatusChanged", { changedUserId: Int, isOnline: Boolean, lastSeen: String ->
            if (changedUserId == contactId) {
                _contactStatus.postValue(ContactStatus(isOnline, lastSeen.ifBlank { null }))
            }
        }, Int::class.java, Boolean::class.java, String::class.java)

        hubConnection?.onClosed {
            _connectionState.postValue(false)
        }

        withContext(Dispatchers.IO) {
            try {
                hubConnection?.start()?.blockingAwait(10, TimeUnit.SECONDS)
                val connected = hubConnection?.connectionState == HubConnectionState.CONNECTED
                _connectionState.postValue(connected)
                if (connected) {
                    // Получить текущий статус контакта
                    hubConnection?.send("GetUserStatus", contactId)
                }
            } catch (_: Exception) {
                _connectionState.postValue(false)
            }
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            try {
                val response = api.sendMessage(SendMessageRequest(contactId, text))
                if (response.isSuccessful) appendMessage(response.body())
            } catch (_: Exception) {}
        }
    }

    fun sendMediaFile(file: File, mimeType: String, forceAttachmentType: String? = null) {
        viewModelScope.launch {
            try {
                val requestBody = file.asRequestBody(mimeType.toMediaTypeOrNull())
                val part = MultipartBody.Part.createFormData("file", file.name, requestBody)
                val uploadResp = api.uploadChatMedia(part)
                if (!uploadResp.isSuccessful) {
                    _uploadError.value = "Ошибка загрузки файла"
                    return@launch
                }
                val mediaUrl       = uploadResp.body()?.get("mediaUrl") ?: return@launch
                val attachmentType = forceAttachmentType ?: uploadResp.body()?.get("attachmentType") ?: "file"
                val sendResp = api.sendMessage(
                    SendMessageRequest(contactId, null, mediaUrl, attachmentType)
                )
                if (sendResp.isSuccessful) appendMessage(sendResp.body())
                else _uploadError.value = "Ошибка отправки"
            } catch (e: Exception) {
                _uploadError.value = "Ошибка: ${e.message}"
            }
        }
    }

    fun startVoiceRecording(outputFile: File) {
        audioFile = outputFile
        mediaRecorder = (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            MediaRecorder(App.instance)
        else
            @Suppress("DEPRECATION") MediaRecorder()).apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(outputFile.absolutePath)
            prepare()
            start()
        }
    }

    fun stopVoiceRecordingAndSend() {
        try {
            mediaRecorder?.stop()
            mediaRecorder?.release()
            mediaRecorder = null
        } catch (_: Exception) { return }

        val file = audioFile ?: return
        if (file.exists() && file.length() > 0) {
            sendMediaFile(file, "audio/m4a")
        }
    }

    fun pauseVoiceRecording() {
        try { mediaRecorder?.pause() } catch (_: Exception) {}
    }

    fun resumeVoiceRecording() {
        try { mediaRecorder?.resume() } catch (_: Exception) {}
    }

    fun cancelVoiceRecording() {
        try {
            mediaRecorder?.stop()
            mediaRecorder?.release()
        } catch (_: Exception) {}
        mediaRecorder = null
        audioFile?.delete()
        audioFile = null
    }

    private fun appendMessage(dto: MessageDto?) {
        dto ?: return
        val list = _messages.value?.toMutableList() ?: mutableListOf()
        list.add(dto)
        _messages.value = list
    }

    fun disconnectHub() {
        viewModelScope.launch(Dispatchers.IO) {
            try { hubConnection?.stop()?.blockingAwait(5, TimeUnit.SECONDS) } catch (_: Exception) {}
            _connectionState.postValue(false)
        }
    }

    fun reconnectHub() {
        viewModelScope.launch {
            val state = hubConnection?.connectionState
            if (state == null || state == HubConnectionState.DISCONNECTED) {
                connectSignalR()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        try { hubConnection?.stop() } catch (_: Exception) {}
        mediaRecorder?.release()
    }
}
