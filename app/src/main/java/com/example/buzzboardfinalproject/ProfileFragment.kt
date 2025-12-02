package com.example.buzzboardfinalproject

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.buzzboardfinalproject.databinding.FragmentProfileBinding
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var postsRef: DatabaseReference
    private lateinit var usersRef: DatabaseReference
    private lateinit var followingRef: DatabaseReference
    private lateinit var followersRef: DatabaseReference

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        val view = binding.root

        val currentUser = FirebaseAuth.getInstance().currentUser
        val userId = currentUser?.uid ?: return view

        val db = FirebaseDatabase.getInstance()
        postsRef = db.getReference("Posts")
        usersRef = db.getReference("Users").child(userId)
        followingRef = db.getReference("Following").child(userId)
        followersRef = db.getReference("Followers").child(userId)

        // Header
        loadUserProfile()

        // Counts
        loadPostCount(userId)
        loadFollowingCount()
        loadFollowersCount()

        // Edit profile
        binding.btnEditProfile.setOnClickListener {
            startActivity(Intent(requireContext(), EditProfileActivity::class.java))
        }

        // Click stats -> list screens
        binding.layoutFollowing.setOnClickListener {
            val i = Intent(requireContext(), FollowListActivity::class.java)
            i.putExtra("mode", "following")
            startActivity(i)
        }

        binding.layoutFollowers.setOnClickListener {
            val i = Intent(requireContext(), FollowListActivity::class.java)
            i.putExtra("mode", "followers")
            startActivity(i)
        }

        // Tabs
        val tabAdapter = ProfileTabAdapter(this)
        binding.profileViewPager.adapter = tabAdapter

        TabLayoutMediator(binding.profileTabLayout, binding.profileViewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Posts"
                1 -> "Favorites"
                else -> "Registered"
            }
        }.attach()

        return view
    }

    // ---------- header ----------

    private fun loadUserProfile() {
        usersRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val name = snapshot.child("name").getValue(String::class.java)
                val bio = snapshot.child("bio").getValue(String::class.java)
                val profileImage = snapshot.child("profileImage").getValue(String::class.java)
                val accountType = snapshot.child("accountType").getValue(String::class.java)

                binding.tvProfileName.text = name ?: "BuzzBoard User"
                binding.tvBio.text = bio ?: "VSU Student 🐝"

                if (!profileImage.isNullOrEmpty()) {
                    try {
                        val bytes = Base64.decode(profileImage, Base64.DEFAULT)
                        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        binding.imgProfilePicture.setImageBitmap(bitmap)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                applyBadge(accountType)
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun applyBadge(accountType: String?) {
        val badgeView = binding.imgProfileBadge
        when (accountType?.lowercase()) {
            "student" -> {
                badgeView.setImageResource(R.drawable.ic_badge_student)
                badgeView.visibility = View.VISIBLE
            }
            "organization" -> {
                badgeView.setImageResource(R.drawable.ic_badge_organization)
                badgeView.visibility = View.VISIBLE
            }
            "official", "buzzboard", "official buzzboard" -> {
                badgeView.setImageResource(R.drawable.ic_badge_buzzboard)
                badgeView.visibility = View.VISIBLE
            }
            else -> badgeView.visibility = View.GONE
        }
    }

    // ---------- counts ----------

    private fun loadPostCount(userId: String) {
        postsRef.orderByChild("publisher").equalTo(userId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    binding.tvPostCount.text = snapshot.childrenCount.toString()
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun loadFollowingCount() {
        followingRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                binding.tvFollowingCount.text = snapshot.childrenCount.toString()
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun loadFollowersCount() {
        followersRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                binding.tvFollowersCount.text = snapshot.childrenCount.toString()
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
