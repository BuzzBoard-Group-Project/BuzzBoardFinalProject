package com.example.buzzboardfinalproject

import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class AddPollActivity : AppCompatActivity() {

    private lateinit var questionEditText: EditText
    private lateinit var optionsContainer: LinearLayout
    private val optionViews = mutableListOf<EditText>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_poll)

        val toolbar: androidx.appcompat.widget.Toolbar = findViewById(R.id.toolbar)
        val addOptionButton: android.view.View = findViewById(R.id.addOptionButton)
        val submitPollButton: android.view.View = findViewById(R.id.submitPollButton)
        questionEditText = findViewById(R.id.questionEditText)
        optionsContainer = findViewById(R.id.optionsContainer)

        toolbar.setNavigationOnClickListener { finish() }

        addOptionButton.setOnClickListener { addPollOption() }

        submitPollButton.setOnClickListener {
            val question = questionEditText.text.toString().trim()
            val options = optionViews.map { it.text.toString().trim() }.filter { it.isNotEmpty() }

            if (question.isEmpty()) {
                Toast.makeText(this, "Enter a question", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (options.size < 2) {
                Toast.makeText(this, "Enter at least two options", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val intent = Intent(this, ConfirmPollActivity::class.java).apply {
                putExtra("question", question)
                putStringArrayListExtra("options", ArrayList(options))
            }
            startActivity(intent)
            finish()
        }

        repeat(2) { addPollOption() }
    }

    private fun addPollOption() {
        val optionEditText = EditText(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            hint = "Option ${optionViews.size + 1}"
            setPadding(24, 24, 24, 24)
            textSize = 16f
            setTextColor(resources.getColor(android.R.color.black))
            setBackgroundResource(android.R.drawable.edit_text)
        }
        optionsContainer.addView(optionEditText)
        optionViews.add(optionEditText)
    }
}
