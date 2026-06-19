package com.fitnesstrainer.app.ui.settings

import com.fitnesstrainer.app.ui.chat.ChatThemeManager
import android.Manifest
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.fitnesstrainer.app.databinding.BottomSheetReminderBinding
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
import androidx.appcompat.app.AlertDialog
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
import android.view.animation.DecelerateInterpolator
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import android.app.AlarmManager
import android.content.Intent
import android.provider.Settings
import androidx.activity.result.PickVisualMediaRequest
import java.io.File
import java.io.FileOutputStream

class SettingsFragment : Fragment() {

    private var _b: FragmentSettingsBinding? = null
    private val b get() = _b!!

    private val pickImage = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
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
        animateEntrance()

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
        b.btnChangeAvatar.setOnClickListener { pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }

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
        b.switchSteps.isChecked = isStepServiceRunning()
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
            AlertDialog.Builder(requireContext())
                .setTitle("Выход из аккаунта")
                .setMessage("Вы уверены, что хотите выйти?")
                .setPositiveButton("Выйти") { _, _ ->
                    com.fitnesstrainer.app.ui.MainActivity.sessionAuthenticated = false
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
                .setNegativeButton("Отмена", null)
                .show()
        }
    }

    private fun setupChatThemePicker() {
        val ctx = requireContext()
        fun applyTheme(t: ChatThemeManager.Theme) {
            ChatThemeManager.save(ctx, 0, t)
            b.tvCurrentTheme.text = t.label
        }
        // show current
        b.tvCurrentTheme.text = ChatThemeManager.load(ctx, 0).label

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

    @Suppress("DEPRECATION")
    private fun isStepServiceRunning(): Boolean {
        val am = requireContext().getSystemService(android.content.Context.ACTIVITY_SERVICE)
            as android.app.ActivityManager
        return am.getRunningServices(Int.MAX_VALUE)
            .any { it.service.className == StepCounterService::class.java.name }
    }

    private fun checkAndStartStepCounter() {
        val sensorManager = requireContext().getSystemService(android.content.Context.SENSOR_SERVICE)
            as android.hardware.SensorManager
        if (sensorManager.getDefaultSensor(android.hardware.Sensor.TYPE_STEP_COUNTER) == null) {
            b.switchSteps.isChecked = false
            Toast.makeText(requireContext(), "Шагомер не поддерживается на этом устройстве", Toast.LENGTH_SHORT).show()
            return
        }
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
        val ctx = context ?: return
        lifecycleScope.launch {
            val initH    = App.instance.tokenStorage.getReminderHour()
            val initM    = App.instance.tokenStorage.getReminderMinute()
            val initDays = App.instance.tokenStorage.getReminderDays().toMutableSet()

            val sheet = BottomSheetDialog(ctx)
            val sb = BottomSheetReminderBinding.inflate(layoutInflater)
            sheet.setContentView(sb.root)

            // Days: Calendar.MONDAY=2 .. Calendar.SUNDAY=1
            val dayViews = mapOf(
                java.util.Calendar.MONDAY    to sb.dayMon,
                java.util.Calendar.TUESDAY   to sb.dayTue,
                java.util.Calendar.WEDNESDAY to sb.dayWed,
                java.util.Calendar.THURSDAY  to sb.dayThu,
                java.util.Calendar.FRIDAY    to sb.dayFri,
                java.util.Calendar.SATURDAY  to sb.daySat,
                java.util.Calendar.SUNDAY    to sb.daySun
            )
            val selectedDays = initDays.toMutableSet()

            fun refreshDays() {
                dayViews.forEach { (day, view) ->
                    val active = day in selectedDays
                    view.isSelected = active
                    view.setTextColor(
                        if (active) ctx.getColor(android.R.color.black)
                        else ctx.getColor(R.color.text_hint)
                    )
                }
            }
            refreshDays()

            dayViews.forEach { (day, view) ->
                view.setOnClickListener {
                    if (day in selectedDays) {
                        if (selectedDays.size > 1) selectedDays.remove(day) // min 1 day
                    } else {
                        selectedDays.add(day)
                    }
                    refreshDays()
                }
            }

            // State: editing hours (true) or minutes (false)
            var editingHour = true
            var hourStr = "%02d".format(initH)
            var minStr  = "%02d".format(initM)

            fun refresh() {
                sb.tvHour.text   = hourStr
                sb.tvMinute.text = minStr
                sb.tvHour.setTextColor(
                    if (editingHour) ctx.getColor(R.color.accent_cyan)
                    else ctx.getColor(R.color.text_primary)
                )
                sb.tvMinute.setTextColor(
                    if (!editingHour) ctx.getColor(R.color.accent_cyan)
                    else ctx.getColor(R.color.text_primary)
                )
            }

            // digit buffer for current field (up to 2 chars)
            var buf = hourStr

            fun commitBuf() {
                val v = buf.toIntOrNull() ?: 0
                if (editingHour) hourStr = "%02d".format(v.coerceIn(0, 23))
                else             minStr  = "%02d".format(v.coerceIn(0, 59))
            }

            fun switchField(toHour: Boolean) {
                commitBuf()
                editingHour = toHour
                buf = if (toHour) hourStr else minStr
                refresh()
            }

            fun appendDigit(d: String) {
                buf = if (buf == "00") d else (buf + d).takeLast(2)
                // validate range without committing permanently
                val v = buf.toIntOrNull() ?: 0
                val max = if (editingHour) 23 else 59
                if (v > max) buf = d  // reset to single digit if over max
                if (editingHour) hourStr = buf.padStart(2, '0')
                else             minStr  = buf.padStart(2, '0')
                refresh()
            }

            fun backspace() {
                buf = if (buf.length > 1) buf.dropLast(1) else "0"
                if (editingHour) hourStr = buf.padStart(2, '0')
                else             minStr  = buf.padStart(2, '0')
                refresh()
            }

            refresh()
            sb.tvHour.setOnClickListener   { switchField(true) }
            sb.tvMinute.setOnClickListener { switchField(false) }

            listOf(sb.key0, sb.key1, sb.key2, sb.key3, sb.key4,
                   sb.key5, sb.key6, sb.key7, sb.key8, sb.key9)
                .forEachIndexed { i, tv -> tv.setOnClickListener { appendDigit(i.toString()) } }
            sb.keyBack.setOnClickListener { backspace() }

            sb.btnCancel.setOnClickListener { sheet.dismiss() }
            sb.btnSave.setOnClickListener {
                val hour   = hourStr.toInt().coerceIn(0, 23)
                val minute = minStr.toInt().coerceIn(0, 59)
                val am = ctx.getSystemService(AlarmManager::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
                    Toast.makeText(ctx,
                        "Разрешите точные будильники в настройках приложения",
                        Toast.LENGTH_LONG).show()
                    startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                        Uri.parse("package:${ctx.packageName}")))
                    return@setOnClickListener
                }
                lifecycleScope.launch {
                    App.instance.tokenStorage.setWorkoutReminder(true, hour, minute, selectedDays)
                    b.switchReminder.isChecked = true
                    b.tvReminderTime.text = "%02d:%02d".format(hour, minute)
                    WorkoutReminderReceiver.scheduleForDays(ctx, hour, minute, selectedDays)
                    Toast.makeText(ctx,
                        "Напоминание установлено на %02d:%02d".format(hour, minute),
                        Toast.LENGTH_SHORT).show()
                }
                sheet.dismiss()
            }
            sheet.show()
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

    private fun animateEntrance() {
        val interp = DecelerateInterpolator()
        listOf(b.ivAvatar, b.tvUserName, b.tvUserRole, b.btnEditProfile).forEachIndexed { i, v ->
            v.alpha = 0f
            v.animate().alpha(1f).setDuration(350).setStartDelay(i * 60L).setInterpolator(interp).start()
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}
