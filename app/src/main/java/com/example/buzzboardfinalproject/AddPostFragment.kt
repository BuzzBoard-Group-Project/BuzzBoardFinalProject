package com.example.buzzboardfinalproject

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment

class AddPostFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_add_post, container, false)

        // Match IDs from the XML
        val pictureOption = view.findViewById<View>(R.id.picturePostCard)
        val pollOption = view.findViewById<View>(R.id.pollPostCard)
        val cameraOption = view.findViewById<View>(R.id.CameraPostCard)

        // Launch AddPostActivity when picture option is clicked
        pictureOption.setOnClickListener {
            val intent = Intent(requireContext(), AddPostActivity::class.java)
            startActivity(intent)
        }

        // You can set up poll and camera to go to their own activities too
        pollOption.setOnClickListener {
            val intent = Intent(requireContext(), AddPollActivity::class.java)
            startActivity(intent)
        }

        cameraOption.setOnClickListener {
            startActivity(Intent(requireContext(), CameraActivity::class.java))
        }


            return view
    }
}
