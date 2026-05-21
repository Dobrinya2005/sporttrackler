package com.fitnesstrainer.app.ui.admin

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
import com.fitnesstrainer.app.data.local.ThemeManager
import com.fitnesstrainer.app.databinding.FragmentAdminSettingsBinding
import com.fitnesstrainer.app.util.BiometricHelper
import kotlinx.coroutines.launch

class AdminSettingsFragment : Fragment() {

    private var _b: FragmentAdminSettingsBinding? = null
    private val b get() = _b!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _b = FragmentAdminSettingsBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        b.btnBack.setOnClickListener { findNavController().popBackStack() }

        // Тема
        val isDark = ThemeManager.isDark(requireContext())
        b.btnTheme.text = if (isDark) "Тёмная" else "Светлая"
        b.btnTheme.setOnClickListener {
            ThemeManager.toggle(requireContext())
            activity?.window?.decorView?.post { activity?.recreate() }
        }

        // PIN-код
        b.switchPin.isClickable = false
        b.switchPin.isFocusable = false
        lifecycleScope.launch {
            val hasPin = App.instance.tokenStorage.hasPinSet()
            b.switchPin.isChecked = hasPin
            b.tvPinStatus.text = if (hasPin) "Включён" else "Выключен"
        }
        b.rowPin.setOnClickListener {
            lifecycleScope.launch {
                val hasPin = App.instance.tokenStorage.hasPinSet()
                if (hasPin) {
                    val args = android.os.Bundle().apply { putString("purpose", "disable_pin") }
                    findNavController().navigate(R.id.action_global_to_pinFragment, args)
                } else {
                    findNavController().navigate(R.id.action_global_to_pinFragment)
                }
            }
        }

        // Биометрия
        lifecycleScope.launch {
            val enabled = App.instance.tokenStorage.isBiometricEnabled()
            b.switchBiometric.isChecked = enabled
        }
        b.switchBiometric.setOnCheckedChangeListener { _, checked ->
            if (checked && !BiometricHelper.isAvailable(this)) {
                b.switchBiometric.isChecked = false
                Toast.makeText(requireContext(), "Биометрия недоступна на этом устройстве", Toast.LENGTH_SHORT).show()
                return@setOnCheckedChangeListener
            }
            if (checked) {
                BiometricHelper.show(
                    fragment  = this,
                    title     = "Подтвердите включение",
                    subtitle  = "Биометрия будет использоваться при входе",
                    onSuccess = {
                        lifecycleScope.launch { App.instance.tokenStorage.setBiometricEnabled(true) }
                        Toast.makeText(requireContext(), "Биометрический вход включён", Toast.LENGTH_SHORT).show()
                    },
                    onError   = { b.switchBiometric.isChecked = false }
                )
            } else {
                lifecycleScope.launch { App.instance.tokenStorage.setBiometricEnabled(false) }
            }
        }
        b.rowBiometric.setOnClickListener { b.switchBiometric.toggle() }

        // Версия приложения
        try {
            val pInfo = requireContext().packageManager.getPackageInfo(requireContext().packageName, 0)
            b.tvVersion.text = pInfo.versionName
        } catch (_: Exception) {}

        // Выход
        b.btnLogout.setOnClickListener {
            lifecycleScope.launch {
                App.instance.tokenStorage.clearAuth()
                App.instance.tokenStorage.clearPin()
                findNavController().navigate(
                    R.id.loginFragment, null,
                    androidx.navigation.NavOptions.Builder()
                        .setPopUpTo(R.id.nav_graph, true)
                        .setLaunchSingleTop(true).build()
                )
            }
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}
