package com.example.buzzboardfinalproject

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.buzzboardfinalproject.databinding.FragmentPostPollBinding

class PostPollFragment : Fragment() {

    private var _binding: FragmentPostPollBinding? = null
    private val binding get() = _binding!!

    private val optionViews = mutableListOf<EditText>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPostPollBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        // Back button
        binding.toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Add option button
        binding.addOptionButton.setOnClickListener {
            addPollOption()
        }

        // Submit poll button
        binding.submitPollButton.setOnClickListener {
            val question = binding.questionEditText.text.toString().trim()
            val options = optionViews.map { it.text.toString().trim() }.filter { it.isNotEmpty() }

            if (question.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter a question", Toast.LENGTH_SHORT).show()
            } else if (options.size < 2) {
                Toast.makeText(requireContext(), "Enter at least two options", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Poll submitted!", Toast.LENGTH_SHORT).show()
                // Implement poll submission logic here
            }
        }

        // Add initial two options
        repeat(2) { addPollOption() }
    }

    private fun addPollOption() {
        val optionEditText = EditText(requireContext()).apply {
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

        binding.optionsContainer.addView(optionEditText)
        optionViews.add(optionEditText)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
