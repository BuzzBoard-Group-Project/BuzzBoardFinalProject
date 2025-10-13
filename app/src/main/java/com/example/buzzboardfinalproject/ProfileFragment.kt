package com.example.buzzboardfinalproject

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.buzzboardfinalproject.databinding.FragmentProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var databaseRef: DatabaseReference
    private lateinit var userPosts: ArrayList<Post>
    private lateinit var adapter: PostAdapter2

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        val view = binding.root

        val currentUser = FirebaseAuth.getInstance().currentUser
        databaseRef = FirebaseDatabase.getInstance().getReference("Posts")

        // Set dummy data for now
        binding.tvProfileName.text = currentUser?.email ?: "BuzzBoard User"
        binding.tvBio.text = "VSU Student | AbstraKt 🐝"

        // Decode Base64 profile image if you have one stored later
        // binding.imgProfilePicture.setImageBitmap(decodedImage)

        // Load user posts
        userPosts = ArrayList()
        adapter = PostAdapter2(requireContext(), userPosts)
        binding.recyclerUserPosts.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerUserPosts.adapter = adapter

        fetchUserPosts(currentUser?.uid ?: "")
        return view
    }

    private fun fetchUserPosts(userId: String) {
        databaseRef.orderByChild("publisher").equalTo(userId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    userPosts.clear()
                    for (dataSnap in snapshot.children) {
                        val post = dataSnap.getValue(Post::class.java)
                        if (post != null) userPosts.add(post)
                    }
                    binding.tvPostCount.text = userPosts.size.toString()
                    adapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
