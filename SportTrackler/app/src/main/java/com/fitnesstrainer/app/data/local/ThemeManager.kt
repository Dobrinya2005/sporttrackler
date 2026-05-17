package com.fitnesstrainer.app.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import com.fitnesstrainer.app.R

object ThemeManager {

    private const val PREFS_NAME = "theme_prefs"
    private const val KEY_THEME = "selected_theme"

    const val THEME_DARK  = "dark"
    const val THEME_LIGHT = "light"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getTheme(context: Context): String =
        prefs(context).getString(KEY_THEME, THEME_DARK) ?: THEME_DARK

    fun setTheme(context: Context, theme: String) {
        prefs(context).edit().putString(KEY_THEME, theme).apply()
    }

    fun isDark(context: Context) = getTheme(context) != THEME_LIGHT

    fun applyTheme(activity: AppCompatActivity) {
        when (getTheme(activity)) {
            THEME_LIGHT -> activity.setTheme(R.style.Theme_FitnessTrainer_Light)
            else        -> activity.setTheme(R.style.Theme_FitnessTrainer)
        }
    }

    fun toggle(context: Context) {
        setTheme(context, if (isDark(context)) THEME_LIGHT else THEME_DARK)
    }
}
