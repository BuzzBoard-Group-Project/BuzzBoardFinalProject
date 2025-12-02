package com.example.buzzboardfinalproject

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.buzzboardfinalproject.databinding.ItemProfileEventRowBinding
import java.text.SimpleDateFormat
import java.util.*

class ProfileEventRowAdapter(
    private val onClick: (Post) -> Unit
) : RecyclerView.Adapter<ProfileEventRowAdapter.EventVH>() {

    private val items = ArrayList<Post>()

    fun submitList(newList: List<Post>) {
        items.clear()
        items.addAll(newList)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventVH {
        val binding = ItemProfileEventRowBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return EventVH(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: EventVH, position: Int) {
        holder.bind(items[position])
    }

    inner class EventVH(
        private val binding: ItemProfileEventRowBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(post: Post) {
            val ctx = itemView.context

            // Title
            binding.tvTitle.text = post.title ?: "(No title)"

            // Date/time
            val millis = post.eventDateMillis ?: 0L
            binding.tvDate.text = if (millis > 0L) {
                val sdf = SimpleDateFormat("EEE, MMM d • h:mm a", Locale.getDefault())
                sdf.timeZone = TimeZone.getDefault()
                sdf.format(Date(millis))
            } else {
                post.time ?: ""
            }

            // Image (http or Base64)
            val imgStr = post.postimage ?: ""
            if (imgStr.startsWith("http")) {
                Glide.with(ctx).load(imgStr).into(binding.imgThumb)
            } else if (imgStr.isNotEmpty()) {
                try {
                    val bytes = Base64.decode(imgStr, Base64.DEFAULT)
                    val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    binding.imgThumb.setImageBitmap(bmp)
                } catch (e: Exception) {
                    binding.imgThumb.setImageResource(R.drawable.add_image_icon)
                }
            } else {
                binding.imgThumb.setImageResource(R.drawable.add_image_icon)
            }

            itemView.setOnClickListener {
                onClick(post)
            }
        }
    }
}
