package com.fitnesstrainer.app.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.fitnesstrainer.app.App
import com.fitnesstrainer.app.R
import com.fitnesstrainer.app.data.local.ThemeManager
import com.fitnesstrainer.app.databinding.FragmentAdminSettingsBinding
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

        val isDark = ThemeManager.isDark(requireContext())
        b.btnTheme.text = if (isDark) "Тёмная" else "Светлая"
        b.btnTheme.setOnClickListener {
            ThemeManager.toggle(requireContext())
            activity?.window?.decorView?.post { activity?.recreate() }
        }

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
