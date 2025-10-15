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

class PostAdapter2(
    private val context: Context,
    private var postList: ArrayList<Post>   // 👈 must be "var", not "val"
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

        // ✅ Load image
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

        // ✅ Go to details
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
    }

    // ✅ Safe update function
    fun updateList(newList: ArrayList<Post>) {
        postList = newList
        notifyDataSetChanged()
    }
}
