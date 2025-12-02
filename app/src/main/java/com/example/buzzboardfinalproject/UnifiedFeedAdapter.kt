package com.example.buzzboardfinalproject

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlin.math.roundToInt

class UnifiedFeedAdapter(
    private val onVote: (poll: Poll, selectedIndex: Int) -> Unit,
    private val onPostClicked: (post: Post) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val items = ArrayList<UnifiedFeedItem>()

    companion object {
        private const val TYPE_POST = 1
        private const val TYPE_POLL = 2
    }

    // ===== Firebase for reactions =====
    private val db = FirebaseDatabase.getInstance()
    private val likesRef: DatabaseReference = db.getReference("Likes")
    private val dislikesRef: DatabaseReference = db.getReference("Dislikes")
    private val favoritesRef: DatabaseReference = db.getReference("Favorites")
    private val usersRef: DatabaseReference = db.getReference("Users")   // 👈 NEW
    private val currentUid get() = FirebaseAuth.getInstance().currentUser?.uid

    // Stores user poll votes so we can disable UI after voting
    var userVotes: Map<String, Int> = emptyMap()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    fun submitList(newList: List<UnifiedFeedItem>) {
        items.clear()
        items.addAll(newList)
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is UnifiedFeedItem.PostItem -> TYPE_POST
            is UnifiedFeedItem.PollItem -> TYPE_POLL
        }
    }

    override fun getItemCount(): Int = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_POST -> {
                val v = LayoutInflater.from(parent.context)
                    .inflate(R.layout.recycler_item, parent, false)
                PostVH(v)
            }
            else -> {
                val v = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_poll, parent, false)
                PollVH(v)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is UnifiedFeedItem.PostItem -> (holder as PostVH).bind(item.post)
            is UnifiedFeedItem.PollItem -> (holder as PollVH).bind(item.poll)
        }
    }

    // ======================================================
    // POST HOLDER  (events with likes / favorites)
    // ======================================================
    inner class PostVH(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val img: ImageView = itemView.findViewById(R.id.recyclerImage)
        private val title: TextView = itemView.findViewById(R.id.recyclerTitle)
        private val desc: TextView = itemView.findViewById(R.id.recyclerCaption)
        private val location: TextView = itemView.findViewById(R.id.recyclerLocation)
        private val username: TextView = itemView.findViewById(R.id.recyclerUsername)
        private val dateTime: TextView = itemView.findViewById(R.id.recyclerDateTime)

        private val btnLike: ImageButton = itemView.findViewById(R.id.btnLike)
        private val tvLikeCount: TextView = itemView.findViewById(R.id.tvLikeCount)
        private val btnDislike: ImageButton = itemView.findViewById(R.id.btnDislike)
        private val tvDislikeCount: TextView = itemView.findViewById(R.id.tvDislikeCount)
        private val btnBeeFavorite: ImageButton = itemView.findViewById(R.id.btnBeeFavorite)

        fun bind(post: Post) {
            val context = itemView.context

            // Title
            title.text = post.title

            // Hide caption on feed
            desc.visibility = View.GONE

            // Location
            location.text = post.location

            // 🔹 Load username from Users node using publisher uid
            val publisherId = post.publisher   // make sure Post has "publisher: String?"
            if (!publisherId.isNullOrEmpty()) {

                // Temporary placeholder while it loads
                username.text = post.username.takeIf { !it.isNullOrEmpty() } ?: "BuzzBoard User"

                // Load real name once
                usersRef.child(publisherId)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            val name = snapshot.child("name")
                                .getValue(String::class.java)
                            if (!name.isNullOrEmpty()) {
                                username.text = name
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {}
                    })

                // Tap username → open that user's profile
                username.setOnClickListener {
                    val intent = Intent(context, UserProfileActivity::class.java)
                    intent.putExtra("user_id", publisherId)
                    context.startActivity(intent)
                }
            } else {
                username.text = "BuzzBoard User"
                username.setOnClickListener(null)
            }

            // Date/time
            if (post.eventDateMillis > 0L) {
                dateTime.visibility = View.VISIBLE
                dateTime.text = formatDate(post.eventDateMillis)
            } else {
                dateTime.visibility = View.GONE
            }



    // Image
            val imgStr = post.postimage
            if (imgStr.startsWith("http")) {
                Glide.with(context).load(imgStr).into(img)
            } else if (imgStr.isNotEmpty()) {
                try {
                    val bytes =
                        android.util.Base64.decode(imgStr, android.util.Base64.DEFAULT)
                    val bmp =
                        android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    img.setImageBitmap(bmp)
                } catch (e: Exception) {
                    img.setImageResource(R.drawable.add_image_icon)
                }
            } else {
                img.setImageResource(R.drawable.add_image_icon)
            }

            val postId = post.postid ?: ""

            if (postId.isNotEmpty()) {
                // ---- Likes ----
                likesRef.child(postId).addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        tvLikeCount.text = snapshot.childrenCount.toString()
                        val uid = currentUid
                        val isLiked = uid != null && snapshot.hasChild(uid)
                        tintLike(isLiked)
                    }
                    override fun onCancelled(error: DatabaseError) {}
                })

                // ---- Dislikes ----
                dislikesRef.child(postId).addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        tvDislikeCount.text = snapshot.childrenCount.toString()
                        val uid = currentUid
                        val isDisliked = uid != null && snapshot.hasChild(uid)
                        tintDislike(isDisliked)
                    }
                    override fun onCancelled(error: DatabaseError) {}
                })

                // ---- Favorites (bee) ----
                val uidFav = currentUid
                if (uidFav == null) {
                    setBeeIcon(false)
                } else {
                    favoritesRef.child(uidFav).child(postId)
                        .addValueEventListener(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                setBeeIcon(snapshot.exists())
                            }
                            override fun onCancelled(error: DatabaseError) {}
                        })
                }

                // Click listeners
                btnLike.setOnClickListener { toggleLike(postId) }
                btnDislike.setOnClickListener { toggleDislike(postId) }
                btnBeeFavorite.setOnClickListener { toggleFavorite(postId) }
            }

            // Open detail screen
            itemView.setOnClickListener { onPostClicked(post) }
        }

        private fun tintLike(liked: Boolean) {
            val color = if (liked)
                itemView.context.getColor(R.color.yellow)
            else
                itemView.context.getColor(android.R.color.black)
            btnLike.setColorFilter(color)
        }

        private fun tintDislike(disliked: Boolean) {
            val color = if (disliked)
                itemView.context.getColor(R.color.yellow)
            else
                itemView.context.getColor(android.R.color.black)
            btnDislike.setColorFilter(color)
        }

        private fun setBeeIcon(isFav: Boolean) {
            btnBeeFavorite.setImageResource(
                if (isFav) R.drawable.ic_bee_filled else R.drawable.ic_bee_outline
            )
            btnBeeFavorite.alpha = if (isFav) 1f else 0.55f
        }
    }

    // ======================================================
    // POLL HOLDER
    // ======================================================
    inner class PollVH(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val question: TextView = itemView.findViewById(R.id.questionText)
        private val statusText: TextView = itemView.findViewById(R.id.statusText)
        private val optionsContainer: RadioGroup = itemView.findViewById(R.id.optionsContainer)

        fun bind(poll: Poll) {

            question.text = poll.question

            val totals = poll.totals
            val totalVotes = totals.sum()
            val denom = if (totalVotes == 0) 1 else totalVotes
            val percents = totals.map { ((it * 100f) / denom).roundToInt() }

            val alreadyVotedIndex = userVotes[poll.id]
            val votingEnabled = (alreadyVotedIndex == null)

            // Reset
            optionsContainer.setOnCheckedChangeListener(null)
            optionsContainer.removeAllViews()

            // Build buttons
            poll.options.forEachIndexed { idx, option ->
                val rb = RadioButton(itemView.context).apply {
                    id = View.generateViewId()
                    text = "$option  ${percents[idx]}%"
                    isEnabled = votingEnabled
                }
                optionsContainer.addView(rb)
            }

            statusText.text = ""

            // User already voted
            if (alreadyVotedIndex != null &&
                alreadyVotedIndex in 0 until optionsContainer.childCount
            ) {
                val rb = optionsContainer.getChildAt(alreadyVotedIndex) as RadioButton
                rb.isChecked = true

                for (i in 0 until optionsContainer.childCount) {
                    val view = optionsContainer.getChildAt(i)
                    view.alpha = if (i == alreadyVotedIndex) 1f else 0.4f
                    view.isEnabled = false
                }
            }

            // Voting listener
            optionsContainer.setOnCheckedChangeListener { group, checkedId ->
                if (!votingEnabled) return@setOnCheckedChangeListener

                val idx = group.indexOfChild(group.findViewById(checkedId))
                if (idx != -1) onVote(poll, idx)
            }
        }
    }

    // ===== Firebase toggle helpers =====

    private fun toggleLike(postId: String) {
        val uid = currentUid ?: return

        likesRef.child(postId).child(uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        likesRef.child(postId).child(uid).removeValue()
                    } else {
                        likesRef.child(postId).child(uid).setValue(true)
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

    private fun formatDate(millis: Long): String {
        val sdf = java.text.SimpleDateFormat(
            "MMM d, yyyy • h:mm a",
            java.util.Locale.getDefault()
        )
        sdf.timeZone = java.util.TimeZone.getDefault()
        return sdf.format(java.util.Date(millis))
    }
}
