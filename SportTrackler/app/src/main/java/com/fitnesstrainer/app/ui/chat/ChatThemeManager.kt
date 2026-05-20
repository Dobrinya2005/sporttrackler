package com.fitnesstrainer.app.ui.chat

import android.content.Context
import android.graphics.Color

object ChatThemeManager {

    private const val PREFS = "chat_theme_per_contact"

    enum class Theme(
        val id: String,
        val label: String,
        val bgRes: Int,
        val sentColor: Int,
        val receivedColor: Int
    ) {
        DEFAULT("default", "Тёмная",
            com.fitnesstrainer.app.R.drawable.chat_bg_default,
            Color.parseColor("#3D4EC8"), Color.parseColor("#1A2540")),
        NAVY("navy", "Синяя",
            com.fitnesstrainer.app.R.drawable.chat_bg_navy,
            Color.parseColor("#1E3FAF"), Color.parseColor("#0D1E46")),
        FOREST("forest", "Лес",
            com.fitnesstrainer.app.R.drawable.chat_bg_forest,
            Color.parseColor("#1A6040"), Color.parseColor("#0A1F14")),
        SUNSET("sunset", "Закат",
            com.fitnesstrainer.app.R.drawable.chat_bg_sunset,
            Color.parseColor("#7C2030"), Color.parseColor("#250810")),
        GALAXY("galaxy", "Галактика",
            com.fitnesstrainer.app.R.drawable.chat_bg_galaxy,
            Color.parseColor("#5B1FA8"), Color.parseColor("#180A30")),
        OCEAN("ocean", "Океан",
            com.fitnesstrainer.app.R.drawable.chat_bg_ocean,
            Color.parseColor("#0A6080"), Color.parseColor("#071525")),
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
