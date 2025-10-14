package com.example.buzzboardfinalproject

import android.widget.ImageButton
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

class PostAdapter2(
    private val context: Context,
    private val postList: ArrayList<Post>,
    private val onItemClick: (Post) -> Unit,
    private val onFavoriteClick: (Post) -> Unit
) : RecyclerView.Adapter<PostAdapter2.PostViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.recycler_item, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = postList[position]

        holder.postTitle.text = post.title
        holder.postDescription.text = post.description
        holder.postLocation.text = post.location

        // ✅ Use Glide if image is URL-based, else decode Base64
        if (post.postimage.startsWith("http")) {
            Glide.with(context).load(post.postimage).into(holder.postImage)
        } else {
            try {
                val imageBytes = Base64.decode(post.postimage, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                holder.postImage.setImageBitmap(bitmap)
            } catch (e: Exception) {
                holder.postImage.setImageResource(R.drawable.add_image_icon)
            }
        }
        // Set the heart icon based on Favorite status
        val iconRes = if (post.isFavorite) {
            R.drawable.baseline_favorite_24
        } else {
            R.drawable.baseline_favorite_border_24
        }
        holder.btnFavorite.setImageResource(iconRes)

        // ✅ On click → open PostDetailActivity with post_id
        holder.itemView.setOnClickListener {
            onItemClick(post)
        }

        // Toggle favorite (filled-heart) when heart outline is clicked
        holder.btnFavorite.setOnClickListener {
            post.isFavorite = !post.isFavorite
            notifyItemChanged(position)
            onFavoriteClick(post)
        }
    }

    override fun getItemCount(): Int = postList.size

    class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val postImage: ImageView = itemView.findViewById(R.id.recyclerImage)
        val postTitle: TextView = itemView.findViewById(R.id.recyclerTitle)
        val postDescription: TextView = itemView.findViewById(R.id.recyclerCaption)
        val postLocation: TextView = itemView.findViewById(R.id.recyclerLocation)
        val btnFavorite: ImageButton = itemView.findViewById(R.id.btnFavorite)
    }
}
