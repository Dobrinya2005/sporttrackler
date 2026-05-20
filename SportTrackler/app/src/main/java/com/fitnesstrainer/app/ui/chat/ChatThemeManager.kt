package com.fitnesstrainer.app.ui.chat

import android.content.Context
import android.content.SharedPreferences

object ChatThemeManager {

    private const val PREFS = "chat_theme_prefs"
    private const val KEY   = "selected_theme"

    enum class Theme(val id: String, val label: String, val colorHex: String) {
        DEFAULT ("default", "Тёмная",    "#080B14"),
        NAVY    ("navy",    "Синяя",     "#0A1628"),
        FOREST  ("forest",  "Лес",       "#081409"),
        SUNSET  ("sunset",  "Закат",     "#1A0A0E"),
        GALAXY  ("galaxy",  "Галактика", "#0E0A1A"),
        OCEAN   ("ocean",   "Океан",     "#071520"),
    }

    fun save(ctx: Context, theme: Theme) =
        prefs(ctx).edit().putString(KEY, theme.id).apply()

    fun load(ctx: Context): Theme =
        Theme.entries.find { it.id == prefs(ctx).getString(KEY, Theme.DEFAULT.id) }
            ?: Theme.DEFAULT

    private fun prefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
