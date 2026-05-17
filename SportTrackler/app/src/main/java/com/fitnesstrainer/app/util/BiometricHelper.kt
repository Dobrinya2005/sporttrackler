package com.fitnesstrainer.app.util

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

object BiometricHelper {

    fun isAvailable(fragment: Fragment): Boolean {
        val bm = BiometricManager.from(fragment.requireContext())
        return bm.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL
        ) == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun show(
        fragment: Fragment,
        title: String = "Вход в SportTrackler",
        subtitle: String = "Подтвердите биометрией",
        onSuccess: () -> Unit,
        onError: (String) -> Unit = {}
    ) {
        val executor = ContextCompat.getMainExecutor(fragment.requireContext())
        val prompt = BiometricPrompt(fragment, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onSuccess()
            }
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                if (errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                    errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                    onError(errString.toString())
                }
            }
            override fun onAuthenticationFailed() {}
        })

        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()

        prompt.authenticate(info)
    }
}
