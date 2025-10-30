package com.example.buzzboardfinalproject

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class SimpleChatAdapter(
    private val list: List<SimpleChatRoom>,
    private val onClick: (SimpleChatRoom) -> Unit
) : RecyclerView.Adapter<SimpleChatAdapter.Holder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat_room, parent, false)
        return Holder(v)
    }

    override fun getItemCount(): Int = list.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val room = list[position]
        holder.title.text = room.title
        holder.itemView.setOnClickListener { onClick(room) }
    }

    class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.tvRoomName)
    }
}
