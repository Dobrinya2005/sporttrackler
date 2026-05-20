package com.fitnesstrainer.app.ui.food

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.fitnesstrainer.app.data.model.DiaryEntryResponse
import com.fitnesstrainer.app.data.model.MealSummary
import com.fitnesstrainer.app.databinding.ItemDiaryEntryBinding
import com.fitnesstrainer.app.databinding.ItemMealHeaderBinding
import com.fitnesstrainer.app.util.FoodEmojiUtil
import com.fitnesstrainer.app.util.SoundManager

private const val TYPE_HEADER = 0
private const val TYPE_ENTRY  = 1

sealed class DiaryListItem {
    data class Header(val meal: MealSummary) : DiaryListItem()
    data class Entry(val dto: DiaryEntryResponse) : DiaryListItem()
}

class FoodDiaryAdapter(
    private val onDelete: (Int) -> Unit,
    private val onAddClick: (String) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val items = mutableListOf<DiaryListItem>()

    fun submitMeals(meals: List<MealSummary>) {
        items.clear()
        val mealOrder = listOf("Breakfast", "Lunch", "Dinner", "Snack")
        val mealMap = meals.associateBy { it.mealType }
        for (type in mealOrder) {
            val meal = mealMap[type] ?: MealSummary(type, 0.0, 0.0, 0.0, 0.0, emptyList())
            items.add(DiaryListItem.Header(meal))
            meal.items.forEach { items.add(DiaryListItem.Entry(it)) }
        }
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int) =
        if (items[position] is DiaryListItem.Header) TYPE_HEADER else TYPE_ENTRY

    override fun getItemCount() = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        if (viewType == TYPE_HEADER)
            HeaderVH(ItemMealHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        else
            EntryVH(ItemDiaryEntryBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is DiaryListItem.Header -> (holder as HeaderVH).bind(item.meal)
            is DiaryListItem.Entry  -> (holder as EntryVH).bind(item.dto)
        }
    }

    inner class HeaderVH(private val b: ItemMealHeaderBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(meal: MealSummary) {
            b.tvMealName.text     = mealDisplayName(meal.mealType)
            b.tvMealEmoji.text    = mealEmoji(meal.mealType)
            b.tvMealCalories.text = "%.0f ккал".format(meal.calories)
            b.btnAddFood.setOnClickListener {
                SoundManager.playClick(it)
                onAddClick(meal.mealType)
            }
        }
    }

    inner class EntryVH(private val b: ItemDiaryEntryBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(dto: DiaryEntryResponse) {
            b.tvEmoji.text    = FoodEmojiUtil.getEmoji(dto.product.name)
            b.tvFoodName.text = dto.product.name
            b.tvWeight.text   = "%.0f г".format(dto.weightG)
            b.tvCalories.text = "%.0f".format(dto.calories)
            b.tvMacros.text   = "Б %.1f  Ж %.1f  У %.1f".format(dto.protein, dto.fat, dto.carbs)
            b.btnDelete.setOnClickListener {
                SoundManager.playDelete()
                onDelete(dto.diaryId)
            }
        }
    }

    private fun mealDisplayName(type: String) = when (type.lowercase()) {
        "breakfast" -> "Завтрак"
        "lunch"     -> "Обед"
        "dinner"    -> "Ужин"
        "snack"     -> "Перекус"
        else        -> type
    }

    private fun mealEmoji(type: String) = when (type.lowercase()) {
        "breakfast" -> "🌅"
        "lunch"     -> "☀️"
        "dinner"    -> "🌙"
        "snack"     -> "🍎"
        else        -> "🍽️"
    }
}
