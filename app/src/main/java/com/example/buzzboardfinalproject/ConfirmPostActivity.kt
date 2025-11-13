package com.example.buzzboardfinalproject

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.buzzboardfinalproject.databinding.ActivityConfirmPostBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import java.io.InputStream

class ConfirmPostActivity : AppCompatActivity() {

    private lateinit var binding: ActivityConfirmPostBinding
    private var imageUri: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConfirmPostBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get data passed from AddPostActivity
        val title = intent.getStringExtra("title")
        val description = intent.getStringExtra("description")
        val location = intent.getStringExtra("location")
        val time = intent.getStringExtra("time")          // 👈 formatted date/time
        imageUri = intent.getStringExtra("imageUri")

        // Fill preview fields
        binding.titleText.text = title
        binding.descriptionText.text = description
        binding.locationText.text = location
        binding.timeText.text = time ?: ""               // 👈 show under location

        // Preview image
        if (!imageUri.isNullOrEmpty()) {
            try {
                Glide.with(this).load(Uri.parse(imageUri)).into(binding.previewImage)
            } catch (e: Exception) {
                Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show()
            }
        }

        // Cancel → return to AddPostActivity
        binding.editButton.setOnClickListener {
            finish()
        }

        // Confirm → upload to Firebase and return to main feed
        binding.confirmButton.setOnClickListener {
            uploadPostToFirebase(title, description, location, time, imageUri)
        }
    }

    private fun uploadPostToFirebase(
        title: String?,
        description: String?,
        location: String?,
        time: String?,
        imageUri: String?
    ) {
        if (title.isNullOrEmpty() || description.isNullOrEmpty() || imageUri.isNullOrEmpty()) {
            Toast.makeText(this, "Missing information", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val inputStream: InputStream? = contentResolver.openInputStream(Uri.parse(imageUri))
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            val baos = java.io.ByteArrayOutputStream()
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 50, baos)
            val imageBytes = baos.toByteArray()
            val imageBase64 = Base64.encodeToString(imageBytes, Base64.DEFAULT)

            val ref = FirebaseDatabase.getInstance().reference.child("Posts")
            val firestore = FirebaseFirestore.getInstance()
            val postId = ref.push().key!!
            val eventDateMillis = intent.getLongExtra("eventDateMillis", 0L)

            val postMap = HashMap<String, Any>()
            postMap["postid"] = postId
            postMap["title"] = title
            postMap["description"] = description
            postMap["location"] = location ?: ""
            postMap["postimage"] = imageBase64
            postMap["publisher"] = FirebaseAuth.getInstance().currentUser!!.uid
            postMap["eventDateMillis"] = eventDateMillis

            // NEW: store formatted time so Home/Detail can display it
            if (!time.isNullOrEmpty()) {
                postMap["time"] = time
            }

            ref.child(postId).updateChildren(postMap)
            firestore.collection("Posts").document(postId).set(postMap)

            Toast.makeText(this, " Post uploaded successfully", Toast.LENGTH_LONG).show()

            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        } catch (e: Exception) {
            Toast.makeText(this, "Upload failed", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }
}
