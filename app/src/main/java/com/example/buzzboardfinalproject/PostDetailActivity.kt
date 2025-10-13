package com.example.buzzboardfinalproject

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import androidx.appcompat.app.AppCompatActivity
import com.example.buzzboardfinalproject.databinding.ActivityPostDetailBinding
import com.google.firebase.database.FirebaseDatabase

class PostDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPostDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inflate layout with ViewBinding
        binding = ActivityPostDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ✅ Get the post ID from the intent
        val postId = intent.getStringExtra("post_id") ?: return

        // ✅ Reference the specific post in Firebase
        val databaseRef = FirebaseDatabase.getInstance().getReference("Posts").child(postId)

        // ✅ Fetch the post data
        databaseRef.get().addOnSuccessListener { snapshot ->
            val post = snapshot.getValue(Post::class.java)
            if (post != null) {
                // 📝 Bind post data to views
                binding.tvPostTitle.text = post.title
                binding.tvPostDetailDescription.text = post.description
                binding.tvLocation.text = post.location
                binding.tvPostDetailDate.text = post.time

                // 🖼️ Decode Base64 → Bitmap and display image
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
        }.addOnFailureListener {
            // ❌ Handle any errors (optional)
            it.printStackTrace()
        }
    }
}
