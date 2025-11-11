package com.example.buzzboardfinalproject

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.buzzboardfinalproject.databinding.ActivityAddPostBinding

class AddPostActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddPostBinding
    private var imageUri: Uri? = null
    private lateinit var activityResultLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityAddPostBinding.inflate(layoutInflater)
        setContentView(binding.root)

        activityResultLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result: ActivityResult ->
            if (result.resultCode == RESULT_OK) {
                imageUri = result.data?.data
                if (imageUri != null) {
                    binding.imagePost.setImageURI(imageUri)
                } else {
                    Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show()
            }
        }

        binding.imagePost.setOnClickListener {
            val picker = Intent(Intent.ACTION_GET_CONTENT).apply { type = "image/*" }
            activityResultLauncher.launch(picker)
        }

        binding.saveNewPostBtn.setOnClickListener {
            val title = binding.TitlePost.text.toString().trim()
            val description = binding.descriptionPost.text.toString().trim()
            val location = binding.LocationPost.text.toString().trim()
            val time = binding.TimePost.text.toString().trim()

            if (title.isEmpty() || description.isEmpty()) {
                Toast.makeText(this, "Enter title and description", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (imageUri == null) {
                Toast.makeText(this, "Pick an image", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val intent = Intent(this, ConfirmPostActivity::class.java).apply {
                putExtra("title", title)
                putExtra("description", description)
                putExtra("location", location)
                putExtra("time", time)
                putExtra("imageUri", imageUri.toString())
            }
            startActivity(intent)
        }

        binding.closeAddPostBtn.setOnClickListener { finish() }
    }
}
