package com.fitnesstrainer.app.ui.auth

import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.fitnesstrainer.app.R
import com.fitnesstrainer.app.App
import com.fitnesstrainer.app.data.model.MeasurementRequest
import com.fitnesstrainer.app.data.model.SaveClientProfileRequest
import com.fitnesstrainer.app.data.model.SendCodeRequest
import com.fitnesstrainer.app.data.model.VerifyCodeRequest
import com.fitnesstrainer.app.databinding.FragmentEmailVerificationBinding
import com.fitnesstrainer.app.util.toUserMessage
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class EmailVerificationFragment : Fragment() {

    private var _b: FragmentEmailVerificationBinding? = null
    private val b get() = _b!!

    private var email    = ""
    private var role     = ""
    private var weightKg = -1f
    private var heightCm = -1f
    private var gender   = ""
    private var resendTimer: CountDownTimer? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _b = FragmentEmailVerificationBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        email    = arguments?.getString("email")   ?: ""
        role     = arguments?.getString("role")    ?: ""
        weightKg = arguments?.getFloat("weightKg") ?: -1f
        heightCm = arguments?.getFloat("heightCm") ?: -1f
        gender   = arguments?.getString("gender")  ?: ""

        b.tvEmailHint.text = "Мы отправили 6-значный код на\n$email"


        b.btnVerify.setOnClickListener { verify() }
        b.tvResend.setOnClickListener  { resend()  }

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    AlertDialog.Builder(requireContext())
                        .setTitle("Отменить регистрацию?")
                        .setMessage("Код подтверждения не введён. Аккаунт не будет создан.")
                        .setPositiveButton("Выйти") { _, _ ->
                            isEnabled = false
                            findNavController().navigate(
                                R.id.loginFragment,
                                null,
                                androidx.navigation.NavOptions.Builder()
                                    .setPopUpTo(R.id.loginFragment, true)
                                    .build()
                            )
                        }
                        .setNegativeButton("Остаться", null)
                        .show()
                }
            }
        )
    }

    private fun verify() {
        val code = b.etCode.text?.toString()?.trim() ?: ""
        if (code.length != 6) {
            showError("Введите 6-значный код")
            return
        }
        setLoading(true)
        lifecycleScope.launch {
            try {
                val app = requireActivity().application as App
                val resp = app.apiService.verifyCode(VerifyCodeRequest(email, code))
                if (resp.isSuccessful) {
                    val body = resp.body()
                    if (body?.accessToken?.isNotEmpty() == true) {
                        // Регистрация — сохраняем токены
                        app.tokenStorage.saveAuth(
                            body.accessToken, body.refreshToken,
                            body.userId, body.role,
                            body.firstName, body.lastName,
                            body.email, body.avatarUrl
                        )
                        // FCM токен
                        try {
                            val fcm = FirebaseMessaging.getInstance().token.await()
                            app.apiService.registerFcmToken(mapOf("deviceToken" to fcm))
                        } catch (_: Exception) {}
                        // Профиль клиента (weight/height/gender из регистрации)
                        if (role == "Client" && (weightKg > 0 || heightCm > 0)) {
                            try {
                                app.apiService.saveClientProfile(
                                    SaveClientProfileRequest(
                                        fitnessGoal   = null,
                                        activityLevel = null,
                                        weightKg      = if (weightKg > 0) weightKg.toDouble() else null,
                                        heightCm      = if (heightCm > 0) heightCm.toDouble() else null,
                                        goalWeightKg  = null,
                                        gender        = gender.ifEmpty { null }
                                    )
                                )
                                app.apiService.addMeasurement(
                                    MeasurementRequest(
                                        weightKg = if (weightKg > 0) weightKg.toDouble() else null,
                                        heightCm = if (heightCm > 0) heightCm.toDouble() else null
                                    )
                                )
                            } catch (_: Exception) {}
                        }
                    }
                    navigateNext(body?.accessToken?.isNotEmpty() == true)
                } else {
                    showError(when (resp.code()) {
                        400 -> "Неверный или истёкший код"
                        else -> "Ошибка. Попробуйте снова"
                    })
                }
            } catch (e: Exception) {
                showError(e.toUserMessage())
            } finally {
                setLoading(false)
            }
        }
    }

    private fun resend() {
        b.tvResend.isEnabled = false
        lifecycleScope.launch {
            try {
                (requireActivity().application as App).apiService.sendVerificationCode(SendCodeRequest(email))
                b.tvError.text = "Новый код отправлен"
                b.tvError.setTextColor(resources.getColor(R.color.accent_indigo, null))
                b.tvError.visibility = View.VISIBLE
            } catch (_: Exception) {}
        }
        resendTimer?.cancel()
        resendTimer = object : CountDownTimer(60_000, 1_000) {
            override fun onTick(ms: Long) {
                b.tvResend.text = "Отправить снова (${ms / 1000}с)"
            }
            override fun onFinish() {
                b.tvResend.isEnabled = true
                b.tvResend.text = "Отправить снова"
            }
        }.start()
    }

    private fun navigateNext(isLoggedIn: Boolean) {
        if (role == "Client" && isLoggedIn) {
            val args = android.os.Bundle().apply {
                putString("email",    email)
                putFloat("weightKg",  weightKg)
                putFloat("heightCm",  heightCm)
            }
            findNavController().navigate(
                R.id.actionEmailVerificationToClientOnboarding, args
            )
        } else {
            val dest = if (role == "Trainer")
                R.id.action_emailVerification_to_trainerDashboardFragment
            else
                R.id.loginFragment
            findNavController().navigate(
                dest,
                null,
                androidx.navigation.NavOptions.Builder()
                    .setPopUpTo(R.id.loginFragment, true)
                    .build()
            )
        }
    }

    private fun showError(msg: String) {
        b.tvError.text = msg
        b.tvError.setTextColor(resources.getColor(R.color.accent_rose, null))
        b.tvError.visibility = View.VISIBLE
    }

    private fun setLoading(loading: Boolean) {
        b.progress.visibility  = if (loading) View.VISIBLE else View.GONE
        b.btnVerify.isEnabled  = !loading
    }

    override fun onDestroyView() { resendTimer?.cancel(); super.onDestroyView(); _b = null }
}
