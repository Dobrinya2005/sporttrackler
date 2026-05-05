package com.fitnesstrainer.app.ui.trainer

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.fitnesstrainer.app.R
import com.fitnesstrainer.app.data.model.ClientProfile
import com.fitnesstrainer.app.databinding.ItemClientBinding

class ClientsAdapter(
    private val onClick: (ClientProfile) -> Unit,
    private val onChatClick: (ClientProfile) -> Unit
) : ListAdapter<ClientProfile, ClientsAdapter.VH>(DIFF) {

    inner class VH(private val b: ItemClientBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(client: ClientProfile) {
            b.tvName.text  = "${client.firstName} ${client.lastName}"
            b.tvEmail.text = client.email

            if (!client.avatarUrl.isNullOrBlank()) {
                Glide.with(b.root).load(client.avatarUrl).circleCrop()
                    .placeholder(R.drawable.ic_person).into(b.ivAvatar)
            } else {
                b.ivAvatar.setImageResource(R.drawable.ic_person)
            }

            b.root.setOnClickListener { onClick(client) }
            b.btnChat.setOnClickListener { onChatClick(client) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemClientBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<ClientProfile>() {
            override fun areItemsTheSame(a: ClientProfile, b: ClientProfile) = a.userId == b.userId
            override fun areContentsTheSame(a: ClientProfile, b: ClientProfile) = a == b
        }
    }
}
