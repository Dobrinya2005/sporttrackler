package com.fitnesstrainer.app.ui.chat

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.fitnesstrainer.app.R
import com.fitnesstrainer.app.data.model.GroupMessageDto
import com.fitnesstrainer.app.databinding.ItemMessageReceivedBinding
import com.fitnesstrainer.app.databinding.ItemMessageSentBinding
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private const val TYPE_SENT     = 0
private const val TYPE_RECEIVED = 1

class GroupChatAdapter(private val myUserId: Int) :
    ListAdapter<GroupMessageDto, RecyclerView.ViewHolder>(DIFF) {

    override fun getItemViewType(position: Int) =
        if (getItem(position).senderId == myUserId) TYPE_SENT else TYPE_RECEIVED

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        if (viewType == TYPE_SENT)
            SentVH(ItemMessageSentBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        else
            ReceivedVH(ItemMessageReceivedBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val msg  = getItem(position)
        val time = formatTime(msg.sentAt)
        when (holder) {
            is SentVH     -> holder.bind(msg, time)
            is ReceivedVH -> holder.bind(msg, time)
        }
    }

    inner class SentVH(private val b: ItemMessageSentBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(msg: GroupMessageDto, time: String) {
            b.layoutAudio.visibility = View.GONE
            b.layoutImage.visibility = View.GONE
            b.tvMessage.visibility   = View.GONE

            when {
                msg.attachmentType == "image" -> {
                    b.layoutImage.visibility = View.VISIBLE
                    Glide.with(b.root).load(msg.attachmentUrl).into(b.ivAttachment)
                }
                msg.attachmentType == "audio" -> {
                    b.layoutAudio.visibility = View.VISIBLE
                    b.tvAudioDuration.text   = "0:00"
                }
                !msg.messageText.isNullOrBlank() -> {
                    b.tvMessage.visibility = View.VISIBLE
                    b.tvMessage.text       = msg.messageText
                }
            }
            b.tvTime.text = time
        }
    }

    inner class ReceivedVH(private val b: ItemMessageReceivedBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(msg: GroupMessageDto, time: String) {
            b.layoutAudio.visibility = View.GONE
            b.layoutImage.visibility = View.GONE
            b.tvMessage.visibility   = View.GONE

            // Показываем имя и аватар отправителя в группе
            b.tvSenderName.visibility = View.VISIBLE
            b.tvSenderName.text       = msg.senderName
            b.ivAvatar.visibility     = View.VISIBLE
            if (!msg.senderAvatar.isNullOrBlank()) {
                Glide.with(b.root).load(msg.senderAvatar)
                    .circleCrop().placeholder(R.drawable.ic_person).into(b.ivAvatar)
            } else {
                b.ivAvatar.setImageResource(R.drawable.ic_person)
            }

            when {
                msg.attachmentType == "image" -> {
                    b.layoutImage.visibility = View.VISIBLE
                    Glide.with(b.root).load(msg.attachmentUrl).into(b.ivAttachment)
                }
                msg.attachmentType == "audio" -> {
                    b.layoutAudio.visibility = View.VISIBLE
                    b.tvAudioDuration.text   = "0:00"
                }
                !msg.messageText.isNullOrBlank() -> {
                    b.tvMessage.visibility = View.VISIBLE
                    b.tvMessage.text       = msg.messageText
                }
            }
            b.tvTime.text = time
        }
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<GroupMessageDto>() {
            override fun areItemsTheSame(a: GroupMessageDto, b: GroupMessageDto) = a.messageId == b.messageId
            override fun areContentsTheSame(a: GroupMessageDto, b: GroupMessageDto) = a == b
        }

        private val fmt = DateTimeFormatter.ofPattern("HH:mm")
        fun formatTime(raw: String): String = try {
            LocalDateTime.parse(raw.take(19)).format(fmt)
        } catch (_: Exception) { "" }
    }
}
