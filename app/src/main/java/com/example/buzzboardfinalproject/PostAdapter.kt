package com.example.buzzboardfinalproject


import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class PostAdapter  (private val posts: List<Post>) :
        RecyclerView.Adapter<PostAdapter.PostViewHolder>() {
        inner class PostViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val usernameText: TextView = view.findViewById(R.id.usernameText)
            val titleText: TextView = view.findViewById(R.id.titleText)
            val descriptionText: TextView = view.findViewById(R.id.descriptionText)
            val locationText: TextView = view.findViewById(R.id.locationText)
            val postImage: ImageView = view.findViewById(R.id.postImage)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_post, parent, false)
            return PostViewHolder(view)
        }

        override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
            val post = posts[position]
            holder.usernameText.text = post.username
            holder.titleText.text = post.title
            holder.descriptionText.text = post.description
            holder.locationText.text = post.location
            holder.postImage.setImageResource(post.imageResId) // Load the image
        }

        override fun getItemCount(): Int = posts.size
    }
