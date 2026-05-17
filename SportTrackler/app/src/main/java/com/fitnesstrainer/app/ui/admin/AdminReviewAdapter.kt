package com.fitnesstrainer.app.ui.admin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.fitnesstrainer.app.data.model.AdminReviewItem
import com.fitnesstrainer.app.databinding.ItemAdminReviewBinding

class AdminReviewAdapter(
    private val onReply:  (AdminReviewItem) -> Unit,
    private val onDelete: (AdminReviewItem) -> Unit
) : ListAdapter<AdminReviewItem, AdminReviewAdapter.VH>(DIFF) {

    inner class VH(val b: ItemAdminReviewBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemAdminReviewBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(h: VH, pos: Int) {
        val item = getItem(pos)
        val b = h.b

        val initials = item.clientName.split(" ")
            .filter { it.isNotEmpty() }.take(2)
            .joinToString("") { it.first().uppercaseChar().toString() }
        b.tvInitials.text = initials
        b.tvClientName.text = item.clientName
        b.tvDate.text = item.createdAt.take(10)
        b.tvRating.text = "★".repeat(item.rating) + "☆".repeat(5 - item.rating)

        if (!item.comment.isNullOrEmpty()) {
            b.tvComment.visibility = View.VISIBLE
            b.tvComment.text = item.comment
        } else {
            b.tvComment.visibility = View.GONE
        }

        if (!item.adminReply.isNullOrEmpty()) {
            b.layoutReply.visibility = View.VISIBLE
            b.tvReply.text = item.adminReply
        } else {
            b.layoutReply.visibility = View.GONE
        }

        b.btnReply.setOnClickListener  { onReply(item) }
        b.btnDelete.setOnClickListener { onDelete(item) }
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<AdminReviewItem>() {
            override fun areItemsTheSame(a: AdminReviewItem, b: AdminReviewItem) = a.reviewId == b.reviewId
            override fun areContentsTheSame(a: AdminReviewItem, b: AdminReviewItem) = a == b
        }
    }
}
