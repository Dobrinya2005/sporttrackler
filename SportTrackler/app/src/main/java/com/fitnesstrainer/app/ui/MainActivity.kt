package com.fitnesstrainer.app.ui

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import com.fitnesstrainer.app.App
import com.fitnesstrainer.app.R
import com.fitnesstrainer.app.data.local.ThemeManager
import com.fitnesstrainer.app.databinding.ActivityMainBinding
import com.fitnesstrainer.app.ui.widget.GlassBottomNav
import com.fitnesstrainer.app.util.SoundManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private var isTrainerMode = false

    companion object {
        // Флаг живёт в памяти процесса — сбрасывается только при kill, не при recreate()
        var sessionAuthenticated = false
    }

    private val noNavDestinations = setOf(
        R.id.loginFragment,
        R.id.registerFragment,
        R.id.pinFragment,
        R.id.chatFragment,
        R.id.groupChatFragment,
        R.id.createGroupFragment,
        R.id.createGroupNameFragment,
        R.id.emailVerificationFragment,
        R.id.clientOnboardingFragment,
        R.id.onboardingMeasurementsFragment,
        R.id.onboardingPhotosFragment,
        R.id.trainerSelectionFragment,
        R.id.clientDetailFragment,
        R.id.qrScanFragment,
        R.id.adminDashboardFragment,
        R.id.adminReviewsFragment,
        R.id.adminSettingsFragment,
        R.id.forgotPasswordFragment,
        R.id.resetPasswordCodeFragment,
        R.id.resetPasswordFragment
    )

    // Время ухода в фон; PIN запрашивается если прошло > 30 сек
    private var backgroundedAt = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applyTheme(this)
        super.onCreate(savedInstanceState)

        enableHighRefreshRate()
        SoundManager.init(this)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHost = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHost.navController

        setupBottomNavigation()
        checkAutoLogin()
        observeNetwork()
        requestNotificationPermission()

        if (intent.getBooleanExtra("session_expired", false)) {
            android.widget.Toast.makeText(this, "Сессия истекла, войдите снова", android.widget.Toast.LENGTH_LONG).show()
            navController.navigate(R.id.loginFragment)
        }

        handleNotificationIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleNotificationIntent(intent)
    }

    private fun handleNotificationIntent(intent: Intent) {
        val userId = intent.getIntExtra("open_chat_user_id", -1)
        val userName = intent.getStringExtra("open_chat_user_name") ?: return
        if (userId == -1) return
        lifecycleScope.launch {
            // Ждём пока nav граф будет готов
            navController.currentDestination ?: return@launch
            val args = android.os.Bundle().apply {
                putInt("contactId", userId)
                putString("contactName", userName)
            }
            try {
                navController.navigate(R.id.chatFragment, args)
            } catch (_: Exception) {}
        }
    }

    private fun enableHighRefreshRate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            display?.supportedModes
                ?.maxByOrNull { it.refreshRate }
                ?.let { mode ->
                    window.attributes = window.attributes.apply {
                        preferredDisplayModeId = mode.modeId
                    }
                }
        }
    }

    private fun configureNavForRole(role: String?) {
        isTrainerMode = (role == "Trainer")
        if (isTrainerMode) {
            binding.bottomNav.setItems(listOf(
                GlassBottomNav.NavItem("Главная",   R.drawable.ic_home),
                GlassBottomNav.NavItem("Чаты",      R.drawable.ic_chat),
                GlassBottomNav.NavItem("QR-код",    R.drawable.ic_qr_code),
                GlassBottomNav.NavItem("Настройки", R.drawable.ic_settings)
            ))
        } else {
            binding.bottomNav.setItems(listOf(
                GlassBottomNav.NavItem("Главная",    R.drawable.ic_home),
                GlassBottomNav.NavItem("Чаты",       R.drawable.ic_chat),
                GlassBottomNav.NavItem("Питание",    R.drawable.ic_nutrition),
                GlassBottomNav.NavItem("Тренировки", R.drawable.ic_dumbbell),
                GlassBottomNav.NavItem("Настройки",  R.drawable.ic_settings)
            ))
        }
    }

    private fun setupBottomNavigation() {
        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.id in noNavDestinations) {
                hideBottomNav()
            } else {
                showBottomNav()
                val idx = if (isTrainerMode) {
                    when (destination.id) {
                        R.id.trainerDashboardFragment -> 0
                        R.id.chatsFragment -> 1
                        R.id.trainerQrFragment -> 2
                        R.id.settingsFragment -> 3
                        else -> -1
                    }
                } else {
                    when (destination.id) {
                        R.id.clientDashboardFragment -> 0
                        R.id.chatsFragment -> 1
                        R.id.foodDiaryFragment -> 2
                        R.id.workoutPlanFragment -> 3
                        R.id.settingsFragment -> 4
                        else -> -1
                    }
                }
                if (idx >= 0) binding.bottomNav.setSelected(idx)
            }
        }

        binding.bottomNav.onItemSelected = { idx ->
            val currentId = navController.currentDestination?.id
            if (isTrainerMode) {
                when (idx) {
                    0 -> if (currentId != R.id.trainerDashboardFragment)
                        navController.navigate(R.id.trainerDashboardFragment, null,
                            NavOptions.Builder().setLaunchSingleTop(true).build())
                    1 -> if (currentId != R.id.chatsFragment)
                        navController.navigate(R.id.chatsFragment, null,
                            NavOptions.Builder().setLaunchSingleTop(true).build())
                    2 -> if (currentId != R.id.trainerQrFragment)
                        navController.navigate(R.id.trainerQrFragment, null,
                            NavOptions.Builder().setLaunchSingleTop(true).build())
                    3 -> if (currentId != R.id.settingsFragment)
                        navController.navigate(R.id.settingsFragment, null,
                            NavOptions.Builder().setLaunchSingleTop(true).build())
                }
            } else {
                when (idx) {
                    0 -> if (currentId != R.id.clientDashboardFragment)
                        navController.navigate(R.id.clientDashboardFragment, null,
                            NavOptions.Builder().setLaunchSingleTop(true).build())
                    1 -> if (currentId != R.id.chatsFragment)
                        navController.navigate(R.id.chatsFragment, null,
                            NavOptions.Builder().setLaunchSingleTop(true).build())
                    2 -> if (currentId != R.id.foodDiaryFragment)
                        navController.navigate(R.id.foodDiaryFragment, null,
                            NavOptions.Builder().setLaunchSingleTop(true).build())
                    3 -> if (currentId != R.id.workoutPlanFragment)
                        navController.navigate(R.id.workoutPlanFragment, null,
                            NavOptions.Builder().setLaunchSingleTop(true).build())
                    4 -> if (currentId != R.id.settingsFragment)
                        navController.navigate(R.id.settingsFragment, null,
                            NavOptions.Builder().setLaunchSingleTop(true).build())
                }
            }
        }
    }

    private fun showBottomNav() {
        if (binding.bottomNav.visibility == View.VISIBLE) return
        binding.bottomNav.visibility = View.VISIBLE
        binding.bottomNav.animate()
            .translationY(0f)
            .alpha(1f)
            .setDuration(220)
            .start()
    }

    private fun hideBottomNav() {
        binding.bottomNav.animate()
            .translationY(binding.bottomNav.height.toFloat())
            .alpha(0f)
            .setDuration(180)
            .withEndAction { binding.bottomNav.visibility = View.GONE }
            .start()
    }

    private fun checkAutoLogin() {
        lifecycleScope.launch {
            val storage = App.instance.tokenStorage
            if (!storage.isLoggedIn()) return@launch
            // Если "запомнить меня" не выбрано — выходим из аккаунта при перезапуске
            if (!storage.isRememberMe()) {
                storage.clearAuth()
                return@launch
            }
            val role = storage.getUserRole()
            configureNavForRole(role)
            when {
                // PIN установлен → спрашиваем только если сессия ещё не аутентифицирована
                storage.hasPinSet() && !sessionAuthenticated -> navController.navigate(R.id.pinFragment)
                // PIN ещё не предлагали → предложим (первый вход после авторизации)
                !storage.isPinOffered() && !sessionAuthenticated -> navController.navigate(R.id.pinFragment)
                // PIN пропущен/отключён → сразу на дашборд
                else -> {
                    configureNavForRole(role)
                    val dest = when (role) {
                        "Trainer" -> R.id.trainerDashboardFragment
                        "Admin"   -> R.id.adminDashboardFragment
                        else      -> R.id.clientDashboardFragment
                    }
                    navController.navigate(dest)
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        backgroundedAt = System.currentTimeMillis()
    }

    override fun onResume() {
        super.onResume()
        val elapsed = System.currentTimeMillis() - backgroundedAt
        // Если прошло > 30 сек с момента ухода в фон — снова спрашиваем PIN
        if (backgroundedAt > 0 && elapsed > 30_000) {
            val current = navController.currentDestination?.id
            val dashboards = setOf(
                R.id.clientDashboardFragment, R.id.trainerDashboardFragment,
                R.id.adminDashboardFragment, R.id.foodDiaryFragment,
                R.id.workoutPlanFragment, R.id.settingsFragment,
                R.id.measurementsFragment, R.id.photosFragment,
                R.id.chatFragment, R.id.goalsFragment
            )
            if (current != null && current in dashboards) {
                lifecycleScope.launch {
                    val tokenStorage = App.instance.tokenStorage
                    if (tokenStorage.isLoggedIn() && tokenStorage.hasPinSet()) {  // только если PIN реально установлен
                        try { navController.navigate(R.id.action_global_to_pinFragment) } catch (_: Exception) {}
                    }
                }
            }
        }
        backgroundedAt = 0L
    }

    private fun observeNetwork() {
        lifecycleScope.launch {
            App.instance.networkMonitor.isOnline.collectLatest { online ->
                if (online) {
                    showOnlineBanner()
                } else {
                    showOfflineBanner()
                }
            }
        }
    }

    private fun showOfflineBanner() {
        val banner = binding.offlineBanner
        banner.visibility = View.VISIBLE
        banner.animate()
            .translationY(0f)
            .alpha(1f)
            .setDuration(300)
            .start()
        binding.bannerText.text = getString(R.string.offline_message)
        binding.bannerText.setTextColor(android.graphics.Color.WHITE)
        banner.setBackgroundColor(android.graphics.Color.parseColor("#EEF14054"))
        binding.bannerIcon.setImageResource(R.drawable.ic_no_wifi)
    }

    private fun showOnlineBanner() {
        val banner = binding.offlineBanner
        if (banner.visibility == View.GONE) return
        binding.bannerText.text = getString(R.string.online_message)
        banner.setBackgroundColor(android.graphics.Color.parseColor("#EE00C853"))
        binding.bannerIcon.setImageResource(R.drawable.ic_check_circle)
        banner.postDelayed({
            banner.animate()
                .translationY(banner.height.toFloat())
                .alpha(0f)
                .setDuration(300)
                .withEndAction {
                    banner.visibility = View.GONE
                    banner.translationY = 200f * resources.displayMetrics.density
                    banner.alpha = 1f
                }
                .start()
        }, 1800)
    }

    fun onUserLoggedIn(role: String) {
        configureNavForRole(role)
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }
    }

    fun restartForThemeChange() {
        recreate()
    }
}
