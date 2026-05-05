package com.fitnesstrainer.app.ui

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.fitnesstrainer.app.App
import com.fitnesstrainer.app.R
import com.fitnesstrainer.app.data.local.ThemeManager
import com.fitnesstrainer.app.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    // Fragments where bottom nav is hidden
    private val authDestinations = setOf(
        R.id.loginFragment,
        R.id.registerFragment
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applyTheme(this)
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHost = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHost.navController

        setupBottomNavigation()
        checkAutoLogin()
    }

    private fun setupBottomNavigation() {
        binding.bottomNav.setupWithNavController(navController)

        // Show / hide bottom nav based on destination
        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.id in authDestinations) {
                hideBottomNav()
            } else {
                showBottomNav()
            }

            // Apply slide animation between main tabs
            binding.bottomNav.menu.findItem(destination.id)?.isChecked = true
        }
    }

    private fun showBottomNav() {
        if (binding.bottomNav.visibility == View.VISIBLE) return
        binding.bottomNav.visibility = View.VISIBLE
        binding.bottomNav.animate()
            .translationY(0f)
            .alpha(1f)
            .setDuration(250)
            .start()
    }

    private fun hideBottomNav() {
        binding.bottomNav.animate()
            .translationY(binding.bottomNav.height.toFloat())
            .alpha(0f)
            .setDuration(200)
            .withEndAction { binding.bottomNav.visibility = View.GONE }
            .start()
    }

    private fun checkAutoLogin() {
        lifecycleScope.launch {
            val tokenStorage = App.instance.tokenStorage
            if (tokenStorage.isLoggedIn()) {
                val role = tokenStorage.getUserRole()
                val dest = if (role == "Trainer")
                    R.id.trainerDashboardFragment
                else
                    R.id.clientDashboardFragment
                navController.navigate(dest)
            }
        }
    }

    fun restartForThemeChange() {
        recreate()
    }
}
