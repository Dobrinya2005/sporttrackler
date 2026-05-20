package com.fitnesstrainer.app.ui.chat

import android.content.Context

object ChatThemeManager {

    private const val PREFS = "chat_theme_per_contact"

    enum class Theme(val id: String, val label: String, val bgRes: Int) {
        DEFAULT ("default", "Тёмная",    com.fitnesstrainer.app.R.drawable.chat_bg_default),
        NAVY    ("navy",    "Синяя",      com.fitnesstrainer.app.R.drawable.chat_bg_navy),
        FOREST  ("forest",  "Лес",        com.fitnesstrainer.app.R.drawable.chat_bg_forest),
        SUNSET  ("sunset",  "Закат",      com.fitnesstrainer.app.R.drawable.chat_bg_sunset),
        GALAXY  ("galaxy",  "Галактика",  com.fitnesstrainer.app.R.drawable.chat_bg_galaxy),
        OCEAN   ("ocean",   "Океан",      com.fitnesstrainer.app.R.drawable.chat_bg_ocean),
    }

    fun save(ctx: Context, contactId: Int, theme: Theme) =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString("theme_$contactId", theme.id).apply()

    fun load(ctx: Context, contactId: Int): Theme =
        Theme.entries.find {
            it.id == ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString("theme_$contactId", Theme.DEFAULT.id)
        } ?: Theme.DEFAULT
}
