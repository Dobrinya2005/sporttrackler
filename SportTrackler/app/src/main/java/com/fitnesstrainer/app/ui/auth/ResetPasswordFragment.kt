package com.fitnesstrainer.app.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.fitnesstrainer.app.App
import com.fitnesstrainer.app.R
import com.fitnesstrainer.app.data.model.ResetPasswordRequest
import com.fitnesstrainer.app.data.model.SendCodeRequest
import com.fitnesstrainer.app.databinding.FragmentResetPasswordBinding
import kotlinx.coroutines.launch

class ResetPasswordFragment : Fragment() {

    private var _b: FragmentResetPasswordBinding? = null
    private val b get() = _b!!

    private var email = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _b = FragmentResetPasswordBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        email = arguments?.getString("email") ?: ""
        b.tvEmailHint.text = "Код отправлен на $email"

        b.btnBack.setOnClickListener { findNavController().popBackStack() }

        b.btnReset.setOnClickListener { attemptReset() }

        b.tvResend.setOnClickListener { resendCode() }
    }

    private fun attemptReset() {
        val code     = b.etCode.text?.toString()?.trim() ?: ""
        val password = b.etPassword.text?.toString() ?: ""
        val confirm  = b.etConfirm.text?.toString() ?: ""

        when {
            code.length != 6 -> showError("Введите 6-значный код")
            password.length < 6 -> showError("Пароль должен содержать минимум 6 символов")
            password != confirm -> showError("Пароли не совпадают")
            else -> resetPassword(code, password)
        }
    }

    private fun resetPassword(code: String, newPassword: String) {
        b.tvError.visibility = View.GONE
        b.btnReset.isEnabled = false
        b.progress.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val resp = App.instance.apiService.resetPassword(
                    ResetPasswordRequest(email, code, newPassword)
                )
                if (resp.isSuccessful) {
                    Toast.makeText(requireContext(), "Пароль успешно изменён", Toast.LENGTH_SHORT).show()
                    findNavController().navigate(
                        R.id.loginFragment, null,
                        androidx.navigation.NavOptions.Builder()
                            .setPopUpTo(R.id.nav_graph, true)
                            .setLaunchSingleTop(true)
                            .build()
                    )
                } else {
                    val msg = resp.errorBody()?.string()?.let {
                        try { org.json.JSONObject(it).getString("message") } catch (_: Exception) { null }
                    } ?: "Неверный или истёкший код"
                    showError(msg)
                }
            } catch (_: Exception) {
                showError("Нет подключения к серверу")
            } finally {
                b.btnReset.isEnabled = true
                b.progress.visibility = View.GONE
            }
        }
    }

    private fun resendCode() {
        lifecycleScope.launch {
            try {
                App.instance.apiService.forgotPassword(SendCodeRequest(email))
                Toast.makeText(requireContext(), "Код отправлен повторно", Toast.LENGTH_SHORT).show()
            } catch (_: Exception) {
                Toast.makeText(requireContext(), "Не удалось отправить код", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showError(msg: String) {
        b.tvError.text = msg
        b.tvError.visibility = View.VISIBLE
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}
