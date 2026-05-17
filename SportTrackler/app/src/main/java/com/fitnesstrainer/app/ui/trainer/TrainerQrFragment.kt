package com.fitnesstrainer.app.ui.trainer

import android.graphics.Bitmap
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.fitnesstrainer.app.App
import com.fitnesstrainer.app.databinding.FragmentTrainerQrBinding
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.launch

class TrainerQrFragment : Fragment() {

    private var _b: FragmentTrainerQrBinding? = null
    private val b get() = _b!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _b = FragmentTrainerQrBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        b.btnBack.setOnClickListener { findNavController().popBackStack() }

        lifecycleScope.launch {
            val trainerId = App.instance.tokenStorage.getUserId()
            val firstName = App.instance.tokenStorage.getFirstName() ?: ""
            val lastName  = App.instance.tokenStorage.getLastName()  ?: ""
            b.tvName.text = "$firstName $lastName"
            b.tvHint.text = "Тренер • SportTrackler"

            val initials = listOf(firstName, lastName)
                .filter { it.isNotEmpty() }
                .take(2)
                .joinToString("") { it.first().uppercaseChar().toString() }
            b.tvAvatarInitials.text = initials

            generateQr("sporttrackler://trainer/$trainerId")

            try {
                val resp = App.instance.apiService.getMyTrainerCode()
                if (resp.isSuccessful) {
                    b.tvTrainerCode.text = resp.body()?.trainerCode ?: "——————"
                }
            } catch (_: Exception) {}
        }
    }

    private fun generateQr(content: String) {
        try {
            val size = 600
            val bits = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size)
            val bmp  = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            for (x in 0 until size) {
                for (y in 0 until size) {
                    bmp.setPixel(x, y, if (bits[x, y]) 0xFF1A1A2E.toInt() else 0xFFFAFAFA.toInt())
                }
            }
            b.ivQr.setImageBitmap(bmp)
        } catch (_: Exception) {}
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}
