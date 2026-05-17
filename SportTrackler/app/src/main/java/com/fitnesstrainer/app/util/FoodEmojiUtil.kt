package com.fitnesstrainer.app.util

object FoodEmojiUtil {

    fun getEmoji(name: String): String = when {
        name.contains("молок", true) -> "🥛"
        name.contains("творог", true) -> "🧈"
        name.contains("йогурт", true) -> "🫙"
        name.contains("кефир", true) -> "🥛"
        name.contains("сыр", true) -> "🧀"

        name.contains("курин", true) || name.contains("индейк", true) -> "🍗"
        name.contains("говядин", true) -> "🥩"
        name.contains("лосось", true) -> "🐟"
        name.contains("тунец", true) -> "🐠"
        name.contains("яйц", true) -> "🥚"

        name.contains("рис", true) -> "🍚"
        name.contains("овсянк", true) -> "🥣"
        name.contains("гречк", true) -> "🌾"
        name.contains("макарон", true) -> "🍝"
        name.contains("перлов", true) -> "🌾"

        name.contains("фасоль", true) -> "🫘"
        name.contains("чечевиц", true) -> "🫘"
        name.contains("нут", true) -> "🫘"

        name.contains("брокколи", true) -> "🥦"
        name.contains("огурец", true) -> "🥒"
        name.contains("помидор", true) -> "🍅"
        name.contains("капуст", true) -> "🥬"
        name.contains("картофел", true) -> "🥔"
        name.contains("морков", true) -> "🥕"

        name.contains("банан", true) -> "🍌"
        name.contains("яблок", true) -> "🍎"
        name.contains("апельсин", true) -> "🍊"
        name.contains("виноград", true) -> "🍇"

        name.contains("миндаль", true) -> "🌰"
        name.contains("грецк", true) -> "🥜"
        name.contains("арахис", true) -> "🥜"

        name.contains("протеин", true) -> "💪"
        name.contains("батончик", true) -> "🍫"

        name.contains("масло", true) -> "🫒"

        name.contains("хлеб", true) -> "🍞"

        else -> "🍽️"
    }
}
