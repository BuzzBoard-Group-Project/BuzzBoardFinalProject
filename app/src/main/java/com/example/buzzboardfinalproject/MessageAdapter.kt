package com.example.buzzboardfinalproject

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class MessageAdapter(
    private val list: List<ChatMessage>,
    private val currentUid: String?
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val TYPE_ME = 1
    private val TYPE_OTHER = 2

    override fun getItemViewType(position: Int): Int {
        val msg = list[position]
        return if (msg.senderId == currentUid) TYPE_ME else TYPE_OTHER
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_ME) {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_chat_me, parent, false)
            MeHolder(v)
        } else {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_chat_other, parent, false)
            OtherHolder(v)
        }
    }

    override fun getItemCount(): Int = list.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val msg = list[position]
        if (holder is MeHolder) {
            holder.txt.text = msg.text
        } else if (holder is OtherHolder) {
            holder.txt.text = msg.text
        }
    }

    class MeHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txt: TextView = itemView.findViewById(R.id.tvMyMessage)
    }

    class OtherHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txt: TextView = itemView.findViewById(R.id.tvOtherMessage)
    }
}
