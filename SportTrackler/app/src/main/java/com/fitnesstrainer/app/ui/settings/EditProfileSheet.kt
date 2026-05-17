package com.fitnesstrainer.app.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.fitnesstrainer.app.App
import com.fitnesstrainer.app.data.model.UpdateProfileRequest
import com.fitnesstrainer.app.databinding.BottomSheetEditProfileBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch

class EditProfileSheet(
    private val onSaved: (firstName: String, lastName: String) -> Unit
) : BottomSheetDialogFragment() {

    private var _b: BottomSheetEditProfileBinding? = null
    private val b get() = _b!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _b = BottomSheetEditProfileBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lifecycleScope.launch {
            val ts = App.instance.tokenStorage
            b.etFirstName.setText(ts.getFirstName() ?: "")
            b.etLastName.setText(ts.getLastName()   ?: "")
            b.etPhone.setText(ts.getPhone()         ?: "")
        }

        b.btnClose.setOnClickListener { dismiss() }

        b.btnSave.setOnClickListener {
            val firstName = b.etFirstName.text?.toString()?.trim() ?: ""
            val lastName  = b.etLastName.text?.toString()?.trim()  ?: ""
            val phone     = b.etPhone.text?.toString()?.trim()?.ifEmpty { null }

            if (firstName.isBlank()) {
                b.tilFirstName.error = "Введите имя"
                return@setOnClickListener
            }
            b.tilFirstName.error = null

            b.btnSave.isEnabled = false
            lifecycleScope.launch {
                try {
                    val r = App.instance.apiService.updateProfile(
                        UpdateProfileRequest(firstName, lastName, phone)
                    )
                    if (r.isSuccessful) {
                        val body = r.body()!!
                        App.instance.tokenStorage.saveProfile(body.firstName, body.lastName, body.phone)
                        onSaved(body.firstName, body.lastName)
                        Toast.makeText(requireContext(), "Профиль обновлён", Toast.LENGTH_SHORT).show()
                        dismiss()
                    } else {
                        Toast.makeText(requireContext(), "Ошибка сохранения", Toast.LENGTH_SHORT).show()
                        b.btnSave.isEnabled = true
                    }
                } catch (_: Exception) {
                    Toast.makeText(requireContext(), "Нет соединения", Toast.LENGTH_SHORT).show()
                    b.btnSave.isEnabled = true
                }
            }
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}
