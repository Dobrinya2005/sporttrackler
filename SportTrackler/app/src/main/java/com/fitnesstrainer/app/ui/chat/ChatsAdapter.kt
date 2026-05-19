package com.fitnesstrainer.app.ui.chat

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.fitnesstrainer.app.R
import com.fitnesstrainer.app.databinding.ItemChatPreviewBinding
import com.fitnesstrainer.app.BuildConfig

class ChatsAdapter(
    private val onTrainerClick: (Int, String) -> Unit,
    private val onGroupClick: (Int, String) -> Unit
) : ListAdapter<ChatListItem, ChatsAdapter.ViewHolder>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<ChatListItem>() {
            override fun areItemsTheSame(a: ChatListItem, b: ChatListItem) = when {
                a is ChatListItem.TrainerItem && b is ChatListItem.TrainerItem -> a.userId == b.userId
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
            is ChatListItem.TrainerItem -> {
                b.tvName.text = item.name
                b.tvLastMessage.text = "Личный чат"
                b.ivBadge.setImageResource(R.drawable.ic_person)
                val avatarUrl = item.avatar?.let { BuildConfig.BASE_URL.trimEnd('/') + it }
                Glide.with(b.ivAvatar).load(avatarUrl).placeholder(R.drawable.ic_person)
                    .circleCrop().into(b.ivAvatar)
                b.root.setOnClickListener { onTrainerClick(item.userId, item.name) }
            }
            is ChatListItem.GroupItem -> {
                b.tvName.text = item.name
                b.tvLastMessage.text = item.lastMessage ?: "Групповой чат"
                b.ivBadge.setImageResource(R.drawable.ic_group)
                val avatarUrl = item.avatar?.let { BuildConfig.BASE_URL.trimEnd('/') + it }
                Glide.with(b.ivAvatar).load(avatarUrl).placeholder(R.drawable.ic_group)
                    .circleCrop().into(b.ivAvatar)
                b.root.setOnClickListener { onGroupClick(item.groupId, item.name) }
            }
        }
    }
}
