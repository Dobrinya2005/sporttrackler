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
import com.fitnesstrainer.app.data.model.SendCodeRequest
import com.fitnesstrainer.app.databinding.FragmentForgotPasswordBinding
import kotlinx.coroutines.launch

class ForgotPasswordFragment : Fragment() {

    private var _b: FragmentForgotPasswordBinding? = null
    private val b get() = _b!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _b = FragmentForgotPasswordBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        b.btnBack.setOnClickListener { findNavController().popBackStack() }

        b.btnSend.setOnClickListener {
            val email = b.etEmail.text?.toString()?.trim() ?: ""
            if (email.isEmpty()) {
                b.tvError.text = "Введите email"
                b.tvError.visibility = View.VISIBLE
                return@setOnClickListener
            }
            sendCode(email)
        }
    }

    private fun sendCode(email: String) {
        b.tvError.visibility = View.GONE
        b.btnSend.isEnabled = false
        b.progress.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val resp = App.instance.apiService.forgotPassword(SendCodeRequest(email))
                if (resp.isSuccessful) {
                    val args = Bundle().apply { putString("email", email) }
                    findNavController().navigate(R.id.action_forgotPasswordFragment_to_resetPasswordFragment, args)
                } else {
                    val msg = resp.errorBody()?.string()?.let {
                        try { org.json.JSONObject(it).getString("message") } catch (_: Exception) { null }
                    } ?: "Ошибка. Проверьте email"
                    b.tvError.text = msg
                    b.tvError.visibility = View.VISIBLE
                }
            } catch (_: Exception) {
                b.tvError.text = "Нет подключения к серверу"
                b.tvError.visibility = View.VISIBLE
            } finally {
                b.btnSend.isEnabled = true
                b.progress.visibility = View.GONE
            }
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}
