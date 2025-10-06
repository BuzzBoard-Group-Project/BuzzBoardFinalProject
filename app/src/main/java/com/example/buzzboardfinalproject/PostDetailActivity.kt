package com.example.buzzboardfinalproject

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.buzzboardfinalproject.databinding.ActivityPostDetailBinding
import com.google.firebase.database.FirebaseDatabase

class PostDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPostDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inflate the activity layout using Activity binding
        binding = ActivityPostDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get the post ID from the intent
        val postId = intent.getStringExtra("post_id") ?: return

        // Reference the specific post in Firebase
        val databaseRef = FirebaseDatabase.getInstance().getReference("Posts").child(postId)

        // Fetch the post data
        databaseRef.get().addOnSuccessListener { snapshot ->
            val post = snapshot.getValue(Post::class.java)
            if (post != null) {
                // Bind post data to views
                binding.tvPublisher.text = post.publisher
                binding.tvPostTitle.text = post.title
                binding.tvPostDetailDescription.text = post.description
                binding.tvLocation.text = post.location
                binding.tvPostDetailDate.text = post.time
                Glide.with(this).load(post.postimage).into(binding.imgPostDetail)
            }
        }.addOnFailureListener {
            // Handle any errors here
        }
    }
}
