package com.example.buzzboardfinalproject

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.buzzboardfinalproject.databinding.ItemProfilePostGridBinding

class ProfilePostGridAdapter(
    private val onClick: (Post) -> Unit
) : RecyclerView.Adapter<ProfilePostGridAdapter.PostGridVH>() {

    private val items = ArrayList<Post>()

    fun submitList(newList: List<Post>) {
        items.clear()
        items.addAll(newList)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostGridVH {
        val binding = ItemProfilePostGridBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PostGridVH(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: PostGridVH, position: Int) {
        holder.bind(items[position])
    }

    inner class PostGridVH(
        private val binding: ItemProfilePostGridBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private val img: ImageView = binding.imgPostThumb

        fun bind(post: Post) {
            val context = itemView.context
            val imgStr = post.postimage ?: ""

            if (imgStr.startsWith("http")) {
                Glide.with(context).load(imgStr).into(img)
            } else if (imgStr.isNotEmpty()) {
                try {
                    val bytes = Base64.decode(imgStr, Base64.DEFAULT)
                    val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    img.setImageBitmap(bmp)
                } catch (e: Exception) {
                    img.setImageResource(R.drawable.add_image_icon)
                }
            } else {
                img.setImageResource(R.drawable.add_image_icon)
            }

            itemView.setOnClickListener { onClick(post) }
        }
    }
}
