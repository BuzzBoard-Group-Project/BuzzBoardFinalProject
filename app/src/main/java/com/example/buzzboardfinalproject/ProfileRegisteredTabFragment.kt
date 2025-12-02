package com.example.buzzboardfinalproject

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class ProfileRegisteredTabFragment : Fragment() {

    private lateinit var registrationsRef: DatabaseReference
    private lateinit var postsRef: DatabaseReference

    private val registered = ArrayList<Post>()
    private lateinit var adapter: ProfileEventRowAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.tab_profile_posts, container, false)
        val rv = view.findViewById<RecyclerView>(R.id.rvPosts)

        rv.layoutManager = LinearLayoutManager(requireContext())
        adapter = ProfileEventRowAdapter { post ->
            val i = Intent(requireContext(), PostDetailActivity::class.java)
            i.putExtra("post_id", post.postid)
            startActivity(i)
        }
        rv.adapter = adapter

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return view
        val db = FirebaseDatabase.getInstance()
        registrationsRef = db.getReference("EventRegistrations")
        postsRef = db.getReference("Posts")

        loadRegistered(uid)

        return view
    }

    private fun loadRegistered(uid: String) {
        registrationsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val registeredPostIds = mutableListOf<String>()

                for (postSnap in snapshot.children) {
                    val postId = postSnap.key ?: continue
                    if (postSnap.hasChild(uid)) {
                        registeredPostIds.add(postId)
                    }
                }

                if (registeredPostIds.isEmpty()) {
                    registered.clear()
                    adapter.submitList(emptyList())
                    return
                }

                val temp = ArrayList<Post>()
                var remaining = registeredPostIds.size

                for (id in registeredPostIds) {
                    postsRef.child(id).addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(psnap: DataSnapshot) {
                            psnap.getValue(Post::class.java)?.let { temp.add(it) }
                            remaining--
                            if (remaining == 0) {
                                temp.sortByDescending { it.eventDateMillis }
                                registered.clear()
                                registered.addAll(temp)
                                adapter.submitList(registered.toList())
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            remaining--
                        }
                    })
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }
}
