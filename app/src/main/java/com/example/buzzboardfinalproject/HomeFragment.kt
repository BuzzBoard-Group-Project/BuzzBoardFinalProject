package com.example.buzzboardfinalproject

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.buzzboardfinalproject.databinding.FragmentHomeBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var databaseRef: DatabaseReference
    private lateinit var postList: ArrayList<Post>
    private lateinit var adapter: PostAdapter2

    // added: polls
    private lateinit var pollsRef: DatabaseReference
    private lateinit var pollAdapter: PollAdapter
    private var pollsListener: ValueEventListener? = null
    private var votesListener: ValueEventListener? = null
    private lateinit var concatAdapter: ConcatAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)

        // Posts (existing)
        databaseRef = FirebaseDatabase.getInstance().getReference("Posts")
        postList = ArrayList()
        adapter = PostAdapter2(requireContext(), postList)

        // Polls (added)
        pollsRef = FirebaseDatabase.getInstance().getReference("Polls")
        pollAdapter = PollAdapter { poll, selectedIndex ->
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@PollAdapter
            val db = FirebaseDatabase.getInstance().reference

            // prevent double vote: write user vote only if not set
            val userVoteRef = db.child("UserPollVotes").child(uid).child(poll.id)
            userVoteRef.runTransaction(object : Transaction.Handler {
                override fun doTransaction(currentData: MutableData): Transaction.Result {
                    if (currentData.getValue(Int::class.java) != null) {
                        return Transaction.abort()
                    }
                    currentData.value = selectedIndex
                    return Transaction.success(currentData)
                }
                override fun onComplete(
                    error: DatabaseError?,
                    committed: Boolean,
                    currentData: DataSnapshot?
                ) {
                    if (committed) {
                        val current = poll.totals.getOrNull(selectedIndex) ?: 0
                        db.child("Polls")
                            .child(poll.id)
                            .child("totals")
                            .child(selectedIndex.toString())
                            .setValue(current + 1)
                    }
                }
            })
        }

        // Combine: polls first, then posts
        concatAdapter = ConcatAdapter(
            ConcatAdapter.Config.Builder()
                .setIsolateViewTypes(true)
                .build(),
            pollAdapter,
            adapter
        )

        // Recycler
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = concatAdapter

        // Load data
        fetchPostsFromFirebase()
        startPollsListener()
        startUserVotesListener(FirebaseAuth.getInstance().currentUser?.uid)

        // Search
        setupSearchBar()

        return binding.root
    }

    private fun fetchPostsFromFirebase() {
        databaseRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val tempList = ArrayList<Post>()
                for (dataSnap in snapshot.children) {
                    val post = dataSnap.getValue(Post::class.java)
                    if (post != null) tempList.add(post)
                }
                tempList.reverse()
                postList = tempList
                adapter.updateList(postList)
            }
            override fun onCancelled(error: DatabaseError) {
                println("❌ Firebase error: ${error.message}")
            }
        })
    }

    private fun startPollsListener() {
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid
        pollsListener = pollsRef
            .orderByChild("createdAt")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val now = System.currentTimeMillis()

                    val allPolls = snapshot.children.mapNotNull { it.getValue(Poll::class.java) }

                    // Show poll if:
                    //   - not expired, or
                    //   - user is the creator
                    val visiblePolls = allPolls.filter { poll ->
                        val end = poll.endTime ?: Long.MAX_VALUE
                        val creator = poll.createdBy
                        end > now || (creator != null && creator == currentUid)
                    }

                    // newest first
                    val sorted = visiblePolls.sortedByDescending { it.createdAt }

                    pollAdapter.submitList(sorted)
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(
                        requireContext(),
                        "Polls read failed: ${error.code}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
    }

    // added: reflect user’s vote in the UI
    private fun startUserVotesListener(uid: String?) {
        if (uid.isNullOrBlank()) return
        val ref = FirebaseDatabase.getInstance()
            .getReference("UserPollVotes")
            .child(uid)
        votesListener = ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val map = mutableMapOf<String, Int>()
                for (c in snapshot.children) {
                    val pollId = c.key ?: continue
                    val idx = c.getValue(Int::class.java) ?: continue
                    map[pollId] = idx
                }
                // requires PollAdapter.userVotes
                pollAdapter.userVotes = map
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

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
        // remove added listeners
        pollsListener?.let { pollsRef.removeEventListener(it) }
        votesListener?.let {
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            if (!uid.isNullOrBlank()) {
                FirebaseDatabase.getInstance()
                    .getReference("UserPollVotes")
                    .child(uid)
                    .removeEventListener(it)
            }
        }
        pollsListener = null
        votesListener = null
        _binding = null
    }
}
