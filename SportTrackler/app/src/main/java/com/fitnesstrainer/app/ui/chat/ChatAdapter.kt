package com.fitnesstrainer.app.ui.chat

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.fitnesstrainer.app.data.model.MessageDto
import com.fitnesstrainer.app.databinding.ItemMessageReceivedBinding
import com.fitnesstrainer.app.databinding.ItemMessageSentBinding
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private const val TYPE_SENT     = 0
private const val TYPE_RECEIVED = 1

class ChatAdapter(
    private val myUserId: Int
) : ListAdapter<MessageDto, RecyclerView.ViewHolder>(DIFF) {

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

    class SentVH(private val b: ItemMessageSentBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(msg: MessageDto, time: String) {
            b.tvMessage.text = msg.messageText ?: ""
            b.tvTime.text    = time
        }
    }

    class ReceivedVH(private val b: ItemMessageReceivedBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(msg: MessageDto, time: String) {
            b.tvSenderName.text = msg.senderName
            b.tvMessage.text    = msg.messageText ?: ""
            b.tvTime.text       = time
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<MessageDto>() {
            override fun areItemsTheSame(a: MessageDto, b: MessageDto) = a.messageId == b.messageId
            override fun areContentsTheSame(a: MessageDto, b: MessageDto) = a == b
        }

        private fun formatTime(sentAt: String) = try {
            LocalDateTime.parse(sentAt, DateTimeFormatter.ISO_DATE_TIME)
                .format(DateTimeFormatter.ofPattern("HH:mm"))
        } catch (_: Exception) { "" }
    }
}
