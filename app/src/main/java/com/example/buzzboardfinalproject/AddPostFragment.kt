package com.example.buzzboardfinalproject

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController

class AddPostFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_add_post, container, false)

        // Match IDs from the XML
        val pictureOption = view.findViewById<View>(R.id.picturePostCard)
        val pollOption = view.findViewById<View>(R.id.pollPostCard)
        val reminderOption = view.findViewById<View>(R.id.reminderPostCard)

        // Set navigation for each option
        pictureOption.setOnClickListener {
            findNavController().navigate(R.id.action_addPostFragment_to_postPictureFragment)
        }

        pollOption.setOnClickListener {
            findNavController().navigate(R.id.action_addPostFragment_to_postPollFragment)
        }

        reminderOption.setOnClickListener {
            findNavController().navigate(R.id.action_addPostFragment_to_reminderFragment)
        }

        return view
    }
}
