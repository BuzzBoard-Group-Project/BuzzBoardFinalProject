package com.example.buzzboardfinalproject

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class MessageAdapter(
    private val messages: List<ChatMessage>,
    private val currentUid: String?
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val TYPE_SENT = 1
    private val TYPE_RECEIVED = 2

    override fun getItemViewType(position: Int): Int {
        val msg = messages[position]
        return if (msg.senderId == currentUid) TYPE_SENT else TYPE_RECEIVED
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_SENT) {
            val view = inflater.inflate(R.layout.item_chat_me, parent, false)
            SentHolder(view)
        } else {
            val view = inflater.inflate(R.layout.item_chat_other, parent, false)
            ReceivedHolder(view)
        }
    }

    override fun getItemCount(): Int = messages.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val msg = messages[position]
        val timeText = formatTime(msg.timestamp)

        when (holder) {
            is SentHolder -> {
                holder.message.text = msg.text
                holder.time.text = timeText

                // For your own messages, show "You"
                holder.name.text = "You"
            }
            is ReceivedHolder -> {
                holder.message.text = msg.text
                holder.time.text = timeText

                // For others, use senderName or fallback
                val label = msg.senderName.ifBlank { "Student" }
                holder.name.text = label
            }
        }
    }

    class SentHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val message: TextView = itemView.findViewById(R.id.tvMessageText)
        val time: TextView = itemView.findViewById(R.id.tvMessageTime)
        val name: TextView = itemView.findViewById(R.id.tvSenderName)
    }

    class ReceivedHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val message: TextView = itemView.findViewById(R.id.tvMessageText)
        val time: TextView = itemView.findViewById(R.id.tvMessageTime)
        val name: TextView = itemView.findViewById(R.id.tvSenderName)
    }

    private fun formatTime(timestamp: Long): String {
        if (timestamp <= 0L) return ""
        val now = System.currentTimeMillis()
        val oneDayMillis = 24 * 60 * 60 * 1000L
        val sameDay = (now / oneDayMillis) == (timestamp / oneDayMillis)

        return if (sameDay) {
            SimpleDateFormat("h:mm a", Locale.getDefault())
                .format(Date(timestamp))
        } else {
            SimpleDateFormat("MMM d", Locale.getDefault())
                .format(Date(timestamp))
        }
    }
}
