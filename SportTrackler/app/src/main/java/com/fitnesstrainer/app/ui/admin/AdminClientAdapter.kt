package com.fitnesstrainer.app.ui.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.fitnesstrainer.app.data.model.AdminClientItem
import com.fitnesstrainer.app.databinding.ItemAdminClientBinding

class AdminClientAdapter(
    private val onBlock:  (AdminClientItem) -> Unit,
    private val onDelete: (AdminClientItem) -> Unit
) : ListAdapter<AdminClientItem, AdminClientAdapter.VH>(DIFF) {

    inner class VH(val b: ItemAdminClientBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemAdminClientBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(h: VH, pos: Int) {
        val item = getItem(pos)
        val b = h.b

        val initials = listOf(item.firstName, item.lastName)
            .filter { it.isNotEmpty() }.take(2)
            .joinToString("") { it.first().uppercaseChar().toString() }
        b.tvInitials.text = initials
        b.tvName.text = "${item.firstName} ${item.lastName}"
        b.tvEmail.text = item.email
        b.tvTrainer.text = if (item.trainerName != null) "Тренер: ${item.trainerName}" else "Без тренера"

        if (item.isActive) {
            b.tvStatus.text = "Активен"
            b.tvStatus.setTextColor(0xFF00E596.toInt())
            b.tvStatus.setBackgroundResource(com.fitnesstrainer.app.R.drawable.bg_badge_active)
            b.btnBlock.text = "Блок"
        } else {
            b.tvStatus.text = "Заблокирован"
            b.tvStatus.setTextColor(0xFFFF4777.toInt())
            b.tvStatus.setBackgroundResource(com.fitnesstrainer.app.R.drawable.bg_badge_blocked)
            b.btnBlock.text = "Разблок"
        }

        b.btnBlock.setOnClickListener  { onBlock(item) }
        b.btnDelete.setOnClickListener { onDelete(item) }
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<AdminClientItem>() {
            override fun areItemsTheSame(a: AdminClientItem, b: AdminClientItem) = a.userId == b.userId
            override fun areContentsTheSame(a: AdminClientItem, b: AdminClientItem) = a == b
        }
    }
}
