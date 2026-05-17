package com.fitnesstrainer.app.ui.trainer

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.fitnesstrainer.app.R
import com.fitnesstrainer.app.data.model.TrainerListItem
import com.fitnesstrainer.app.databinding.ItemTrainerCardBinding
import kotlin.math.roundToInt

class TrainerSelectionAdapter(
    private val onSelect: (TrainerListItem) -> Unit
) : ListAdapter<TrainerListItem, TrainerSelectionAdapter.VH>(Diff()) {

    inner class VH(val b: ItemTrainerCardBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemTrainerCardBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)
        with(holder.b) {
            tvName.text = "${item.firstName} ${item.lastName}"
            tvSpecialization.text = item.specialization ?: ""

            val rating = item.rating ?: 0.0
            val stars = "★".repeat(rating.roundToInt().coerceIn(0, 5)) +
                        "☆".repeat((5 - rating.roundToInt()).coerceIn(0, 5))
            tvStars.text = stars
            tvRating.text = if (item.reviewCount > 0)
                "%.1f (%d)".format(rating, item.reviewCount)
            else "Нет отзывов"

            if (item.experience != null) {
                tvExperience.text = "${item.experience} лет\nопыта"
            }

            if (!item.description.isNullOrBlank()) {
                tvDescription.text = item.description
                tvDescription.visibility = android.view.View.VISIBLE
            }

            if (!item.avatarUrl.isNullOrBlank()) {
                Glide.with(ivAvatar)
                    .load(item.avatarUrl)
                    .placeholder(R.drawable.ic_person)
                    .circleCrop()
                    .into(ivAvatar)
            }

            btnSelect.setOnClickListener { onSelect(item) }
        }
    }

    class Diff : DiffUtil.ItemCallback<TrainerListItem>() {
        override fun areItemsTheSame(a: TrainerListItem, b: TrainerListItem) = a.userId == b.userId
        override fun areContentsTheSame(a: TrainerListItem, b: TrainerListItem) = a == b
    }
}
