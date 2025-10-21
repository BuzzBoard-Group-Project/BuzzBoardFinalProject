package com.example.buzzboardfinalproject

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import androidx.appcompat.app.AppCompatActivity
import com.example.buzzboardfinalproject.databinding.ActivityPostDetailBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class PostDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPostDetailBinding
    private var isFavorited = false
    private lateinit var postId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPostDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ✅ Back button
        binding.btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // ✅ Get post ID
        postId = intent.getStringExtra("post_id") ?: return

        // ✅ Firebase references
        val postRef = FirebaseDatabase.getInstance().getReference("Posts").child(postId)
        val favoritesRef = FirebaseDatabase.getInstance().getReference("Favorites")
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        // ✅ Fetch post data
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
                        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                        binding.imgPostDetail.setImageBitmap(bitmap)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        binding.imgPostDetail.setImageResource(R.drawable.add_image_icon)
                    }
                } else {
                    binding.imgPostDetail.setImageResource(R.drawable.add_image_icon)
                }
            }
        }

        // ✅ Check if post is already favorited
        favoritesRef.child(currentUserId).child(postId).get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                isFavorited = true
                binding.btnFavorite.setImageResource(R.drawable.baseline_favorite_24) // filled
            } else {
                binding.btnFavorite.setImageResource(R.drawable.baseline_favorite_border_24) // empty
            }
        }

        // ✅ Toggle favorite on click
        binding.btnFavorite.setOnClickListener {
            isFavorited = !isFavorited
            if (isFavorited) {
                favoritesRef.child(currentUserId).child(postId).setValue(true)
                binding.btnFavorite.setImageResource(R.drawable.baseline_favorite_24) // ❤️ filled
            } else {
                favoritesRef.child(currentUserId).child(postId).removeValue()
                binding.btnFavorite.setImageResource(R.drawable.baseline_favorite_border_24) // 🤍 empty
            }
        }
    }
}
