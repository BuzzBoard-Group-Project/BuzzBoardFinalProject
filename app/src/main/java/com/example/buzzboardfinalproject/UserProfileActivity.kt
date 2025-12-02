package com.example.buzzboardfinalproject

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.buzzboardfinalproject.databinding.ActivityUserProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class UserProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUserProfileBinding
    private val db by lazy { FirebaseDatabase.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }

    private lateinit var userId: String
    private val currentUid get() = auth.currentUser?.uid

    private lateinit var usersRef: DatabaseReference
    private lateinit var followingRef: DatabaseReference
    private lateinit var followersRef: DatabaseReference

    private var isFollowing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        userId = intent.getStringExtra("user_id") ?: run {
            Toast.makeText(this, "No user id", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // If they somehow open their own page, just finish (or you could open Profile tab)
        if (userId == currentUid) {
            Toast.makeText(this, "This is your profile.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        usersRef = db.getReference("Users").child(userId)
        followingRef = db.getReference("Following")
        followersRef = db.getReference("Followers")

        binding.btnBack.setOnClickListener { finish() }

        loadUserProfile()
        loadFollowCounts()
        checkIfFollowing()

        binding.btnFollow.setOnClickListener { toggleFollow() }
    }

    // -------- profile info --------

    private fun loadUserProfile() {
        usersRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val name = snapshot.child("name").getValue(String::class.java)
                val bio = snapshot.child("bio").getValue(String::class.java)
                val profileImage = snapshot.child("profileImage").getValue(String::class.java)
                val accountType = snapshot.child("accountType").getValue(String::class.java)

                binding.tvHeaderTitle.text = name ?: "Profile"
                binding.tvProfileName.text = name ?: "BuzzBoard User"
                binding.tvBio.text = bio ?: "VSU Student 🐝"

                if (!profileImage.isNullOrEmpty()) {
                    try {
                        val bytes = Base64.decode(profileImage, Base64.DEFAULT)
                        val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        binding.imgProfilePicture.setImageBitmap(bmp)
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

    // -------- follow counts --------

    private fun loadFollowCounts() {
        followersRef.child(userId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val count = snapshot.childrenCount
                binding.tvFollowersCount.text = "$count Followers"
            }

            override fun onCancelled(error: DatabaseError) {}
        })

        followingRef.child(userId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val count = snapshot.childrenCount
                binding.tvFollowingCount.text = "$count Following"
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    // -------- follow / unfollow --------

    private fun checkIfFollowing() {
        val me = currentUid ?: return

        followingRef.child(me).child(userId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    isFollowing = snapshot.exists()
                    updateFollowButton()
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun toggleFollow() {
        val me = currentUid ?: return

        if (isFollowing) {
            // UNFOLLOW
            val updates = hashMapOf<String, Any?>(
                "/Following/$me/$userId" to null,
                "/Followers/$userId/$me" to null
            )
            db.reference.updateChildren(updates).addOnCompleteListener {
                if (it.isSuccessful) {
                    isFollowing = false
                    updateFollowButton()
                }
            }
        } else {
            // FOLLOW
            val updates = hashMapOf<String, Any?>(
                "/Following/$me/$userId" to true,
                "/Followers/$userId/$me" to true
            )
            db.reference.updateChildren(updates).addOnCompleteListener {
                if (it.isSuccessful) {
                    isFollowing = true
                    updateFollowButton()
                }
            }
        }
    }

    private fun updateFollowButton() {
        if (isFollowing) {
            binding.btnFollow.text = "Following"
            binding.btnFollow.alpha = 0.8f
        } else {
            binding.btnFollow.text = "Follow"
            binding.btnFollow.alpha = 1f
        }
    }
}
