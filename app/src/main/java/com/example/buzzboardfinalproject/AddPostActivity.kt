package com.example.buzzboardfinalproject

import android.app.ProgressDialog
import android.content.ContentResolver
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.TextUtils
import android.util.Base64
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.buzzboardfinalproject.databinding.ActivityAddPostBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import java.io.ByteArrayOutputStream

class AddPostActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddPostBinding
    private var imageUri: Uri? = null
    private lateinit var activityResultLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityAddPostBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ✅ Image picker setup
        activityResultLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result: ActivityResult ->
            if (result.resultCode == RESULT_OK) {
                val data = result.data
                imageUri = data?.data
                if (imageUri != null) {
                    binding.imagePost.setImageURI(imageUri)
                } else {
                    Toast.makeText(this, "No Image Selected", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "No Image Selected", Toast.LENGTH_SHORT).show()
            }
        }

        binding.imagePost.setOnClickListener {
            val photoPicker = Intent(Intent.ACTION_GET_CONTENT)
            photoPicker.type = "image/*"
            activityResultLauncher.launch(photoPicker)
        }

        // ✅ Save / Post button
        binding.saveNewPostBtn.setOnClickListener {
            val title = binding.TitlePost.text.toString()
            val description = binding.descriptionPost.text.toString()
            val location = binding.LocationPost.text.toString()
            val time = binding.TimePost.text.toString()

            if (title.isEmpty() || description.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 👉 Go to confirmation page
            val intent = Intent(this, ConfirmPostActivity::class.java)
            intent.putExtra("title", title)
            intent.putExtra("description", description)
            intent.putExtra("location", location)
            intent.putExtra("time", time)
            intent.putExtra("imageUri", imageUri.toString())
            startActivity(intent)
        }

        // ✅ Close button
        binding.closeAddPostBtn.setOnClickListener {
            finish()
        }
    }

    private fun uploadImage() {
        when {
            imageUri == null -> {
                Toast.makeText(this, "Please select image first.", Toast.LENGTH_LONG).show()
            }

            TextUtils.isEmpty(binding.descriptionPost.text.toString()) -> {
                Toast.makeText(this, "Please write caption.", Toast.LENGTH_LONG).show()
            }

            else -> {
                val progressDialog = ProgressDialog(this)
                progressDialog.setTitle("Adding New Post")
                progressDialog.setMessage("Please wait, we are adding your picture...")
                progressDialog.show()

                try {
                    // Convert image to Base64
                    val bitmap: Bitmap = MediaStore.Images.Media.getBitmap(contentResolver, imageUri)
                    val baos = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 50, baos)
                    val imageBytes = baos.toByteArray()
                    val imageBase64 = Base64.encodeToString(imageBytes, Base64.DEFAULT)

                    val ref = FirebaseDatabase.getInstance().reference.child("Posts")
                    val firestore = FirebaseFirestore.getInstance()
                    val postId = ref.push().key!!

                    val postMap = HashMap<String, Any>()
                    postMap["postid"] = postId
                    postMap["description"] = binding.descriptionPost.text.toString().lowercase()
                    postMap["publisher"] = FirebaseAuth.getInstance().currentUser!!.uid
                    postMap["postimage"] = imageBase64
                    postMap["title"] = binding.TitlePost.text.toString()
                    postMap["location"] = binding.LocationPost.text.toString()
                    postMap["time"] = binding.TimePost.text.toString()

                    ref.child(postId).updateChildren(postMap)
                    firestore.collection("Posts").document(postId).set(postMap)

                    Toast.makeText(this, "Post uploaded successfully.", Toast.LENGTH_LONG).show()
                    startActivity(Intent(this@AddPostActivity, MainActivity::class.java))
                    finish()
                } catch (e: Exception) {
                    Toast.makeText(this, "Failed to upload image", Toast.LENGTH_SHORT).show()
                    e.printStackTrace()
                } finally {
                    progressDialog.dismiss()
                }
            }
        }
    }

    private fun getFileExtension(fileUri: Uri): String? {
        val contentResolver: ContentResolver = contentResolver
        val mime = MimeTypeMap.getSingleton()
        return mime.getExtensionFromMimeType(contentResolver.getType(fileUri))
    }
}
