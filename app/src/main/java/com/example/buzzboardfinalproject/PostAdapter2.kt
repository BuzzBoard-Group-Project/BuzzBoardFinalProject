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

class PostAdapter2(
    private val context: Context,
    private val postList: ArrayList<Post>,
    private val onItemClick: (Post) -> Unit
) : RecyclerView.Adapter<PostAdapter2.PostViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.recycler_item, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = postList[position]
        holder.itemView.setOnClickListener {
           onItemClick(post)
        }
        // ✅ Decode Base64 image string if stored that way
        if (post.postimage.startsWith("http")) {
            // If you ever switch to Firebase Storage URLs later, you can use Glide here
            com.bumptech.glide.Glide.with(context).load(post.postimage).into(holder.postImage)
        } else {
            try {
                val imageBytes = Base64.decode(post.postimage, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                holder.postImage.setImageBitmap(bitmap)
            } catch (e: Exception) {
                holder.postImage.setImageResource(R.drawable.add_image_icon) // fallback image
            }
        }

        // ✅ Bind text fields
        holder.postTitle.text = post.title
        holder.postDescription.text = post.description
        holder.postLocation.text = post.location

    }

    override fun getItemCount(): Int = postList.size

    class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val postImage: ImageView = itemView.findViewById(R.id.recyclerImage)
        val postTitle: TextView = itemView.findViewById(R.id.recyclerTitle)
        val postDescription: TextView = itemView.findViewById(R.id.recyclerCaption)
        val postLocation: TextView = itemView.findViewById(R.id.recyclerLocation)
        val postTime: TextView = itemView.findViewById(R.id.recyclerTime)
    }
}
