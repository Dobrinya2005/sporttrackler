package com.fitnesstrainer.app.ui.settings

import com.fitnesstrainer.app.ui.chat.ChatThemeManager
import android.Manifest
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.fitnesstrainer.app.App
import com.fitnesstrainer.app.R
import com.fitnesstrainer.app.data.local.ThemeManager
import com.fitnesstrainer.app.databinding.FragmentSettingsBinding
import com.fitnesstrainer.app.service.StepCounterService
import com.fitnesstrainer.app.service.WorkoutReminderReceiver
import com.fitnesstrainer.app.util.BiometricHelper
import com.fitnesstrainer.app.util.SoundManager
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream

class SettingsFragment : Fragment() {

    private var _b: FragmentSettingsBinding? = null
    private val b get() = _b!!

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri ?: return@registerForActivityResult
        uploadAvatar(uri)
    }

    private val requestActivityPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startStepCounter()
        else {
            b.switchSteps.isChecked = false
            Toast.makeText(requireContext(), "Нет разрешения на подсчёт шагов", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _b = FragmentSettingsBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadProfile()
        setupTrainerSections()

        // Тема
        val isDark = ThemeManager.isDark(requireContext())
        b.switchTheme.isChecked = isDark
        updateThemeUI(isDark)
        b.switchTheme.setOnCheckedChangeListener { _, checked ->
            SoundManager.playClick(b.switchTheme)
            ThemeManager.toggle(requireContext())
            updateThemeUI(checked)
            activity?.window?.decorView?.post { activity?.recreate() }
        }
        b.rowTheme.setOnClickListener { b.switchTheme.toggle() }

        // Аватар
        b.btnChangeAvatar.setOnClickListener { pickImage.launch("image/*") }

        // Редактирование профиля
        b.btnEditProfile.setOnClickListener { showEditProfileDialog() }

        // Биометрия
        lifecycleScope.launch {
            val biometricEnabled = App.instance.tokenStorage.isBiometricEnabled()
            b.switchBiometric.isChecked = biometricEnabled
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
                    onError   = {
                        b.switchBiometric.isChecked = false
                    }
                )
            } else {
                lifecycleScope.launch { App.instance.tokenStorage.setBiometricEnabled(false) }
            }
        }
        b.rowBiometric.setOnClickListener { b.switchBiometric.toggle() }

        // PIN-код
        lifecycleScope.launch {
            val hasPin = App.instance.tokenStorage.hasPinSet()
            b.switchPin.isChecked = hasPin
            b.tvPinStatus.text = if (hasPin) "Включён" else "Выключен"
        }
        // Отключаем прямые клики на свитч — всё идёт через строку
        b.switchPin.isClickable = false
        b.switchPin.isFocusable = false
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

        // Шагомер
        b.switchSteps.setOnCheckedChangeListener { _, checked ->
            if (checked) checkAndStartStepCounter()
            else StepCounterService.stop(requireContext())
        }
        b.rowSteps.setOnClickListener { b.switchSteps.toggle() }

        // Напоминания
        lifecycleScope.launch {
            val ts      = App.instance.tokenStorage
            val enabled = ts.isWorkoutReminderEnabled()
            val h       = ts.getReminderHour()
            val m       = ts.getReminderMinute()
            b.switchReminder.isChecked = enabled
            b.tvReminderTime.text = if (enabled) "%02d:%02d".format(h, m) else "Выкл"
        }
        b.switchReminder.setOnCheckedChangeListener { _, checked ->
            if (checked) showTimePicker()
            else {
                lifecycleScope.launch {
                    App.instance.tokenStorage.setWorkoutReminder(false)
                    b.tvReminderTime.text = "Выкл"
                    WorkoutReminderReceiver.cancel(requireContext())
                }
            }
        }
        b.rowReminder.setOnClickListener {
            if (b.switchReminder.isChecked) showTimePicker()
            else b.switchReminder.toggle()
        }

        // Тема чата
        setupChatThemePicker()

        // Выход
        b.rowLogout.setOnClickListener {
            SoundManager.playClick(it)
            lifecycleScope.launch {
                App.instance.tokenStorage.clearAuth()
                App.instance.tokenStorage.clearPin()
                val nav = androidx.navigation.Navigation.findNavController(b.root)
                nav.navigate(R.id.loginFragment,
                    null,
                    androidx.navigation.NavOptions.Builder()
                        .setPopUpTo(R.id.nav_graph, inclusive = true)
                        .setLaunchSingleTop(true)
                        .build()
                )
            }
        }
    }

    private fun setupChatThemePicker() {
        val ctx = requireContext()
        fun applyTheme(t: ChatThemeManager.Theme) {
            ChatThemeManager.save(ctx, t)
            b.tvCurrentTheme.text = t.label
        }
        // show current
        b.tvCurrentTheme.text = ChatThemeManager.load(ctx).label

        b.themeDefault.setOnClickListener { applyTheme(ChatThemeManager.Theme.DEFAULT) }
        b.themeNavy.setOnClickListener   { applyTheme(ChatThemeManager.Theme.NAVY) }
        b.themeForest.setOnClickListener { applyTheme(ChatThemeManager.Theme.FOREST) }
        b.themeSunset.setOnClickListener { applyTheme(ChatThemeManager.Theme.SUNSET) }
        b.themeGalaxy.setOnClickListener { applyTheme(ChatThemeManager.Theme.GALAXY) }
        b.themeOcean.setOnClickListener  { applyTheme(ChatThemeManager.Theme.OCEAN) }
    }

    private fun setupTrainerSections() {
        lifecycleScope.launch {
            val role = App.instance.tokenStorage.getUserRole()
            if (role == "Trainer") {
                b.labelTrainerSections.visibility = View.VISIBLE
                b.cardTrainerSections.visibility  = View.VISIBLE
                b.rowFood.setOnClickListener {
                    findNavController().navigate(R.id.foodDiaryFragment, null,
                        NavOptions.Builder().setLaunchSingleTop(true).build())
                }
                b.rowWorkouts.setOnClickListener {
                    findNavController().navigate(R.id.workoutPlanFragment, null,
                        NavOptions.Builder().setLaunchSingleTop(true).build())
                }
            }
        }
    }

    private fun checkAndStartStepCounter() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACTIVITY_RECOGNITION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestActivityPermission.launch(Manifest.permission.ACTIVITY_RECOGNITION)
        } else {
            startStepCounter()
        }
    }

    private fun startStepCounter() {
        StepCounterService.start(requireContext())
        Toast.makeText(requireContext(), "Шагомер запущен", Toast.LENGTH_SHORT).show()
    }

    private fun showTimePicker() {
        lifecycleScope.launch {
            val h = App.instance.tokenStorage.getReminderHour()
            val m = App.instance.tokenStorage.getReminderMinute()
            TimePickerDialog(requireContext(), { _, hour, minute ->
                lifecycleScope.launch {
                    App.instance.tokenStorage.setWorkoutReminder(true, hour, minute)
                    b.switchReminder.isChecked = true
                    b.tvReminderTime.text = "%02d:%02d".format(hour, minute)
                    WorkoutReminderReceiver.schedule(requireContext(), hour, minute)
                    Toast.makeText(requireContext(), "Напоминание установлено на %02d:%02d".format(hour, minute), Toast.LENGTH_SHORT).show()
                }
            }, h, m, true).show()
        }
    }

    private fun loadProfile() {
        lifecycleScope.launch {
            try {
                val tokenStorage = App.instance.tokenStorage
                val firstName = tokenStorage.getFirstName() ?: ""
                val lastName  = tokenStorage.getLastName()  ?: ""
                val role      = tokenStorage.getUserRole()  ?: ""
                val avatarUrl = tokenStorage.getAvatarUrl()

                b.tvUserName.text = "$firstName $lastName".trim()
                b.tvUserRole.text = if (role == "Trainer") "Тренер" else "Клиент"

                if (!avatarUrl.isNullOrBlank()) {
                    Glide.with(this@SettingsFragment)
                        .load(avatarUrl)
                        .placeholder(R.drawable.ic_person)
                        .centerCrop()
                        .into(b.ivAvatar)
                }
            } catch (_: Exception) {}
        }
    }

    private fun uploadAvatar(uri: Uri) {
        lifecycleScope.launch {
            try {
                val file = uriToFile(uri) ?: return@launch
                val part = MultipartBody.Part.createFormData(
                    "file", file.name,
                    file.asRequestBody("image/*".toMediaTypeOrNull())
                )
                val response = App.instance.apiService.uploadAvatar(part)
                if (response.isSuccessful) {
                    val url = response.body()?.get("avatarUrl") ?: return@launch
                    App.instance.tokenStorage.saveAvatarUrl(url)
                    Glide.with(this@SettingsFragment).load(url).centerCrop().into(b.ivAvatar)
                    Toast.makeText(requireContext(), "Фото обновлено!", Toast.LENGTH_SHORT).show()
                }
            } catch (_: Exception) {
                Toast.makeText(requireContext(), "Ошибка загрузки фото", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun uriToFile(uri: Uri): File? {
        return try {
            val ctx   = requireContext()
            val input = ctx.contentResolver.openInputStream(uri) ?: return null
            val file  = File(ctx.cacheDir, "avatar_${System.currentTimeMillis()}.jpg")
            FileOutputStream(file).use { input.copyTo(it) }
            file
        } catch (_: Exception) { null }
    }

    private fun showEditProfileDialog() {
        EditProfileSheet { firstName, lastName ->
            b.tvUserName.text = "$firstName $lastName".trim()
        }.show(parentFragmentManager, "edit_profile")
    }

    private fun updateThemeUI(isDark: Boolean) {
        if (isDark) {
            b.ivThemeIcon.setImageResource(R.drawable.ic_moon)
            b.tvThemeLabel.text    = "Тёмная тема"
            b.tvThemeSublabel.text = "Переключить на светлую"
        } else {
            b.ivThemeIcon.setImageResource(R.drawable.ic_sun)
            b.tvThemeLabel.text    = "Светлая тема"
            b.tvThemeSublabel.text = "Переключить на тёмную"
        }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            val hasPin = App.instance.tokenStorage.hasPinSet()
            _b?.switchPin?.isChecked = hasPin
            _b?.tvPinStatus?.text = if (hasPin) "Включён" else "Выключен"
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}
