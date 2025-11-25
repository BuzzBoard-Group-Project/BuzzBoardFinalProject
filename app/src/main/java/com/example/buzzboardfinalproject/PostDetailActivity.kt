package com.example.buzzboardfinalproject

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

    private lateinit var favoritesRef: DatabaseReference
    private lateinit var registrationsRef: DatabaseReference
    private lateinit var postRef: DatabaseReference
    private val eventChatsRef: DatabaseReference by lazy {
        FirebaseDatabase.getInstance().getReference("EventChats")
    }

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

        binding.btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        postId = intent.getStringExtra("post_id") ?: return

        val db = FirebaseDatabase.getInstance()
        favoritesRef = db.getReference("Favorites")
        registrationsRef = db.getReference("EventRegistrations").child(postId)
        postRef = db.getReference("Posts").child(postId)
        commentsRef = db.getReference("Comments").child(postId)

        // comments RecyclerView
        commentAdapter = CommentAdapter(commentList)
        binding.rvComments.layoutManager = LinearLayoutManager(this)
        binding.rvComments.adapter = commentAdapter

        loadPostDetails()
        checkFavoriteStatus()
        checkIfUserRegistered()
        loadComments()

        binding.btnFavorite.setOnClickListener { toggleFavorite() }
        binding.btnRegisterEvent.setOnClickListener { toggleRegistration() }

        binding.btnSendComment.setOnClickListener {
            sendComment()
        }
    }

    // ========= POST DETAILS =========

    private fun loadPostDetails() {
        postRef.get().addOnSuccessListener { snapshot ->
            val post = snapshot.getValue(Post::class.java)
            if (post != null) {
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
    }

    // ========= COMMENTS =========

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

    // ========= FAVORITE =========

    private fun checkFavoriteStatus() {
        val uid = currentUserId ?: return
        favoritesRef.child(uid).child(postId).get().addOnSuccessListener { snapshot ->
            isFavorited = snapshot.exists()
            updateFavoriteIcon()
        }
    }

    private fun toggleFavorite() {
        val uid = currentUserId ?: return
        isFavorited = !isFavorited

        if (isFavorited) {
            favoritesRef.child(uid).child(postId).setValue(true)
            Toast.makeText(this, "Added to favorites ❤️", Toast.LENGTH_SHORT).show()
        } else {
            favoritesRef.child(uid).child(postId).removeValue()
            Toast.makeText(this, "Removed from favorites 🤍", Toast.LENGTH_SHORT).show()
        }
        updateFavoriteIcon()
    }

    private fun updateFavoriteIcon() {
        if (isFavorited) {
            binding.btnFavorite.setImageResource(R.drawable.baseline_favorite_24)
        } else {
            binding.btnFavorite.setImageResource(R.drawable.baseline_favorite_border_24)
        }
    }

    // ========= REGISTER + CREATE CHAT =========

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

        registrationsRef.child(uid).get().addOnSuccessListener { snap ->
            if (snap.exists()) {
                // unregister
                registrationsRef.child(uid).removeValue()
                isRegistered = false
                Toast.makeText(this, "Registration canceled.", Toast.LENGTH_SHORT).show()
            } else {
                // register
                val data = mapOf(
                    "userId" to uid,
                    "timestamp" to System.currentTimeMillis()
                )
                registrationsRef.child(uid).setValue(data)
                isRegistered = true
                Toast.makeText(this, "Registered for this event ✅", Toast.LENGTH_SHORT).show()

                // 🔥 create/join chat for this event
                createOrJoinChat(uid)

                // (optional) open chat immediately:
                // openChat()
            }
            updateRegisterButton()
        }
    }

    private fun updateRegisterButton() {
        binding.btnRegisterEvent.text =
            if (isRegistered) "Registered ✅" else "Register"
    }

    /**
     * Ensure there is a chat room for this post, and that this user is a participant.
     * Uses postId as chatId so it's 1 chat per event.
     */
    private fun createOrJoinChat(uid: String) {
        val chatRef = eventChatsRef.child(postId)

        val title = binding.tvPostTitle.text.toString().ifBlank { "Event chat" }

        val now = System.currentTimeMillis()

        val updates = hashMapOf<String, Any>(
            "postId" to postId,
            "title" to title,
            "participants/$uid" to true,
            "lastMessageTime" to now
        )

        chatRef.updateChildren(updates)
    }


    // If you want to open chat right away after registering:
    private fun openChat() {
        val intent = Intent(this, EventChatActivity::class.java)
        intent.putExtra("post_id", postId)  // chatId == postId
        startActivity(intent)
    }

    // ========= UTIL =========

    private fun formatDateTime(millis: Long): String {
        val sdf = SimpleDateFormat("EEE, MMM d • h:mm a", Locale.getDefault())
        sdf.timeZone = TimeZone.getDefault()
        return sdf.format(Date(millis))
    }
}
