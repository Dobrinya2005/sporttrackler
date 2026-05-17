package com.fitnesstrainer.app.ui.food

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.fitnesstrainer.app.data.model.FoodProduct
import com.fitnesstrainer.app.databinding.ItemFoodProductBinding
import com.fitnesstrainer.app.util.FoodEmojiUtil
import com.fitnesstrainer.app.util.SoundManager

class FoodSearchAdapter(
    private val onClick: (FoodProduct) -> Unit
) : ListAdapter<FoodProduct, FoodSearchAdapter.VH>(DIFF) {

    inner class VH(private val b: ItemFoodProductBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(product: FoodProduct) {
            b.tvEmoji.text    = FoodEmojiUtil.getEmoji(product.name)
            b.tvName.text     = product.name
            b.tvBrand.text    = product.brand ?: ""
            b.tvCalories.text = "%.0f ккал/100г".format(product.caloriesPer100g)
            b.tvMacros.text   = "Б %.1f  Ж %.1f  У %.1f".format(
                product.proteinPer100g, product.fatPer100g, product.carbsPer100g
            )
            b.root.setOnClickListener {
                SoundManager.playClick(it)
                onClick(product)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemFoodProductBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<FoodProduct>() {
            override fun areItemsTheSame(a: FoodProduct, b: FoodProduct) = a.productId == b.productId
            override fun areContentsTheSame(a: FoodProduct, b: FoodProduct) = a == b
        }
    }
}
