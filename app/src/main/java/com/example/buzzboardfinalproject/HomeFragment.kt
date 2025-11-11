package com.example.buzzboardfinalproject

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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

        databaseRef = FirebaseDatabase.getInstance().getReference("Posts")
        postList = ArrayList()
        adapter = PostAdapter2(requireContext(), postList)

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        fetchPostsFromFirebase()
        setupSearchBar() //  NEW: initialize search functionality

        return binding.root
    }

    private fun fetchPostsFromFirebase() {
        databaseRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val tempList = ArrayList<Post>() // local temp list
                for (dataSnap in snapshot.children) {
                    val post = dataSnap.getValue(Post::class.java)
                    if (post != null) tempList.add(post)
                }

                // ✅ Newest first
                tempList.reverse()

                // ✅ Update adapter safely
                postList = tempList
                adapter.updateList(postList)
            }

            override fun onCancelled(error: DatabaseError) {
                println("❌ Firebase error: ${error.message}")
            }
        })
    }

    //  NEW FUNCTION — handles live search + clearing
    private fun setupSearchBar() {
        binding.searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().trim()

                if (query.isEmpty()) {
                    //  If search bar is cleared → show all posts again
                    adapter.updateList(postList)
                } else {
                    //  Filter by title, description, or location
                    val filtered = postList.filter {
                        it.title.contains(query, ignoreCase = true) ||
                                it.description.contains(query, ignoreCase = true) ||
                                it.location.contains(query, ignoreCase = true)
                    }
                    adapter.updateList(ArrayList(filtered))
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
