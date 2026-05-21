package com.fitnesstrainer.app.ui.chat

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.fitnesstrainer.app.R
import com.fitnesstrainer.app.databinding.ItemChatPreviewBinding
import com.fitnesstrainer.app.BuildConfig
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class ChatsAdapter(
    private val onPersonClick: (Int, String) -> Unit,
    private val onGroupClick: (Int, String) -> Unit
) : ListAdapter<ChatListItem, ChatsAdapter.ViewHolder>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<ChatListItem>() {
            override fun areItemsTheSame(a: ChatListItem, b: ChatListItem) = when {
                a is ChatListItem.PersonItem && b is ChatListItem.PersonItem -> a.userId == b.userId
                a is ChatListItem.GroupItem && b is ChatListItem.GroupItem -> a.groupId == b.groupId
                else -> false
            }
            override fun areContentsTheSame(a: ChatListItem, b: ChatListItem) = a == b
        }
    }

    inner class ViewHolder(val binding: ItemChatPreviewBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemChatPreviewBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val b = holder.binding
        when (val item = getItem(position)) {
            is ChatListItem.PersonItem -> {
                b.tvName.text        = item.name
                b.tvLastMessage.text = item.lastMessage ?: "Личный чат"
                b.tvTime.text        = formatTime(item.lastMessageAt)
                b.tvTime.visibility  = if (item.lastMessageAt != null) View.VISIBLE else View.GONE
                bindUnread(b, item.unreadCount)
                b.ivBadge.setImageResource(R.drawable.ic_person)
                val placeholder = AvatarHelper.forName(item.name)
                val avatarUrl = item.avatar?.let { BuildConfig.BASE_URL.trimEnd('/') + it }
                Glide.with(b.ivAvatar).load(avatarUrl)
                    .placeholder(placeholder).error(placeholder)
                    .circleCrop().into(b.ivAvatar)
                b.card.setOnClickListener { onPersonClick(item.userId, item.name) }
            }
            is ChatListItem.GroupItem -> {
                b.tvName.text        = item.name
                b.tvLastMessage.text = item.lastMessage ?: "Групповой чат"
                b.tvTime.text        = formatTime(item.lastMessageAt)
                b.tvTime.visibility  = if (item.lastMessageAt != null) View.VISIBLE else View.GONE
                bindUnread(b, item.unreadCount)
                b.ivBadge.setImageResource(R.drawable.ic_group)
                val placeholder = AvatarHelper.forName(item.name)
                val avatarUrl = item.avatar?.let { BuildConfig.BASE_URL.trimEnd('/') + it }
                Glide.with(b.ivAvatar).load(avatarUrl)
                    .placeholder(placeholder).error(placeholder)
                    .circleCrop().into(b.ivAvatar)
                b.card.setOnClickListener { onGroupClick(item.groupId, item.name) }
            }
        }
    }

    private fun bindUnread(b: ItemChatPreviewBinding, count: Int) {
        if (count > 0) {
            b.tvUnread.visibility = View.VISIBLE
            b.tvUnread.text = if (count > 99) "99+" else count.toString()
        } else {
            b.tvUnread.visibility = View.GONE
        }
    }

    private fun formatTime(iso: String?): String {
        iso ?: return ""
        return try {
            val dt = LocalDateTime.parse(iso.take(19))
            if (dt.toLocalDate() == LocalDate.now())
                dt.format(DateTimeFormatter.ofPattern("HH:mm"))
            else
                dt.format(DateTimeFormatter.ofPattern("dd.MM"))
        } catch (_: Exception) { "" }
    }
}
