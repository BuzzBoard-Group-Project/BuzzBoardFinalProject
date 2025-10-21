package com.example.buzzboardfinalproject

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.example.buzzboardfinalproject.databinding.FragmentFavoritesBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class FavoritesFragment : Fragment() {

    private var _binding: FragmentFavoritesBinding? = null
    private val binding get() = _binding!!

    private lateinit var databaseRef: DatabaseReference
    private lateinit var favoritesRef: DatabaseReference
    private lateinit var favoritePosts: ArrayList<Post>
    private lateinit var adapter: PostAdapter2

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFavoritesBinding.inflate(inflater, container, false)

        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return binding.root

        databaseRef = FirebaseDatabase.getInstance().getReference("Posts")
        favoritesRef = FirebaseDatabase.getInstance().getReference("Favorites").child(currentUserId)
        favoritePosts = ArrayList()
        adapter = PostAdapter2(requireContext(), favoritePosts)

        binding.recyclerFavorites.layoutManager = GridLayoutManager(requireContext(), 3)
        binding.recyclerFavorites.adapter = adapter

        loadFavoritePosts(currentUserId)

        return binding.root
    }

    private fun loadFavoritePosts(userId: String) {
        favoritesRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val favIds = ArrayList<String>()
                for (child in snapshot.children) {
                    favIds.add(child.key.toString())
                }

                if (favIds.isEmpty()) {
                    showEmptyState(true)
                    return
                }

                // Fetch the actual post data
                databaseRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(postSnap: DataSnapshot) {
                        favoritePosts.clear()
                        for (data in postSnap.children) {
                            val post = data.getValue(Post::class.java)
                            if (post != null && favIds.contains(post.postid)) {
                                favoritePosts.add(post)
                            }
                        }

                        if (favoritePosts.isEmpty()) {
                            showEmptyState(true)
                        } else {
                            showEmptyState(false)
                        }

                        favoritePosts.reverse() // show newest first
                        adapter.updateList(favoritePosts)
                    }

                    override fun onCancelled(error: DatabaseError) {}
                })
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun showEmptyState(show: Boolean) {
        binding.tvNoFavorites.visibility = if (show) View.VISIBLE else View.GONE
        binding.recyclerFavorites.visibility = if (show) View.GONE else View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
