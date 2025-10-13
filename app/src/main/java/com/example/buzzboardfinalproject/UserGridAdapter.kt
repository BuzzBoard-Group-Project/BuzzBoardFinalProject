package com.example.buzzboardfinalproject

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView

class UserGridAdapter(private val postList: ArrayList<Post>) :
    RecyclerView.Adapter<UserGridAdapter.GridViewHolder>() {

    inner class GridViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val postImage: ImageView = view.findViewById(R.id.imgGridPost)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GridViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_post_grid, parent, false)
        return GridViewHolder(view)
    }

    override fun onBindViewHolder(holder: GridViewHolder, position: Int) {
        val post = postList[position]
        if (!post.postimage.isNullOrEmpty()) {
            try {
                val imageBytes = Base64.decode(post.postimage, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                holder.postImage.setImageBitmap(bitmap)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun getItemCount(): Int = postList.size
}
