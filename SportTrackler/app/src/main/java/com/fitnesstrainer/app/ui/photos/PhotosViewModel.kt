package com.fitnesstrainer.app.ui.photos

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitnesstrainer.app.App
import com.fitnesstrainer.app.data.model.ProgressPhoto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream

sealed class PhotosState {
    object Loading : PhotosState()
    data class Success(val photos: List<ProgressPhoto>) : PhotosState()
    data class Error(val message: String) : PhotosState()
}

class PhotosViewModel : ViewModel() {

    private val api          = App.instance.apiService
    private val tokenStorage = App.instance.tokenStorage

    private val _state = MutableLiveData<PhotosState>()
    val state: LiveData<PhotosState> = _state

    private val _uploading = MutableLiveData(false)
    val uploading: LiveData<Boolean> = _uploading

    private var targetClientId = -1
    var isOwnData = true
        private set

    fun init(clientId: Int) {
        viewModelScope.launch {
            val myId = tokenStorage.getUserId()
            targetClientId = if (clientId == -1) myId else clientId
            isOwnData      = (clientId == -1 || clientId == myId)
            load()
        }
    }

    fun load() {
        if (targetClientId == -1) return
        viewModelScope.launch {
            _state.value = PhotosState.Loading
            try {
                val response = api.getPhotos(targetClientId)
                _state.value = if (response.isSuccessful)
                    PhotosState.Success(response.body() ?: emptyList())
                else
                    PhotosState.Error("Ошибка загрузки фото")
            } catch (e: Exception) {
                _state.value = PhotosState.Error("Нет подключения к серверу")
            }
        }
    }

    fun uploadPhoto(uri: Uri, poseType: String?, description: String?) {
        viewModelScope.launch {
            _uploading.value = true
            try {
                val context  = App.instance
                val tmpFile  = withContext(Dispatchers.IO) {
                    val file = File(context.cacheDir, "upload_${System.currentTimeMillis()}.jpg")
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(file).use { output -> input.copyTo(output) }
                    }
                    file
                }
                val filePart = MultipartBody.Part.createFormData(
                    "file", tmpFile.name,
                    tmpFile.asRequestBody("image/*".toMediaTypeOrNull())
                )
                val visibleBody = "true".toRequestBody("text/plain".toMediaTypeOrNull())
                val poseBody    = poseType?.toRequestBody("text/plain".toMediaTypeOrNull())
                val descBody    = description?.toRequestBody("text/plain".toMediaTypeOrNull())

                val response = api.uploadPhoto(filePart, poseBody, descBody, visibleBody)
                if (response.isSuccessful) load()
                withContext(Dispatchers.IO) { tmpFile.delete() }
            } catch (e: Exception) {
                _state.value = PhotosState.Error("Ошибка загрузки: ${e.message}")
            } finally {
                _uploading.value = false
            }
        }
    }
}
