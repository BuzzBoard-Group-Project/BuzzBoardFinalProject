package com.example.buzzboardfinalproject

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.buzzboardfinalproject.databinding.ActivityConfirmPollBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue

class ConfirmPollActivity : AppCompatActivity() {

    private lateinit var binding: ActivityConfirmPollBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConfirmPollBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val question = intent.getStringExtra("question")
        val options = intent.getStringArrayListExtra("options") ?: arrayListOf()

        binding.questionText.text = question

        binding.optionsContainer.removeAllViews()
        options.forEachIndexed { i, option ->
            val tv = TextView(this).apply {
                text = "${i + 1}. $option"
                textSize = 16f
                setPadding(8, 8, 8, 8)
            }
            binding.optionsContainer.addView(tv)
        }

        binding.editButton.setOnClickListener { finish() }

        binding.confirmButton.setOnClickListener {
            if (question.isNullOrEmpty() || options.size < 2) {
                Toast.makeText(this, "Incomplete poll", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            uploadPollToFirebase(question, options)
        }
    }

    private fun uploadPollToFirebase(question: String, options: ArrayList<String>) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous"
        val ref = FirebaseDatabase.getInstance().getReference("Polls")
        val pollId = ref.push().key ?: return

        // match your Poll data class fields
        val pollMap = hashMapOf<String, Any?>(
            "id" to pollId,
            "question" to question,
            "options" to options,
            "totals" to List(options.size) { 0 },
            "endTime" to null,                  // set a timestamp if you plan to auto-close
            "createdAt" to ServerValue.TIMESTAMP,
            "createdBy" to uid
        )

        ref.child(pollId).setValue(pollMap).addOnCompleteListener { t ->
            if (!t.isSuccessful) {
                Toast.makeText(this, "Upload failed", Toast.LENGTH_SHORT).show()
                return@addOnCompleteListener
            }
            Toast.makeText(this, "Poll uploaded", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }
    }
}
