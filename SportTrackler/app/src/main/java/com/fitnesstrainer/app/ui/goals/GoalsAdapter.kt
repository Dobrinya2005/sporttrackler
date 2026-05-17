package com.fitnesstrainer.app.ui.goals

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.fitnesstrainer.app.R
import com.fitnesstrainer.app.data.local.db.GoalType
import com.fitnesstrainer.app.databinding.ItemGoalBinding

class GoalsAdapter(
    private val onDelete: (GoalType) -> Unit
) : ListAdapter<GoalProgress, GoalsAdapter.VH>(DIFF) {

    inner class VH(private val b: ItemGoalBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(item: GoalProgress) {
            b.tvLabel.text   = item.label
            b.tvTarget.text  = "Цель: ${item.target} ${item.unit}"
            b.tvCurrent.text = if (item.current != null)
                "Сейчас: ${"%.1f".format(item.current)} ${item.unit}"
            else "Нет данных замеров"
            b.progressGoal.progress = item.percent
            b.tvPercent.text = "${item.percent}%"
            b.tvDone.visibility = if (item.isDone) android.view.View.VISIBLE else android.view.View.GONE
            b.btnDelete.setOnClickListener { onDelete(item.type) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(ItemGoalBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<GoalProgress>() {
            override fun areItemsTheSame(a: GoalProgress, b: GoalProgress) = a.type == b.type
            override fun areContentsTheSame(a: GoalProgress, b: GoalProgress) = a == b
        }
    }
}
