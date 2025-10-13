package com.example.buzzboardfinalproject

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.buzzboardfinalproject.databinding.FragmentHomeBinding
import com.google.firebase.database.*

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var databaseRef: DatabaseReference
    private lateinit var postList: ArrayList<Post>
    private lateinit var adapter: PostAdapter2

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)

        // ✅ Set up Firebase and RecyclerView
        databaseRef = FirebaseDatabase.getInstance().getReference("Posts")
        postList = ArrayList()
        adapter = PostAdapter2(requireContext(), postList)
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        fetchPostsFromFirebase()

        return binding.root
    }

    private fun fetchPostsFromFirebase() {
        databaseRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                postList.clear()

                // 🧪 ✅ DEBUG PRINT — This is the part you add!
                println("🔥 Snapshot count: ${snapshot.childrenCount}")
                for (dataSnap in snapshot.children) {
                    println("👉 Post key: ${dataSnap.key}")
                    println("👉 Post data: ${dataSnap.value}")
                }

                for (dataSnap in snapshot.children) {
                    val post = dataSnap.getValue(Post::class.java)
                    if (post != null) {
                        postList.add(post)
                    }
                }

// ✅ Reverse the list so newest posts come first
                postList.reverse()

                adapter.notifyDataSetChanged()

            }

            override fun onCancelled(error: DatabaseError) {
                println("❌ Firebase error: ${error.message}")
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
