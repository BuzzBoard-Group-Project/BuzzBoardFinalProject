package com.example.buzzboardfinalproject

import android.app.ProgressDialog
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.buzzboardfinalproject.databinding.ActivityEditProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.io.ByteArrayOutputStream

class EditProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditProfileBinding
    private var imageUri: Uri? = null
    private lateinit var pickImageLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val userRef = FirebaseDatabase.getInstance().getReference("Users").child(userId)

        // Pick image from gallery
        pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                imageUri = result.data?.data
                binding.imgEditProfile.setImageURI(imageUri)
            }
        }

        binding.imgEditProfile.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/*"
            pickImageLauncher.launch(intent)
        }

        binding.btnSaveProfile.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            val bio = binding.etBio.text.toString().trim()

            val progress = ProgressDialog(this)
            progress.setMessage("Updating profile...")
            progress.show()

            val updates = HashMap<String, Any>()
            updates["name"] = name
            updates["bio"] = bio

            if (imageUri != null) {
                val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, imageUri)
                val baos = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos)
                val imageBase64 = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT)
                updates["profileImage"] = imageBase64
            }

            userRef.updateChildren(updates).addOnCompleteListener {
                progress.dismiss()
                Toast.makeText(this, "Profile updated!", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
}
