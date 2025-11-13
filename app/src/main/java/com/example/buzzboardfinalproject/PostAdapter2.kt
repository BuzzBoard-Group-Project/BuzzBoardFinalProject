package com.example.buzzboardfinalproject

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class PostAdapter2(
    private val context: Context,
    private var postList: ArrayList<Post>
) : RecyclerView.Adapter<PostAdapter2.PostViewHolder>() {

    // Cache to store usernames (so we don’t fetch repeatedly)
    private val userNameCache = mutableMapOf<String, String>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.recycler_item, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = postList[position]

        holder.postTitle.text = post.title
        holder.postDescription.text = post.description
        holder.postLocation.text = post.location

        // NEW: show formatted event date if present
        if (post.eventDateMillis > 0) {
            holder.postDateTime.visibility = View.VISIBLE
            holder.postDateTime.text = formatDate(post.eventDateMillis)
        } else {
            holder.postDateTime.visibility = View.GONE
        }

        // Load image (URL or Base64)
        if (post.postimage.startsWith("http")) {
            Glide.with(context).load(post.postimage).into(holder.postImage)
        } else if (post.postimage.isNotEmpty()) {
            try {
                val imageBytes = Base64.decode(post.postimage, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                holder.postImage.setImageBitmap(bitmap)
            } catch (e: Exception) {
                holder.postImage.setImageResource(R.drawable.add_image_icon)
            }
        } else {
            holder.postImage.setImageResource(R.drawable.add_image_icon)
        }

        // Fetch and display username
        holder.postUsername.text = "BuzzBoard User"
        val publisherId = post.publisher
        if (publisherId.isNotEmpty()) {
            val cached = userNameCache[publisherId]
            if (cached != null) {
                holder.postUsername.text = cached
            } else {
                FirebaseDatabase.getInstance()
                    .getReference("Users")
                    .child(publisherId)
                    .child("name")
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            val name = snapshot.getValue(String::class.java) ?: "BuzzBoard User"
                            userNameCache[publisherId] = name
                            // update the visible holder safely
                            val pos = holder.adapterPosition
                            if (pos != RecyclerView.NO_POSITION) {
                                holder.postUsername.text = name
                            }
                        }
                        override fun onCancelled(error: DatabaseError) {}
                    })
            }
        }

        // Open post detail
        holder.itemView.setOnClickListener {
            val intent = Intent(context, PostDetailActivity::class.java)
            intent.putExtra("post_id", post.postid)
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = postList.size

    class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val postImage: ImageView = itemView.findViewById(R.id.recyclerImage)
        val postTitle: TextView = itemView.findViewById(R.id.recyclerTitle)
        val postDescription: TextView = itemView.findViewById(R.id.recyclerCaption)
        val postLocation: TextView = itemView.findViewById(R.id.recyclerLocation)
        val postUsername: TextView = itemView.findViewById(R.id.recyclerUsername)
        val postDateTime: TextView = itemView.findViewById(R.id.recyclerDateTime) // 👈 NEW
    }

    fun updateList(newList: ArrayList<Post>) {
        postList = newList
        notifyDataSetChanged()
    }

    private fun formatDate(millis: Long): String {
        // e.g., "Nov 12, 2025"
        val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        sdf.timeZone = TimeZone.getDefault()
        return sdf.format(Date(millis))
    }
}
