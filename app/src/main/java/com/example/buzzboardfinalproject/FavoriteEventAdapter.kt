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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FavoriteEventAdapter(
    private val context: Context,
    private var events: ArrayList<Post>
) : RecyclerView.Adapter<FavoriteEventAdapter.FavViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavViewHolder {
        val view = LayoutInflater.from(context)
            .inflate(R.layout.item_favorite_event, parent, false)
        return FavViewHolder(view)
    }

    override fun onBindViewHolder(holder: FavViewHolder, position: Int) {
        val post = events[position]

        // 📝 Title
        holder.tvTitle.text = post.title

        // 📅 Only show date + time
        val millis = post.eventDateMillis ?: 0L
        if (millis != 0L) {
            val sdf = SimpleDateFormat("EEE, MMM d • h:mm a", Locale.getDefault())
            holder.tvDateTime.text = sdf.format(Date(millis))
        } else {
            holder.tvDateTime.text = ""   // or "Time TBD"
        }

        // 🖼 Thumbnail image
        val img = post.postimage.orEmpty()
        when {
            img.startsWith("http") -> {
                Glide.with(context)
                    .load(img)
                    .placeholder(R.drawable.add_image_icon)
                    .into(holder.ivThumb)
            }
            img.isNotEmpty() -> {
                try {
                    val bytes = Base64.decode(img, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    holder.ivThumb.setImageBitmap(bitmap)
                } catch (e: Exception) {
                    holder.ivThumb.setImageResource(R.drawable.add_image_icon)
                }
            }
            else -> holder.ivThumb.setImageResource(R.drawable.add_image_icon)
        }

        // 👉 Tap row → open PostDetailActivity
        holder.itemView.setOnClickListener {
            val intent = Intent(context, PostDetailActivity::class.java)
            intent.putExtra("post_id", post.postid)
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = events.size

    fun updateList(newList: ArrayList<Post>) {
        events = newList
        notifyDataSetChanged()
    }

    class FavViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivThumb: ImageView = itemView.findViewById(R.id.ivEventThumb)
        val tvTitle: TextView = itemView.findViewById(R.id.tvEventTitle)
        val tvDateTime: TextView = itemView.findViewById(R.id.tvEventDateTime)
    }
}
