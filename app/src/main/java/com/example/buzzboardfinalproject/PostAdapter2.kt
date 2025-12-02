package com.example.buzzboardfinalproject

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class PostAdapter2(
    private val context: Context,
    private var postList: ArrayList<Post>
) : RecyclerView.Adapter<PostAdapter2.PostViewHolder>() {

    // cache usernames so we don't re-fetch
    private val userNameCache = mutableMapOf<String, String>()

    // Firebase
    private val db = FirebaseDatabase.getInstance()
    private val likesRef: DatabaseReference = db.getReference("Likes")
    private val dislikesRef: DatabaseReference = db.getReference("Dislikes")
    private val favoritesRef: DatabaseReference = db.getReference("Favorites")
    private val currentUid get() = FirebaseAuth.getInstance().currentUser?.uid

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.recycler_item, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = postList[position]

        holder.postTitle.text = post.title
        holder.postDescription.text = post.description
        holder.postLocation.text = post.location

        // date/time
        if (post.eventDateMillis > 0L) {
            holder.postDateTime.visibility = View.VISIBLE
            holder.postDateTime.text = formatDate(post.eventDateMillis)
        } else {
            holder.postDateTime.visibility = View.GONE
        }

        // image (url or Base64)
        if (post.postimage.startsWith("http")) {
            Glide.with(context).load(post.postimage).into(holder.postImage)
        } else if (post.postimage.isNotEmpty()) {
            try {
                val bytes = Base64.decode(post.postimage, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                holder.postImage.setImageBitmap(bitmap)
            } catch (e: Exception) {
                holder.postImage.setImageResource(R.drawable.add_image_icon)
            }
        } else {
            holder.postImage.setImageResource(R.drawable.add_image_icon)
        }

        // username
        holder.postUsername.text = "BuzzBoard User"
        val publisherId = post.publisher
        if (publisherId.isNotEmpty()) {
            val cached = userNameCache[publisherId]
            if (cached != null) {
                holder.postUsername.text = cached
            } else {
                db.getReference("Users")
                    .child(publisherId)
                    .child("name")
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            val name = snapshot.getValue(String::class.java) ?: "BuzzBoard User"
                            userNameCache[publisherId] = name
                            if (holder.adapterPosition != RecyclerView.NO_POSITION) {
                                holder.postUsername.text = name
                            }
                        }
                        override fun onCancelled(error: DatabaseError) {}
                    })
            }
        }

        val postId = post.postid
        if (postId.isNullOrEmpty()) {
            // still allow details open even if id is weird
            holder.itemView.setOnClickListener {
                val i = Intent(context, PostDetailActivity::class.java)
                i.putExtra("post_id", post.postid)
                context.startActivity(i)
            }
            return
        }

        // ====== LIVE LIKE COUNT + STATE ======
        likesRef.child(postId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val count = snapshot.childrenCount.toInt()
                holder.tvLikeCount.text = count.toString()

                val uid = currentUid
                val isLiked = uid != null && snapshot.hasChild(uid)
                tintLike(holder, isLiked)
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        // ====== LIVE DISLIKE COUNT + STATE ======
        dislikesRef.child(postId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val count = snapshot.childrenCount.toInt()
                holder.tvDislikeCount.text = count.toString()

                val uid = currentUid
                val isDisliked = uid != null && snapshot.hasChild(uid)
                tintDislike(holder, isDisliked)
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        // ====== LIVE FAVORITE (BEE) STATE ======
        currentUid?.let { uid ->
            favoritesRef.child(uid).child(postId)
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val isFav = snapshot.exists()
                        setBeeIcon(holder, isFav)
                    }
                    override fun onCancelled(error: DatabaseError) {}
                })
        }

        // clicks
        holder.btnLike.setOnClickListener { toggleLike(postId) }
        holder.btnDislike.setOnClickListener { toggleDislike(postId) }
        holder.btnBeeFavorite.setOnClickListener { toggleFavorite(postId) }

        // open detail
        holder.itemView.setOnClickListener {
            val i = Intent(context, PostDetailActivity::class.java)
            i.putExtra("post_id", postId)
            context.startActivity(i)
        }
    }

    override fun getItemCount(): Int = postList.size

    class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val postImage: ImageView = itemView.findViewById(R.id.recyclerImage)
        val postTitle: TextView = itemView.findViewById(R.id.recyclerTitle)
        val postDescription: TextView = itemView.findViewById(R.id.recyclerCaption)
        val postLocation: TextView = itemView.findViewById(R.id.recyclerLocation)
        val postUsername: TextView = itemView.findViewById(R.id.recyclerUsername)
        val postDateTime: TextView = itemView.findViewById(R.id.recyclerDateTime)

        val btnLike: ImageButton = itemView.findViewById(R.id.btnLike)
        val tvLikeCount: TextView = itemView.findViewById(R.id.tvLikeCount)
        val btnDislike: ImageButton = itemView.findViewById(R.id.btnDislike)
        val tvDislikeCount: TextView = itemView.findViewById(R.id.tvDislikeCount)
        val btnBeeFavorite: ImageButton = itemView.findViewById(R.id.btnBeeFavorite)
    }

    fun updateList(newList: ArrayList<Post>) {
        postList = newList
        notifyDataSetChanged()
    }

    private fun formatDate(millis: Long): String {
        val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        sdf.timeZone = TimeZone.getDefault()
        return sdf.format(Date(millis))
    }

    // ====== UI helpers ======

    private fun tintLike(holder: PostViewHolder, liked: Boolean) {
        val color = if (liked) context.getColor(R.color.yellow)
        else context.getColor(android.R.color.black)
        holder.btnLike.setColorFilter(color)
    }

    private fun tintDislike(holder: PostViewHolder, disliked: Boolean) {
        val color = if (disliked) context.getColor(R.color.yellow)
        else context.getColor(android.R.color.black)
        holder.btnDislike.setColorFilter(color)
    }

    private fun setBeeIcon(holder: PostViewHolder, isFav: Boolean) {
        holder.btnBeeFavorite.setImageResource(
            if (isFav) R.drawable.ic_bee_filled
            else R.drawable.ic_bee_outline
        )
        holder.btnBeeFavorite.alpha = if (isFav) 1f else 0.55f
    }

    // ====== Firebase toggles ======

    private fun toggleLike(postId: String) {
        val uid = currentUid ?: return
        likesRef.child(postId).child(uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        likesRef.child(postId).child(uid).removeValue()
                    } else {
                        likesRef.child(postId).child(uid).setValue(true)
                        // remove any dislike
                        dislikesRef.child(postId).child(uid).removeValue()
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun toggleDislike(postId: String) {
        val uid = currentUid ?: return
        dislikesRef.child(postId).child(uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        dislikesRef.child(postId).child(uid).removeValue()
                    } else {
                        dislikesRef.child(postId).child(uid).setValue(true)
                        // remove any like
                        likesRef.child(postId).child(uid).removeValue()
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun toggleFavorite(postId: String) {
        val uid = currentUid ?: return
        favoritesRef.child(uid).child(postId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        favoritesRef.child(uid).child(postId).removeValue()
                    } else {
                        favoritesRef.child(uid).child(postId).setValue(true)
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }
}
