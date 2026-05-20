package com.fitnesstrainer.app.ui.chat

import android.media.MediaRecorder
import android.os.Build
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
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.util.concurrent.TimeUnit

class GroupChatViewModel : ViewModel() {

    private val api          = App.instance.apiService
    private val tokenStorage = App.instance.tokenStorage

    private val _messages = MutableLiveData<List<GroupMessageDto>>(emptyList())
    val messages: LiveData<List<GroupMessageDto>> = _messages

    private val _group = MutableLiveData<GroupDto?>()
    val group: LiveData<GroupDto?> = _group

    private val _accessToken = MutableLiveData<String?>(null)
    val accessToken: LiveData<String?> = _accessToken

    private val _uploadError = MutableLiveData<String?>(null)
    val uploadError: LiveData<String?> = _uploadError

    private val _recordingError = MutableLiveData<String?>(null)
    val recordingError: LiveData<String?> = _recordingError

    private val _amplitudes = MutableLiveData<FloatArray>(floatArrayOf())
    val amplitudes: LiveData<FloatArray> = _amplitudes
    private val amplitudeList = mutableListOf<Float>()
    private val amplitudeHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val amplitudeSampler = object : Runnable {
        override fun run() {
            val amp = mediaRecorder?.maxAmplitude ?: 0
            amplitudeList.add((amp / 32767f).coerceIn(0f, 1f))
            _amplitudes.postValue(amplitudeList.toFloatArray())
            amplitudeHandler.postDelayed(this, 80)
        }
    }

    private var mediaRecorder: MediaRecorder? = null
    private var audioFile: File? = null

    private var hubConnection: HubConnection? = null
    var groupId: Int = 0
        private set

    fun init(groupId: Int) {
        this.groupId = groupId
        viewModelScope.launch { _accessToken.value = tokenStorage.getAccessToken() }
        loadMessages()
        loadGroupInfo()
        viewModelScope.launch { connectSignalR() }
    }

    fun loadGroupInfo() {
        viewModelScope.launch {
            try {
                val resp = api.getMyGroups()
                if (resp.isSuccessful) {
                    _group.value = resp.body()?.find { it.groupId == groupId }
                }
            } catch (_: Exception) {}
        }
    }

    fun addMember(userId: Int, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val resp = api.addGroupMember(groupId, userId)
                if (resp.isSuccessful) {
                    loadGroupInfo()
                    onResult(true)
                } else {
                    onResult(false)
                }
            } catch (_: Exception) { onResult(false) }
        }
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
                val resp = api.sendGroupMessage(groupId, SendGroupMessageRequest(text = text))
                if (resp.isSuccessful) resp.body()?.let { appendMessage(it) }
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
                val sendResp = api.sendGroupMessage(
                    groupId,
                    SendGroupMessageRequest(text = null, attachmentUrl = mediaUrl, attachmentType = attachmentType)
                )
                if (sendResp.isSuccessful) sendResp.body()?.let { appendMessage(it) }
                else _uploadError.value = "Ошибка отправки"
            } catch (e: Exception) {
                _uploadError.value = "Ошибка: ${e.message}"
            }
        }
    }

    fun startVoiceRecording(outputFile: File) {
        audioFile = outputFile
        try {
            mediaRecorder = (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                MediaRecorder(App.instance)
            else
                @Suppress("DEPRECATION") MediaRecorder()).apply {
                setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128_000)
                setAudioSamplingRate(44_100)
                setOutputFile(outputFile.absolutePath)
                prepare()
                start()
            }
            amplitudeList.clear()
            _amplitudes.postValue(floatArrayOf())
            amplitudeHandler.postDelayed(amplitudeSampler, 150)
        } catch (e: Exception) {
            mediaRecorder?.release()
            mediaRecorder = null
            audioFile?.delete()
            audioFile = null
            _recordingError.postValue("Ошибка записи: ${e.message}")
        }
    }

    fun stopVoiceRecordingAndSend() {
        amplitudeHandler.removeCallbacks(amplitudeSampler)
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
        amplitudeHandler.removeCallbacks(amplitudeSampler)
        try { mediaRecorder?.pause() } catch (_: Exception) {}
    }

    fun resumeVoiceRecording() {
        try { mediaRecorder?.resume() } catch (_: Exception) {}
        amplitudeHandler.postDelayed(amplitudeSampler, 150)
    }

    fun cancelVoiceRecording() {
        amplitudeHandler.removeCallbacks(amplitudeSampler)
        try {
            mediaRecorder?.stop()
            mediaRecorder?.release()
        } catch (_: Exception) {}
        mediaRecorder = null
        audioFile?.delete()
        audioFile = null
        amplitudeList.clear()
        _amplitudes.postValue(floatArrayOf())
    }

    fun appendMessage(msg: GroupMessageDto) {
        val current = _messages.value?.toMutableList() ?: mutableListOf()
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
        amplitudeHandler.removeCallbacks(amplitudeSampler)
        try { hubConnection?.stop() } catch (_: Exception) {}
        mediaRecorder?.release()
    }
}
