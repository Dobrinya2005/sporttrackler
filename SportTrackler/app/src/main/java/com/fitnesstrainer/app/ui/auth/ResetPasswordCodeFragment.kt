package com.fitnesstrainer.app.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.fitnesstrainer.app.App
import com.fitnesstrainer.app.R
import com.fitnesstrainer.app.data.model.SendCodeRequest
import com.fitnesstrainer.app.databinding.FragmentResetPasswordCodeBinding
import kotlinx.coroutines.launch

class ResetPasswordCodeFragment : Fragment() {

    private var _b: FragmentResetPasswordCodeBinding? = null
    private val b get() = _b!!

    private var email = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _b = FragmentResetPasswordCodeBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        email = arguments?.getString("email") ?: ""
        b.tvEmailHint.text = "Мы отправили 6-значный код на\n$email"

        b.btnBack.setOnClickListener { findNavController().popBackStack() }
        b.btnVerify.setOnClickListener { verify() }
        b.tvResend.setOnClickListener { resend() }
    }

    private fun verify() {
        val code = b.etCode.text?.toString()?.trim() ?: ""
        if (code.length != 6) { showError("Введите 6-значный код"); return }
        val args = Bundle().apply {
            putString("email", email)
            putString("code", code)
        }
        findNavController().navigate(R.id.action_resetPasswordCodeFragment_to_resetPasswordFragment, args)
    }

    private fun resend() {
        lifecycleScope.launch {
            try {
                App.instance.apiService.forgotPassword(SendCodeRequest(email))
                b.tvError.text = "Новый код отправлен"
                b.tvError.setTextColor(resources.getColor(R.color.accent_indigo, null))
                b.tvError.visibility = View.VISIBLE
            } catch (_: Exception) {
                showError("Не удалось отправить код")
            }
        }
    }

    private fun showError(msg: String) {
        b.tvError.text = msg
        b.tvError.setTextColor(resources.getColor(R.color.accent_rose, null))
        b.tvError.visibility = View.VISIBLE
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}
