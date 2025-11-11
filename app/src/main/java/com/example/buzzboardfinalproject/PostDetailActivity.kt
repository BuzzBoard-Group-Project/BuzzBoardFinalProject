package com.example.buzzboardfinalproject

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.buzzboardfinalproject.databinding.ActivityPostDetailBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class PostDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPostDetailBinding
    private var isFavorited = false
    private var isRegistered = false
    private lateinit var postId: String

    private lateinit var favoritesRef: DatabaseReference
    private lateinit var registrationsRef: DatabaseReference
    private lateinit var postRef: DatabaseReference

    private val currentUserId: String?
        get() = FirebaseAuth.getInstance().currentUser?.uid

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPostDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // back
        binding.btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // which post?
        postId = intent.getStringExtra("post_id") ?: return

        val db = FirebaseDatabase.getInstance()
        favoritesRef = db.getReference("Favorites")
        registrationsRef = db.getReference("EventRegistrations").child(postId)
        postRef = db.getReference("Posts").child(postId)

        loadPostDetails()
        checkFavoriteStatus()
        checkIfUserRegistered()

        // favorites
        binding.btnFavorite.setOnClickListener {
            toggleFavorite()
        }

        // register → join chat
        binding.btnRegisterEvent.setOnClickListener {
            toggleRegistration()
        }
    }

    private fun loadPostDetails() {
        postRef.get().addOnSuccessListener { snapshot ->
            val post = snapshot.getValue(Post::class.java)
            if (post != null) {
                binding.tvPostTitle.text = post.title
                binding.tvPostDetailDescription.text = post.description
                binding.tvLocation.text = post.location
                binding.tvPostDetailDate.text = post.time

                if (!post.postimage.isNullOrEmpty()) {
                    try {
                        val imageBytes = Base64.decode(post.postimage, Base64.DEFAULT)
                        val bitmap =
                            BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
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

    // ------------------ FAVORITES ------------------
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

    // ------------------ REGISTRATION + CHAT ------------------
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
                // 1) unregister
                registrationsRef.child(uid).removeValue()
                // 2) remove from chat participants
                FirebaseDatabase.getInstance()
                    .getReference("EventChats")
                    .child(postId)
                    .child("participants")
                    .child(uid)
                    .removeValue()

                isRegistered = false
                Toast.makeText(this, "Registration canceled.", Toast.LENGTH_SHORT).show()
                updateRegisterButton()
            } else {
                // 1) add to registrations
                val data = mapOf(
                    "userId" to uid,
                    "timestamp" to System.currentTimeMillis()
                )
                registrationsRef.child(uid).setValue(data)

                // 2) make sure chat exists + add participant
                val chatRef = FirebaseDatabase.getInstance()
                    .getReference("EventChats")
                    .child(postId)

                // set title (from post)
                val roomName =
                    binding.tvPostTitle.text?.toString()?.ifEmpty { "Event Chat" } ?: "Event Chat"
                chatRef.child("title").setValue(roomName)
                chatRef.child("participants").child(uid).setValue(true)

                isRegistered = true
                updateRegisterButton()
                Toast.makeText(this, "Registered for this event ✅", Toast.LENGTH_SHORT).show()

                // 3) open chat
                val i = Intent(this, EventChatActivity::class.java)
                i.putExtra("post_id", postId)   // chatId == postId
                startActivity(i)
            }
        }
    }

    private fun updateRegisterButton() {
        binding.btnRegisterEvent.text = if (isRegistered) "Registered ✅" else "Register"
    }
}
