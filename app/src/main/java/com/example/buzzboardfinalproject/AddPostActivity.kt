package com.example.buzzboardfinalproject

import android.app.DatePickerDialog
import android.app.ProgressDialog
import android.app.TimePickerDialog
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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AddPostActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddPostBinding
    private var imageUri: Uri? = null
    private lateinit var activityResultLauncher: ActivityResultLauncher<Intent>

    // Holds full date + time in millis for the chosen event
    private var selectedEventDateMillis: Long = 0L

    // NEW: pretty string used on Confirm screen + saved as "time"
    private var eventTimeDisplay: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityAddPostBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Image picker
        activityResultLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result: ActivityResult ->
            if (result.resultCode == RESULT_OK) {
                imageUri = result.data?.data
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
            val photoPicker = Intent(Intent.ACTION_GET_CONTENT).apply { type = "image/*" }
            activityResultLauncher.launch(photoPicker)
        }

        // Date + Time picker for event date
        binding.etEventDate.setOnClickListener {
            showDateTimePicker()
        }

        // Save / Post
        binding.saveNewPostBtn.setOnClickListener {
            val title = binding.TitlePost.text.toString().trim()
            val description = binding.descriptionPost.text.toString().trim()
            val location = binding.LocationPost.text.toString().trim()

            if (title.isEmpty() || description.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (selectedEventDateMillis == 0L) {
                Toast.makeText(this, "Please pick an event date & time", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val intent = Intent(this, ConfirmPostActivity::class.java).apply {
                putExtra("title", title)
                putExtra("description", description)
                putExtra("location", location)
                putExtra("eventDateMillis", selectedEventDateMillis)
                putExtra("time", eventTimeDisplay)          // 👈 NEW
                putExtra("imageUri", imageUri?.toString() ?: "")
            }
            startActivity(intent)
        }

        binding.closeAddPostBtn.setOnClickListener { finish() }
    }

    // ========= DATE & TIME PICKER =========

    private fun showDateTimePicker() {
        val cal = Calendar.getInstance()
        if (selectedEventDateMillis != 0L) {
            cal.timeInMillis = selectedEventDateMillis
        }

        // First pick DATE
        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                cal.set(Calendar.YEAR, year)
                cal.set(Calendar.MONTH, month)
                cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)

                // Then pick TIME
                TimePickerDialog(
                    this,
                    { _, hourOfDay, minute ->
                        cal.set(Calendar.HOUR_OF_DAY, hourOfDay)
                        cal.set(Calendar.MINUTE, minute)
                        cal.set(Calendar.SECOND, 0)
                        cal.set(Calendar.MILLISECOND, 0)

                        selectedEventDateMillis = cal.timeInMillis
                        updateEventDateField()
                    },
                    cal.get(Calendar.HOUR_OF_DAY),
                    cal.get(Calendar.MINUTE),
                    false // 12-hour clock
                ).show()
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun updateEventDateField() {
        if (selectedEventDateMillis == 0L) return
        val sdf = SimpleDateFormat("EEE, MMM d • h:mm a", Locale.getDefault())
        val text = sdf.format(Date(selectedEventDateMillis))

        eventTimeDisplay = text               // 👈 save pretty value
        binding.etEventDate.setText(text)
    }

    // ========= UPLOAD (used if you ever call uploadImage() directly) =========

    private fun uploadImage() {
        when {
            imageUri == null -> {
                Toast.makeText(this, "Please select image first.", Toast.LENGTH_LONG).show()
            }
            TextUtils.isEmpty(binding.descriptionPost.text.toString()) -> {
                Toast.makeText(this, "Please write caption.", Toast.LENGTH_LONG).show()
            }
            selectedEventDateMillis == 0L -> {
                Toast.makeText(this, "Please pick an event date & time.", Toast.LENGTH_LONG).show()
            }
            else -> {
                val progressDialog = ProgressDialog(this).apply {
                    setTitle("Adding New Post")
                    setMessage("Please wait, we are adding your picture...")
                    show()
                }

                try {
                    val bitmap: Bitmap =
                        MediaStore.Images.Media.getBitmap(contentResolver, imageUri)
                    val baos = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 50, baos)
                    val imageBase64 = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT)

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
                    postMap["eventDateMillis"] = selectedEventDateMillis
                    if (eventTimeDisplay.isNotEmpty()) {
                        postMap["time"] = eventTimeDisplay
                    }

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
