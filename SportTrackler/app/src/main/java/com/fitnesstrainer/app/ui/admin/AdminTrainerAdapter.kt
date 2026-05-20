package com.fitnesstrainer.app.ui.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import android.view.View
import com.bumptech.glide.Glide
import com.fitnesstrainer.app.R
import com.fitnesstrainer.app.data.model.AdminTrainerItem
import com.fitnesstrainer.app.databinding.ItemAdminTrainerBinding

class AdminTrainerAdapter(
    private val onReviews: (AdminTrainerItem) -> Unit,
    private val onBlock:   (AdminTrainerItem) -> Unit,
    private val onDelete:  (AdminTrainerItem) -> Unit
) : ListAdapter<AdminTrainerItem, AdminTrainerAdapter.VH>(DIFF) {

    inner class VH(val b: ItemAdminTrainerBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemAdminTrainerBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(h: VH, pos: Int) {
        val item = getItem(pos)
        val b = h.b

        val initials = listOf(item.firstName, item.lastName)
            .filter { it.isNotEmpty() }.take(2)
            .joinToString("") { it.first().uppercaseChar().toString() }
        b.tvInitials.text = initials
        b.tvName.text = "${item.firstName} ${item.lastName}"
        b.tvEmail.text = item.email
        b.tvClients.text = item.clientCount.toString()
        b.tvRating.text = if (item.avgRating != null) "%.1f".format(item.avgRating) else "—"
        b.tvCode.text = item.trainerCode

        if (!item.avatarUrl.isNullOrBlank()) {
            b.ivAvatar.visibility = View.VISIBLE
            b.tvInitials.visibility = View.GONE
            Glide.with(b.ivAvatar).load(item.avatarUrl).circleCrop().into(b.ivAvatar)
        } else {
            b.ivAvatar.visibility = View.GONE
            b.tvInitials.visibility = View.VISIBLE
        }

        if (item.isActive) {
            b.tvStatus.text = "Активен"
            b.tvStatus.setTextColor(0xFF00E596.toInt())
            b.tvStatus.setBackgroundResource(R.drawable.bg_badge_active)
            b.btnBlock.text = "Блок"
            b.btnBlock.setBackgroundColor(0xFFE59B00.toInt())
        } else {
            b.tvStatus.text = "Заблокирован"
            b.tvStatus.setTextColor(0xFFFF4777.toInt())
            b.tvStatus.setBackgroundResource(R.drawable.bg_badge_blocked)
            b.btnBlock.text = "Разблок"
            b.btnBlock.setBackgroundColor(0xFF00C853.toInt())
        }

        b.btnReviews.text = "Отзывы (${item.reviewCount})"
        b.btnReviews.setOnClickListener { onReviews(item) }
        b.btnBlock.setOnClickListener  { onBlock(item) }
        b.btnDelete.setOnClickListener { onDelete(item) }
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<AdminTrainerItem>() {
            override fun areItemsTheSame(a: AdminTrainerItem, b: AdminTrainerItem) = a.userId == b.userId
            override fun areContentsTheSame(a: AdminTrainerItem, b: AdminTrainerItem) = a == b
        }
    }
}
