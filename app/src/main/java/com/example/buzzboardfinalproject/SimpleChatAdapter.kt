package com.example.buzzboardfinalproject

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.database.*

class SimpleChatAdapter(
    private val context: Context,
    private var list: List<SimpleChatRoom>,
    private val onClick: (SimpleChatRoom) -> Unit
) : RecyclerView.Adapter<SimpleChatAdapter.Holder>() {

    private val postsRef: DatabaseReference =
        FirebaseDatabase.getInstance().getReference("Posts")

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val v = LayoutInflater.from(context)
            .inflate(R.layout.item_chat_room, parent, false)
        return Holder(v)
    }

    override fun getItemCount(): Int = list.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val room = list[position]

        holder.title.text = room.title.ifBlank { "Event chat" }

        // 👇 Show "1 participant" vs "3 participants"
        val count = room.participantCount
        holder.participantCount.text =
            if (count == 1) "1 participant" else "$count participants"

        holder.thumb.setImageResource(R.drawable.add_image_icon)

        val postId = room.postId
        holder.itemView.tag = postId // prevent wrong image on recycled rows

        if (postId.isNotBlank()) {
            postsRef.child(postId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        // if view was recycled, ignore result
                        if (holder.itemView.tag != postId) return

                        val post = snapshot.getValue(Post::class.java)
                        val img = post?.postimage.orEmpty()

                        when {
                            img.startsWith("http") -> {
                                Glide.with(context)
                                    .load(img)
                                    .placeholder(R.drawable.add_image_icon)
                                    .into(holder.thumb)
                            }
                            img.isNotEmpty() -> {
                                try {
                                    val bytes = Base64.decode(img, Base64.DEFAULT)
                                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                    holder.thumb.setImageBitmap(bitmap)
                                } catch (e: Exception) {
                                    holder.thumb.setImageResource(R.drawable.add_image_icon)
                                }
                            }
                            else -> holder.thumb.setImageResource(R.drawable.add_image_icon)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        holder.thumb.setImageResource(R.drawable.add_image_icon)
                    }
                })
        }

        holder.itemView.setOnClickListener { onClick(room) }
    }

    class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val thumb: ImageView = itemView.findViewById(R.id.ivRoomThumb)
        val title: TextView = itemView.findViewById(R.id.tvRoomName)
        val participantCount: TextView = itemView.findViewById(R.id.tvParticipantCount) // 👈 NEW

    }

    fun submitList(newList: List<SimpleChatRoom>) {
        list = newList
        notifyDataSetChanged()
    }
}
