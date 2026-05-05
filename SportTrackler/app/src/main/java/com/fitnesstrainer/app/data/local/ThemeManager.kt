package com.fitnesstrainer.app.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import com.fitnesstrainer.app.R

object ThemeManager {

    private const val PREFS_NAME = "theme_prefs"
    private const val KEY_THEME = "selected_theme"

    const val THEME_DEFAULT = "default"
    const val THEME_SOLARIZED_DARK = "solarized_dark"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getTheme(context: Context): String =
        prefs(context).getString(KEY_THEME, THEME_SOLARIZED_DARK) ?: THEME_SOLARIZED_DARK

    fun setTheme(context: Context, theme: String) {
        prefs(context).edit().putString(KEY_THEME, theme).apply()
    }

    fun applyTheme(activity: AppCompatActivity) {
        when (getTheme(activity)) {
            THEME_SOLARIZED_DARK -> activity.setTheme(R.style.Theme_FitnessTrainer_SolarizedDark)
            else -> activity.setTheme(R.style.Theme_FitnessTrainer)
        }
    }

    fun isSolarizedDark(context: Context) = getTheme(context) == THEME_SOLARIZED_DARK
}
