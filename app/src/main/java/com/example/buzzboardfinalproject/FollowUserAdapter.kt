package com.example.buzzboardfinalproject

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class FollowUserAdapter(
    private val users: List<SimpleUser>,
    private val onUserClicked: (SimpleUser) -> Unit
) : RecyclerView.Adapter<FollowUserAdapter.UserVH>() {

    inner class UserVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imgProfile: ImageView = itemView.findViewById(R.id.imgProfile)
        private val tvName: TextView = itemView.findViewById(R.id.tvName)
        private val tvSubtitle: TextView = itemView.findViewById(R.id.tvSubtitle)

        fun bind(user: SimpleUser) {
            tvName.text = user.name
            tvSubtitle.text = user.accountType ?: "Student"

            val img = user.profileImage
            if (!img.isNullOrEmpty()) {
                if (img.startsWith("http")) {
                    Glide.with(itemView.context).load(img).into(imgProfile)
                } else {
                    try {
                        val bytes = Base64.decode(img, Base64.DEFAULT)
                        val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        imgProfile.setImageBitmap(bmp)
                    } catch (e: Exception) {
                        imgProfile.setImageResource(R.drawable.add_image_icon)
                    }
                }
            } else {
                imgProfile.setImageResource(R.drawable.add_image_icon)
            }

            itemView.setOnClickListener {
                onUserClicked(user)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserVH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_follow_user, parent, false)
        return UserVH(v)
    }

    override fun getItemCount(): Int = users.size

    override fun onBindViewHolder(holder: UserVH, position: Int) {
        holder.bind(users[position])
    }
}
