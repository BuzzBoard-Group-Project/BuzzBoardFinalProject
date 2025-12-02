package com.example.buzzboardfinalproject

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class ProfilePostsTabFragment : Fragment() {

    private lateinit var postsRef: DatabaseReference
    private val posts = ArrayList<Post>()
    private lateinit var adapter: ProfilePostGridAdapter   // 👈 new adapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.tab_profile_posts, container, false)
        val rv = view.findViewById<RecyclerView>(R.id.rvPosts)

        // 3-column square grid
        rv.layoutManager = GridLayoutManager(requireContext(), 3)

        adapter = ProfilePostGridAdapter { post ->
            // open normal detail screen when a tile is tapped
            val i = Intent(requireContext(), PostDetailActivity::class.java)
            i.putExtra("post_id", post.postid)
            startActivity(i)
        }
        rv.adapter = adapter

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return view
        postsRef = FirebaseDatabase.getInstance().getReference("Posts")

        loadMyPosts(uid)

        return view
    }

    private fun loadMyPosts(uid: String) {
        postsRef.orderByChild("publisher").equalTo(uid)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val temp = ArrayList<Post>()
                    for (snap in snapshot.children) {
                        val post = snap.getValue(Post::class.java)
                        if (post != null) temp.add(post)
                    }

                    // 🔽 Newest first – adjust field if your time field is different
                    temp.sortByDescending { it.eventDateMillis }

                    posts.clear()
                    posts.addAll(temp)
                    adapter.submitList(posts.toList())
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }
}
