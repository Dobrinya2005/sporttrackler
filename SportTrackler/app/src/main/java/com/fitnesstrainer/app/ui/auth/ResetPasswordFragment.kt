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
import com.fitnesstrainer.app.databinding.FragmentResetPasswordBinding
import kotlinx.coroutines.launch

class ResetPasswordFragment : Fragment() {

    private var _b: FragmentResetPasswordBinding? = null
    private val b get() = _b!!

    private var email = ""
    private var code  = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _b = FragmentResetPasswordBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        email = arguments?.getString("email") ?: ""
        code  = arguments?.getString("code")  ?: ""

        b.btnBack.setOnClickListener { findNavController().popBackStack() }
        b.btnReset.setOnClickListener { attemptReset() }
    }

    private fun attemptReset() {
        val password = b.etPassword.text?.toString() ?: ""
        val confirm  = b.etConfirm.text?.toString() ?: ""
        val error = validatePassword(password)
        when {
            error != null       -> showError(error)
            password != confirm -> showError("Пароли не совпадают")
            else                -> resetPassword(password)
        }
    }

    private fun validatePassword(pwd: String): String? = when {
        pwd.length < 8                     -> "Пароль должен содержать минимум 8 символов"
        !pwd.any { it.isUpperCase() }      -> "Пароль должен содержать хотя бы одну заглавную букву"
        !pwd.any { it.isLowerCase() }      -> "Пароль должен содержать хотя бы одну строчную букву"
        !pwd.any { !it.isLetterOrDigit() } -> "Пароль должен содержать хотя бы один специальный символ (!@#\$...)"
        else                               -> null
    }

    private fun resetPassword(newPassword: String) {
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
                    } ?: "Не удалось сменить пароль"
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

    private fun showError(msg: String) {
        b.tvError.text = msg
        b.tvError.visibility = View.VISIBLE
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}
