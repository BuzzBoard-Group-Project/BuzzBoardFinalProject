package com.example.buzzboardfinalproject

import android.app.ProgressDialog
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.buzzboardfinalproject.databinding.ActivityEditProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.io.ByteArrayOutputStream
import android.graphics.BitmapFactory


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

        // 🔹 Load existing profile data (name, bio, image, accountType)
        loadUserProfile(userRef)

        // 🔹 Set up Account Type Spinner
        setupAccountTypeSpinner()

        // 🔹 Pick image from gallery
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

        // 🔹 Save Profile Button
        binding.btnSaveProfile.setOnClickListener {
            saveProfile(userRef)
        }
    }

    private fun loadUserProfile(userRef: DatabaseReference) {
        userRef.get().addOnSuccessListener { snapshot ->
            binding.etName.setText(snapshot.child("name").value?.toString() ?: "")
            binding.etBio.setText(snapshot.child("bio").value?.toString() ?: "")

            // Load account type selection
            val accountType = snapshot.child("accountType").value?.toString()
            val index = when (accountType) {
                "student" -> 0
                "organization" -> 1
                "official" -> 2
                else -> 0
            }
            binding.spinnerAccountType.setSelection(index)

            // Load profile image if exists
            val profileImageBase64 = snapshot.child("profileImage").value?.toString()
            if (!profileImageBase64.isNullOrEmpty()) {
                try {
                    val bytes = Base64.decode(profileImageBase64, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    binding.imgEditProfile.setImageBitmap(bitmap)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun setupAccountTypeSpinner() {
        val adapter = ArrayAdapter.createFromResource(
            this,
            R.array.account_type_options,
            android.R.layout.simple_spinner_item
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerAccountType.adapter = adapter
    }

    private fun saveProfile(userRef: DatabaseReference) {
        val name = binding.etName.text.toString().trim()
        val bio = binding.etBio.text.toString().trim()

        val progress = ProgressDialog(this)
        progress.setMessage("Updating profile...")
        progress.show()

        val updates = HashMap<String, Any>()
        updates["name"] = name
        updates["bio"] = bio

        // Handle selected account type
        val selectedType = when (binding.spinnerAccountType.selectedItemPosition) {
            0 -> "student"
            1 -> "organization"
            2 -> "official"
            else -> "student"
        }
        updates["accountType"] = selectedType

        // Handle profile image upload
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
