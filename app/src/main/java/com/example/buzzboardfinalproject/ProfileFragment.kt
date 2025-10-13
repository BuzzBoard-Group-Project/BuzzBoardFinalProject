package com.example.buzzboardfinalproject

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.example.buzzboardfinalproject.databinding.FragmentProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var postsRef: DatabaseReference
    private lateinit var usersRef: DatabaseReference
    private lateinit var userPosts: ArrayList<Post>
    private lateinit var adapter: PostAdapter2

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        val view = binding.root

        val currentUser = FirebaseAuth.getInstance().currentUser
        val userId = currentUser?.uid ?: return view

        postsRef = FirebaseDatabase.getInstance().getReference("Posts")
        usersRef = FirebaseDatabase.getInstance().getReference("Users").child(userId)

        // ✅ Load and display the user's info
        loadUserProfile()

        // ✅ Edit profile button
        binding.btnEditProfile.setOnClickListener {
            startActivity(Intent(requireContext(), EditProfileActivity::class.java))
        }

        // ✅ Load user's posts
        userPosts = ArrayList()
        adapter = PostAdapter2(requireContext(), userPosts)
        binding.recyclerUserPosts.layoutManager = GridLayoutManager(requireContext(), 3)
        binding.recyclerUserPosts.adapter = adapter

        fetchUserPosts(userId)
        return view
    }

    private fun loadUserProfile() {
        usersRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val name = snapshot.child("name").getValue(String::class.java)
                val bio = snapshot.child("bio").getValue(String::class.java)
                val profileImage = snapshot.child("profileImage").getValue(String::class.java)

                binding.tvProfileName.text = name ?: "BuzzBoard User"
                binding.tvBio.text = bio ?: "VSU Student | AbstraKt 🐝"

                // ✅ Decode Base64 image
                if (!profileImage.isNullOrEmpty()) {
                    try {
                        val imageBytes = Base64.decode(profileImage, Base64.DEFAULT)
                        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                        binding.imgProfilePicture.setImageBitmap(bitmap)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun fetchUserPosts(userId: String) {
        postsRef.orderByChild("publisher").equalTo(userId)
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
