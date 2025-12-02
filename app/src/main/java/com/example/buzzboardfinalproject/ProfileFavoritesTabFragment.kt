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

class ProfileFavoritesTabFragment : Fragment() {

    private lateinit var favoritesRef: DatabaseReference
    private lateinit var postsRef: DatabaseReference

    private val favorites = ArrayList<Post>()
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
        favoritesRef = db.getReference("Favorites").child(uid)
        postsRef = db.getReference("Posts")

        loadFavorites()

        return view
    }

    private fun loadFavorites() {
        favoritesRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val postIds = snapshot.children.mapNotNull { it.key }
                if (postIds.isEmpty()) {
                    favorites.clear()
                    adapter.submitList(emptyList())
                    return
                }

                // Fetch each post once
                val temp = ArrayList<Post>()
                var remaining = postIds.size

                for (id in postIds) {
                    postsRef.child(id).addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(psnap: DataSnapshot) {
                            psnap.getValue(Post::class.java)?.let { temp.add(it) }
                            remaining--
                            if (remaining == 0) {
                                // newest first by eventDateMillis (or fallback to timestamp)
                                temp.sortByDescending { it.eventDateMillis }
                                favorites.clear()
                                favorites.addAll(temp)
                                adapter.submitList(favorites.toList())
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
