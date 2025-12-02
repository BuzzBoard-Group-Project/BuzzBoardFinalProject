package com.example.buzzboardfinalproject

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.text.TextUtils
import android.util.Base64
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.buzzboardfinalproject.databinding.ActivityPostDetailBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class PostDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPostDetailBinding
    private var isFavorited = false
    private var isRegistered = false
    private lateinit var postId: String

    // Firebase
    private val db: FirebaseDatabase by lazy { FirebaseDatabase.getInstance() }
    private lateinit var favoritesRef: DatabaseReference
    private lateinit var registrationsRef: DatabaseReference
    private lateinit var postRef: DatabaseReference

    // likes / dislikes
    private lateinit var likesRef: DatabaseReference
    private lateinit var dislikesRef: DatabaseReference

    // comments
    private lateinit var commentsRef: DatabaseReference
    private var commentList = ArrayList<Comment>()
    private lateinit var commentAdapter: CommentAdapter

    private val currentUserId: String?
        get() = FirebaseAuth.getInstance().currentUser?.uid

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPostDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }

        postId = intent.getStringExtra("post_id") ?: return

        favoritesRef = db.getReference("Favorites")
        registrationsRef = db.getReference("EventRegistrations").child(postId)
        postRef = db.getReference("Posts").child(postId)
        commentsRef = db.getReference("Comments").child(postId)

        likesRef = db.getReference("Likes")
        dislikesRef = db.getReference("Dislikes")

        // comments RV
        commentAdapter = CommentAdapter(commentList)
        binding.rvComments.layoutManager = LinearLayoutManager(this)
        binding.rvComments.adapter = commentAdapter

        loadPostDetails()
        checkFavoriteStatus()
        checkIfUserRegistered()
        observeLikesAndDislikes()
        observeFavorite()
        loadComments()

        // Register / unregister ONLY
        binding.btnRegisterEvent.setOnClickListener { toggleRegistration() }

        // Favorites
        binding.btnFavorite.setOnClickListener { toggleFavorite() }

        // Likes / dislikes
        binding.btnLikeDetail.setOnClickListener { toggleLike(postId) }
        binding.btnDislikeDetail.setOnClickListener { toggleDislike(postId) }

        // Comments
        binding.btnSendComment.setOnClickListener { sendComment() }

        // Optional: tap image to open event chat
        binding.imgPostDetail.setOnClickListener {
            openEventChat()
        }
    }

    // ---------- Load post ----------

    private fun loadPostDetails() {
        postRef.get().addOnSuccessListener { snapshot ->
            val post = snapshot.getValue(Post::class.java) ?: return@addOnSuccessListener

            binding.tvPostTitle.text = post.title
            binding.tvPostDetailDescription.text = post.description
            binding.tvLocation.text = post.location

            if (post.eventDateMillis > 0L) {
                binding.tvPostDetailDate.text = formatDateTime(post.eventDateMillis)
            } else {
                binding.tvPostDetailDate.text = post.time
            }

            if (!post.postimage.isNullOrEmpty()) {
                try {
                    val imageBytes = Base64.decode(post.postimage, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                    binding.imgPostDetail.setImageBitmap(bitmap)
                } catch (e: Exception) {
                    binding.imgPostDetail.setImageResource(R.drawable.add_image_icon)
                }
            } else {
                binding.imgPostDetail.setImageResource(R.drawable.add_image_icon)
            }
        }
    }

    // ---------- Likes / Dislikes ----------

    private fun observeLikesAndDislikes() {
        val uid = currentUserId

        likesRef.child(postId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val count = snapshot.childrenCount.toInt()
                binding.tvLikeCountDetail.text = count.toString()

                val isLiked = uid != null && snapshot.hasChild(uid)
                tintLike(isLiked)
            }

            override fun onCancelled(error: DatabaseError) {}
        })

        dislikesRef.child(postId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val count = snapshot.childrenCount.toInt()
                binding.tvDislikeCountDetail.text = count.toString()

                val isDisliked = uid != null && snapshot.hasChild(uid)
                tintDislike(isDisliked)
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun tintLike(liked: Boolean) {
        val color = if (liked) getColor(R.color.yellow) else getColor(android.R.color.black)
        binding.btnLikeDetail.setColorFilter(color)
    }

    private fun tintDislike(disliked: Boolean) {
        val color = if (disliked) getColor(R.color.yellow) else getColor(android.R.color.black)
        binding.btnDislikeDetail.setColorFilter(color)
    }

    private fun toggleLike(postId: String) {
        val uid = currentUserId ?: return

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
        val uid = currentUserId ?: return

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

    // ---------- Favorite (bee) ----------

    private fun observeFavorite() {
        val uid = currentUserId ?: return
        favoritesRef.child(uid).child(postId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    isFavorited = snapshot.exists()
                    updateFavoriteIcon()
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun toggleFavorite() {
        val uid = currentUserId ?: return

        favoritesRef.child(uid).child(postId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        favoritesRef.child(uid).child(postId).removeValue()
                        isFavorited = false
                        Toast.makeText(
                            this@PostDetailActivity,
                            "Removed from favorites",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        favoritesRef.child(uid).child(postId).setValue(true)
                        isFavorited = true
                        Toast.makeText(
                            this@PostDetailActivity,
                            "Added to favorites",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    updateFavoriteIcon()
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun updateFavoriteIcon() {
        if (isFavorited) {
            binding.btnFavorite.setImageResource(R.drawable.ic_bee_filled)
        } else {
            binding.btnFavorite.setImageResource(R.drawable.ic_bee_outline)
        }
    }

    // ---------- Register / Unregister + wire up EventChats ----------

    private fun checkIfUserRegistered() {
        val uid = currentUserId ?: return
        registrationsRef.child(uid).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                isRegistered = snapshot.exists()
                updateRegisterButton()
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun toggleRegistration() {
        val uid = currentUserId ?: return
        val rootRef = db.reference
        val prefs = getSharedPreferences("chat_prefs", Context.MODE_PRIVATE)

        registrationsRef.child(uid).get().addOnSuccessListener { snap ->
            if (snap.exists()) {
                // 🔻 UNREGISTER + remove from EventChats participants
                val updates = hashMapOf<String, Any?>(
                    "/EventRegistrations/$postId/$uid" to null,
                    "/EventChats/$postId/participants/$uid" to null
                )

                rootRef.updateChildren(updates).addOnCompleteListener {
                    isRegistered = false
                    // mark as left so Messages hides it
                    prefs.edit().putBoolean("left_$postId", true).apply()
                    updateRegisterButton()
                    Toast.makeText(this, "Registration canceled.", Toast.LENGTH_SHORT).show()
                }
            } else {
                // 🔺 REGISTER + ensure EventChats entry + participant
                val user = FirebaseAuth.getInstance().currentUser
                val name = user?.displayName ?: user?.email ?: "Student"
                val now = System.currentTimeMillis()
                val title = binding.tvPostTitle.text.toString().ifBlank { "Event chat" }

                val updates = hashMapOf<String, Any?>(
                    // registration
                    "/EventRegistrations/$postId/$uid/userId" to uid,
                    "/EventRegistrations/$postId/$uid/timestamp" to now,

                    // chat metadata
                    "/EventChats/$postId/title" to title,
                    "/EventChats/$postId/postId" to postId,

                    // participant info
                    "/EventChats/$postId/participants/$uid/name" to name,
                    "/EventChats/$postId/participants/$uid/lastSeenTime" to now
                )

                rootRef.updateChildren(updates).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        isRegistered = true
                        // clear any old "left" flag so chat shows again
                        prefs.edit().remove("left_$postId").apply()
                        updateRegisterButton()
                        Toast.makeText(
                            this,
                            "Registered for this event ✅",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            this,
                            "Failed to register. Try again.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    private fun updateRegisterButton() {
        binding.btnRegisterEvent.text =
            if (isRegistered) "Registered ✅" else "Register"
    }

    // ---------- Open Chat (separate action) ----------

    private fun openEventChat() {
        val intent = Intent(this, EventChatActivity::class.java)
        intent.putExtra("post_id", postId)
        intent.putExtra("event_title", binding.tvPostTitle.text.toString())
        startActivity(intent)
    }

    // ---------- Comments ----------

    private fun loadComments() {
        commentsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val temp = ArrayList<Comment>()
                for (child in snapshot.children) {
                    val c = child.getValue(Comment::class.java)
                    if (c != null) temp.add(c)
                }
                commentList = temp
                commentAdapter.updateList(commentList)

                if (commentList.isNotEmpty()) {
                    binding.rvComments.scrollToPosition(commentList.size - 1)
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun sendComment() {
        val text = binding.etComment.text.toString().trim()
        val uid = currentUserId ?: return

        if (TextUtils.isEmpty(text)) {
            Toast.makeText(this, "Comment can't be empty", Toast.LENGTH_SHORT).show()
            return
        }

        val username = FirebaseAuth.getInstance().currentUser?.email ?: "Student"
        val commentId = commentsRef.push().key ?: return

        val comment = Comment(
            commentId = commentId,
            postId = postId,
            userId = uid,
            username = username,
            text = text,
            timestamp = System.currentTimeMillis()
        )

        commentsRef.child(commentId).setValue(comment).addOnCompleteListener {
            if (it.isSuccessful) {
                binding.etComment.text?.clear()
            }
        }
    }

    // ---------- Util ----------

    private fun formatDateTime(millis: Long): String {
        val sdf = SimpleDateFormat("EEE, MMM d • h:mm a", Locale.getDefault())
        sdf.timeZone = TimeZone.getDefault()
        return sdf.format(Date(millis))
    }

    private fun checkFavoriteStatus() {
        // kept for backwards compatibility – observeFavorite now keeps it updated
    }
}
