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
import com.fitnesstrainer.app.databinding.FragmentPinBinding
import com.fitnesstrainer.app.util.BiometricHelper
import kotlinx.coroutines.launch
import java.security.MessageDigest

class PinFragment : Fragment() {

    private var _binding: FragmentPinBinding? = null
    private val binding get() = _binding!!

    enum class Mode { SETUP, ENTRY, DISABLE }

    private var mode = Mode.SETUP
    private val entered = StringBuilder()
    private var firstPin = ""
    private var isConfirming = false
    private var role = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPinBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupNumpad()
        binding.btnBiometric.setOnClickListener { tryBiometric() }
        binding.tvForgotPin.setOnClickListener { logout() }
        binding.tvSkipPin.setOnClickListener {
            lifecycleScope.launch {
                App.instance.tokenStorage.setPinOffered(true)
                navigateToDashboard()
            }
        }

        // Загружаем режим асинхронно, но UI уже готов
        val purpose = arguments?.getString("purpose") ?: "default"
        lifecycleScope.launch {
            val storage = App.instance.tokenStorage
            role = storage.getUserRole() ?: "Client"
            mode = when {
                purpose == "disable_pin" -> Mode.DISABLE
                storage.hasPinSet() -> Mode.ENTRY
                else -> Mode.SETUP
            }
            applyMode(storage.isBiometricEnabled())
        }
    }

    private fun applyMode(biometricEnabled: Boolean) {
        when (mode) {
            Mode.SETUP -> {
                binding.tvPinTitle.text = "Создайте PIN-код"
                binding.tvPinSubtitle.text = "Введите 4-значный PIN для входа в приложение"
                binding.tvSkipPin.visibility = View.VISIBLE
                binding.tvForgotPin.visibility = View.GONE
                binding.btnBiometric.visibility = View.GONE
            }
            Mode.ENTRY -> {
                binding.tvPinTitle.text = "Введите PIN-код"
                binding.tvPinSubtitle.text = "Используйте ваш PIN для входа в приложение"
                binding.tvSkipPin.visibility = View.GONE
                binding.tvForgotPin.visibility = View.VISIBLE
                if (biometricEnabled && BiometricHelper.isAvailable(this)) {
                    binding.btnBiometric.visibility = View.VISIBLE
                }
            }
            Mode.DISABLE -> {
                binding.tvPinTitle.text = "Подтвердите PIN-код"
                binding.tvPinSubtitle.text = "Введите текущий PIN для отключения защиты"
                binding.tvSkipPin.visibility = View.GONE
                binding.tvForgotPin.visibility = View.GONE
                binding.btnBiometric.visibility = View.GONE
            }
        }
    }

    private fun setupNumpad() {
        val keys = mapOf(
            binding.btn0 to "0", binding.btn1 to "1", binding.btn2 to "2",
            binding.btn3 to "3", binding.btn4 to "4", binding.btn5 to "5",
            binding.btn6 to "6", binding.btn7 to "7", binding.btn8 to "8",
            binding.btn9 to "9"
        )
        keys.forEach { (btn, digit) -> btn.setOnClickListener { appendDigit(digit) } }
        binding.btnDelete.setOnClickListener { deleteDigit() }
    }

    private fun appendDigit(d: String) {
        if (entered.length >= 4) return
        entered.append(d)
        updateDots()
        if (entered.length == 4) onPinComplete()
    }

    private fun deleteDigit() {
        if (entered.isEmpty()) return
        entered.deleteCharAt(entered.length - 1)
        updateDots()
        binding.tvPinError.visibility = View.GONE
    }

    private fun updateDots() {
        val dots = listOf(binding.dot1, binding.dot2, binding.dot3, binding.dot4)
        dots.forEachIndexed { i, dot ->
            dot.setBackgroundResource(
                if (i < entered.length) R.drawable.pin_dot_filled else R.drawable.pin_dot_empty
            )
        }
    }

    private fun onPinComplete() {
        when (mode) {
            Mode.SETUP   -> handleSetup()
            Mode.ENTRY   -> handleEntry()
            Mode.DISABLE -> handleDisable()
        }
    }

    private fun handleSetup() {
        if (!isConfirming) {
            firstPin = entered.toString()
            entered.clear()
            updateDots()
            isConfirming = true
            binding.tvPinTitle.text = "Подтвердите PIN-код"
            binding.tvPinSubtitle.text = "Введите PIN ещё раз для подтверждения"
            binding.tvSkipPin.visibility = View.GONE
            binding.tvPinError.visibility = View.GONE
        } else {
            if (entered.toString() == firstPin) {
                savePin(entered.toString())
            } else {
                binding.tvPinError.text = "PIN-коды не совпадают. Попробуйте снова"
                binding.tvPinError.visibility = View.VISIBLE
                entered.clear()
                firstPin = ""
                isConfirming = false
                updateDots()
                binding.tvPinTitle.text = "Создайте PIN-код"
                binding.tvPinSubtitle.text = "Введите 4-значный PIN для входа в приложение"
                binding.tvSkipPin.visibility = View.VISIBLE
            }
        }
    }

    private fun handleEntry() {
        lifecycleScope.launch {
            val stored = App.instance.tokenStorage.getPinHash()
            if (hash(entered.toString()) == stored) {
                navigateToDashboard()
            } else {
                binding.tvPinError.text = "Неверный PIN-код"
                binding.tvPinError.visibility = View.VISIBLE
                entered.clear()
                updateDots()
            }
        }
    }

    private fun handleDisable() {
        lifecycleScope.launch {
            val stored = App.instance.tokenStorage.getPinHash()
            if (hash(entered.toString()) == stored) {
                App.instance.tokenStorage.clearPin()
                try {
                    findNavController().popBackStack(R.id.settingsFragment, false)
                } catch (_: Exception) {}
            } else {
                binding.tvPinError.text = "Неверный PIN-код"
                binding.tvPinError.visibility = View.VISIBLE
                entered.clear()
                updateDots()
            }
        }
    }

    private fun savePin(pin: String) {
        lifecycleScope.launch {
            App.instance.tokenStorage.savePinHash(hash(pin))
            App.instance.tokenStorage.setPinOffered(true)
            offerBiometric()
        }
    }

    private fun offerBiometric() {
        if (!BiometricHelper.isAvailable(this)) {
            navigateToDashboard()
            return
        }
        lifecycleScope.launch {
            if (App.instance.tokenStorage.isBiometricEnabled()) {
                navigateToDashboard()
                return@launch
            }
            android.app.AlertDialog.Builder(requireContext())
                .setTitle("Вход по биометрии")
                .setMessage("Хотите использовать отпечаток пальца или Face ID для входа?")
                .setPositiveButton("Включить") { _, _ ->
                    BiometricHelper.show(
                        fragment  = this,
                        title     = "Настройка биометрии",
                        subtitle  = "Отсканируйте отпечаток пальца или подтвердите Face ID",
                        onSuccess = {
                            lifecycleScope.launch {
                                App.instance.tokenStorage.setBiometricEnabled(true)
                                navigateToDashboard()
                            }
                        },
                        onError   = { _ -> navigateToDashboard() }
                    )
                }
                .setNegativeButton("Пропустить") { _, _ -> navigateToDashboard() }
                .setCancelable(false)
                .show()
        }
    }

    private fun tryBiometric() {
        BiometricHelper.show(
            fragment  = this,
            title     = "Вход в SportTrackler",
            subtitle  = "Подтвердите биометрией",
            onSuccess = { navigateToDashboard() },
            onError   = { msg -> Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show() }
        )
    }

    private fun navigateToDashboard() {
        val b = _binding ?: return
        com.fitnesstrainer.app.ui.MainActivity.sessionAuthenticated = true
        (requireActivity() as? com.fitnesstrainer.app.ui.MainActivity)?.onUserLoggedIn(role)
        val action = when (role) {
            "Trainer" -> R.id.action_pinFragment_to_trainerDashboardFragment
            "Admin"   -> R.id.action_pinFragment_to_adminDashboardFragment
            else      -> R.id.action_pinFragment_to_clientDashboardFragment
        }
        try { findNavController().navigate(action) } catch (_: Exception) {}
    }

    private fun logout() {
        com.fitnesstrainer.app.ui.MainActivity.sessionAuthenticated = false
        lifecycleScope.launch {
            App.instance.tokenStorage.clearAuth()
            App.instance.tokenStorage.clearPin()
        }
        try {
            findNavController().navigate(
                R.id.loginFragment, null,
                androidx.navigation.NavOptions.Builder()
                    .setPopUpTo(R.id.nav_graph, true)
                    .setLaunchSingleTop(true)
                    .build()
            )
        } catch (_: Exception) {}
    }

    private fun hash(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
