package com.fitnesstrainer.app.ui.photos

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitnesstrainer.app.App
import com.fitnesstrainer.app.data.model.ProgressPhoto
import com.fitnesstrainer.app.util.toUserMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
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

    private val api            = App.instance.apiService
    private val tokenStorage   = App.instance.tokenStorage
    private val networkMonitor = App.instance.networkMonitor

    private val _state = MutableLiveData<PhotosState>()
    val state: LiveData<PhotosState> = _state

    private val _uploading = MutableLiveData(false)
    val uploading: LiveData<Boolean> = _uploading

    private val _deleting = MutableLiveData(false)
    val deleting: LiveData<Boolean> = _deleting

    private var targetClientId = -1
    var isOwnData = true
        private set

    init {
        viewModelScope.launch {
            networkMonitor.isOnline.collectLatest { online ->
                if (online && _state.value is PhotosState.Error) load()
            }
        }
    }

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
                _state.value = PhotosState.Error(e.toUserMessage())
            }
        }
    }

    fun deletePhoto(photoId: Int, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _deleting.value = true
            try {
                val resp = api.deletePhoto(photoId)
                if (resp.isSuccessful) { load(); onSuccess() }
            } catch (_: Exception) {
            } finally {
                _deleting.value = false
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
                    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    if (bytes != null) {
                        // Read EXIF rotation before decoding
                        val rotation = ExifInterface(bytes.inputStream()).let {
                            when (it.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                                ExifInterface.ORIENTATION_ROTATE_90  -> 90f
                                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                                else -> 0f
                            }
                        }
                        var bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        // Apply rotation from EXIF
                        if (rotation != 0f) {
                            val m = Matrix().apply { postRotate(rotation) }
                            val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, m, true)
                            bitmap.recycle()
                            bitmap = rotated
                        }
                        // Scale down if too large
                        val maxSide = 1080
                        val scaled = if (bitmap.width > maxSide || bitmap.height > maxSide) {
                            val ratio = bitmap.width.toFloat() / bitmap.height
                            if (ratio > 1f)
                                Bitmap.createScaledBitmap(bitmap, maxSide, (maxSide / ratio).toInt(), true)
                            else
                                Bitmap.createScaledBitmap(bitmap, (maxSide * ratio).toInt(), maxSide, true)
                        } else bitmap
                        FileOutputStream(file).use { out -> scaled.compress(Bitmap.CompressFormat.JPEG, 82, out) }
                        if (scaled !== bitmap) scaled.recycle()
                        bitmap.recycle()
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
                _state.value = PhotosState.Error(e.toUserMessage())
            } finally {
                _uploading.value = false
            }
        }
    }
}
