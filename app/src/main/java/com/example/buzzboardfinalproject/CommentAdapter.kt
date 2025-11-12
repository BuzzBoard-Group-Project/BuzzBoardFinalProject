package com.example.buzzboardfinalproject

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class CommentAdapter(
    private var comments: ArrayList<Comment>
) : RecyclerView.Adapter<CommentAdapter.CommentViewHolder>() {

    inner class CommentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvInitial: TextView = itemView.findViewById(R.id.tvCommentUser)
        val tvUsername: TextView = itemView.findViewById(R.id.tvCommentUsername)
        val tvText: TextView = itemView.findViewById(R.id.tvCommentText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_comment, parent, false)
        return CommentViewHolder(view)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        val c = comments[position]

        holder.tvUsername.text = c.username ?: "Student"
        holder.tvText.text = c.text ?: ""

        // little initial from username/email
        val initial = (c.username ?: "S").first().uppercaseChar().toString()
        holder.tvInitial.text = initial
    }

    override fun getItemCount(): Int = comments.size

    fun updateList(newList: ArrayList<Comment>) {
        comments = newList
        notifyDataSetChanged()
    }
}
