package com.fitnesstrainer.app.ui.onboarding

import android.net.Uri
import android.os.Bundle
import androidx.activity.result.PickVisualMediaRequest
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.fitnesstrainer.app.App
import com.fitnesstrainer.app.R
import com.fitnesstrainer.app.databinding.FragmentOnboardingPhotosBinding
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream

class OnboardingPhotosFragment : Fragment() {

    private var _b: FragmentOnboardingPhotosBinding? = null
    private val b get() = _b!!

    private var pendingPose: String = "front"
    private val uris = mutableMapOf<String, Uri>()

    private val pickImage = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri ?: return@registerForActivityResult
        uris[pendingPose] = uri
        when (pendingPose) {
            "front" -> { b.ivFront.setImageURI(uri); b.ivFront.visibility = View.VISIBLE; b.placeholderFront.visibility = View.GONE }
            "side"  -> { b.ivSide.setImageURI(uri);  b.ivSide.visibility  = View.VISIBLE; b.placeholderSide.visibility  = View.GONE }
            "back"  -> { b.ivBack.setImageURI(uri);  b.ivBack.visibility  = View.VISIBLE; b.placeholderBack.visibility  = View.GONE }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _b = FragmentOnboardingPhotosBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Fix back card width to match half-width
        b.cardBack.post {
            val params = b.cardBack.layoutParams
            params.width = (b.cardFront.width)
            b.cardBack.layoutParams = params
        }

        val req = PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        b.cardFront.setOnClickListener { pendingPose = "front"; pickImage.launch(req) }
        b.cardSide.setOnClickListener  { pendingPose = "side";  pickImage.launch(req) }
        b.cardBack.setOnClickListener  { pendingPose = "back";  pickImage.launch(req) }

        b.btnUpload.setOnClickListener { uploadAll() }
        b.btnSkip.setOnClickListener   { navigateToDashboard() }
    }

    private fun uploadAll() {
        if (uris.isEmpty()) {
            b.tvError.text = "Выберите хотя бы одно фото"
            b.tvError.visibility = View.VISIBLE
            return
        }
        setLoading(true)
        lifecycleScope.launch {
            try {
                for ((pose, uri) in uris) {
                    val file = uriToFile(uri) ?: continue
                    val part = MultipartBody.Part.createFormData(
                        "file", file.name,
                        file.asRequestBody("image/*".toMediaTypeOrNull())
                    )
                    val poseBody = pose.toRequestBody("text/plain".toMediaTypeOrNull())
                    val visible  = "true".toRequestBody("text/plain".toMediaTypeOrNull())
                    (requireActivity().application as App).apiService.uploadPhoto(part, poseBody, null, visible)
                }
                navigateToDashboard()
            } catch (_: Exception) {
                b.tvError.text = "Ошибка загрузки"
                b.tvError.visibility = View.VISIBLE
            } finally {
                setLoading(false)
            }
        }
    }

    private fun uriToFile(uri: Uri): File? {
        return try {
            val ctx = requireContext()
            val input = ctx.contentResolver.openInputStream(uri) ?: return null
            val file  = File(ctx.cacheDir, "upload_${System.currentTimeMillis()}.jpg")
            FileOutputStream(file).use { input.copyTo(it) }
            file
        } catch (_: Exception) { null }
    }

    private fun navigateToDashboard() {
        findNavController().navigate(R.id.action_onboardingPhotos_to_trainerSelection)
    }

    private fun setLoading(loading: Boolean) {
        b.progress.visibility  = if (loading) View.VISIBLE else View.GONE
        b.btnUpload.isEnabled  = !loading
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}
