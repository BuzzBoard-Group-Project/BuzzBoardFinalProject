package com.example.buzzboardfinalproject

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.buzzboardfinalproject.databinding.ActivityFollowListBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

data class SimpleUser(
    val uid: String = "",
    val name: String = "",
    val profileImage: String? = null,
    val accountType: String? = null
)

class FollowListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFollowListBinding
    private val users = ArrayList<SimpleUser>()
    private lateinit var adapter: FollowUserAdapter

    private val db by lazy { FirebaseDatabase.getInstance().reference }
    private val currentUid get() = FirebaseAuth.getInstance().currentUser?.uid

    private var mode: String = "followers" // or "following"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFollowListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mode = intent.getStringExtra("mode") ?: "followers"

        binding.tvTitle.text =
            if (mode == "following") "Following" else "Followers"

        binding.btnBack.setOnClickListener { finish() }

        adapter = FollowUserAdapter(users) { user ->
            // TODO: open user profile screen when you have it
            Toast.makeText(this, user.name, Toast.LENGTH_SHORT).show()
        }

        binding.rvUsers.layoutManager = LinearLayoutManager(this)
        binding.rvUsers.adapter = adapter

        loadUsers()
    }

    private fun loadUsers() {
        val uid = currentUid ?: return

        val idsRef = if (mode == "following") {
            db.child("Following").child(uid)
        } else {
            db.child("Followers").child(uid)
        }

        idsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val ids = snapshot.children.mapNotNull { it.key }
                users.clear()

                if (ids.isEmpty()) {
                    adapter.notifyDataSetChanged()
                    binding.tvEmpty.text =
                        if (mode == "following") "You're not following anyone yet." else "No followers yet."
                    binding.tvEmpty.visibility = View.VISIBLE
                    return
                }

                binding.tvEmpty.visibility = View.GONE

                // Fetch each user profile
                for (id in ids) {
                    db.child("Users").child(id)
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(userSnap: DataSnapshot) {
                                val name =
                                    userSnap.child("name").getValue(String::class.java) ?: "BuzzBoard User"
                                val img =
                                    userSnap.child("profileImage").getValue(String::class.java)
                                val type =
                                    userSnap.child("accountType").getValue(String::class.java)

                                users.add(
                                    SimpleUser(
                                        uid = id,
                                        name = name,
                                        profileImage = img,
                                        accountType = type
                                    )
                                )
                                // Keep a stable order
                                users.sortBy { it.name.lowercase() }
                                adapter.notifyDataSetChanged()
                            }

                            override fun onCancelled(error: DatabaseError) {}
                        })
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }
}
