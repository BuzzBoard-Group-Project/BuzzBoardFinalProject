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

        //  main realtime listener
        fetchPostsFromFirebase()

        //  search
        setupSearchBar()

        //  pull-to-refresh
        binding.swipeRefresh.setOnRefreshListener {
            refreshPostsFromFirebase()
        }

        return binding.root
    }

    /**
     * This one stays as your realtime listener — it listens forever.
     */
    private fun fetchPostsFromFirebase() {
        databaseRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val tempList = ArrayList<Post>()
                for (dataSnap in snapshot.children) {
                    val post = dataSnap.getValue(Post::class.java)
                    if (post != null) tempList.add(post)
                }

                // newest first
                tempList.reverse()

                postList = tempList
                adapter.updateList(postList)

                // stop spinner if it was from swipe
                binding.swipeRefresh.isRefreshing = false
            }

            override fun onCancelled(error: DatabaseError) {
                binding.swipeRefresh.isRefreshing = false
                println("❌ Firebase error: ${error.message}")
            }
        })
    }


    private fun refreshPostsFromFirebase() {
        databaseRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val tempList = ArrayList<Post>()
                for (dataSnap in snapshot.children) {
                    val post = dataSnap.getValue(Post::class.java)
                    if (post != null) tempList.add(post)
                }

                tempList.reverse()
                postList = tempList
                adapter.updateList(postList)
                binding.swipeRefresh.isRefreshing = false
            }

            override fun onCancelled(error: DatabaseError) {
                binding.swipeRefresh.isRefreshing = false
            }
        })
    }

    //  search bar logic
    private fun setupSearchBar() {
        binding.searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().trim()

                if (query.isEmpty()) {
                    adapter.updateList(postList)
                } else {
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
