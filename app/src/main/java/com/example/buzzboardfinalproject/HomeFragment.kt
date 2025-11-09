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

    private lateinit var postsRef: DatabaseReference
    private lateinit var pollsRef: DatabaseReference

    private var postsListener: ValueEventListener? = null
    private var pollsListener: ValueEventListener? = null
    private var votesListener: ValueEventListener? = null

    private var postList: ArrayList<Post> = arrayListOf()

    private lateinit var postAdapter: PostAdapter2
    private lateinit var pollAdapter: PollAdapter
    private lateinit var concatAdapter: ConcatAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)

        // DB refs
        postsRef = FirebaseDatabase.getInstance().getReference("Posts")
        pollsRef = FirebaseDatabase.getInstance().getReference("Polls")

        // Adapters
        postAdapter = PostAdapter2(requireContext(), arrayListOf())
        pollAdapter = PollAdapter { poll, index ->
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            if (uid.isNullOrBlank()) {
                Toast.makeText(requireContext(), "Not signed in", Toast.LENGTH_SHORT).show()
                return@PollAdapter
            }
            PollRepositoryRtDb.vote(poll, index, uid) { ok, err ->
                if (!ok) Toast.makeText(requireContext(), err ?: "Vote failed", Toast.LENGTH_SHORT).show()
            }
        }

        // Concat. Use default isolation to avoid view-type collisions.
        concatAdapter = ConcatAdapter(pollAdapter, postAdapter)

        // One RecyclerView in fragment_home.xml with id homeRecycler
        binding.homeRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.homeRecycler.adapter = concatAdapter
        binding.homeRecycler.setHasFixedSize(false)

        setupSearchBar()
        ensureAuthAndStart()

        return binding.root
    }

    private fun ensureAuthAndStart() {
        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser
        if (user == null) {
            auth.signInAnonymously().addOnCompleteListener { t ->
                if (t.isSuccessful) {
                    startPostsListener()
                    startPollsListener()
                    startUserVotesListener(auth.currentUser?.uid)
                } else {
                    Toast.makeText(requireContext(), "Auth failed", Toast.LENGTH_LONG).show()
                }
            }
        } else {
            startPostsListener()
            startPollsListener()
            startUserVotesListener(user.uid)
        }
    }

    private fun startPostsListener() {
        postsListener = postsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val temp = ArrayList<Post>()
                for (c in snapshot.children) {
                    c.getValue(Post::class.java)?.let { temp.add(it) }
                }
                temp.reverse()
                postList = temp
                postAdapter.updateList(postList)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Posts read failed: ${error.code}", Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun startPollsListener() {
        pollsListener = pollsRef
            .orderByChild("createdAt")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val polls = snapshot.children.mapNotNull { it.getValue(Poll::class.java) }
                    // newest last from RTDB when ordered by createdAt. reverse to show newest first.
                    pollAdapter.submitList(polls.reversed())
                }
                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(requireContext(), "Polls read failed: ${error.code}", Toast.LENGTH_LONG).show()
                }
            })
    }


    private fun startUserVotesListener(uid: String?) {
        if (uid.isNullOrBlank()) return
        val ref = FirebaseDatabase.getInstance().getReference("UserPollVotes").child(uid)
        votesListener = ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val map = mutableMapOf<String, Int>()
                for (c in snapshot.children) {
                    val pollId = c.key ?: continue
                    val idx = c.getValue(Int::class.java) ?: continue
                    map[pollId] = idx
                }
                pollAdapter.userVotes = map
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun setupSearchBar() {
        binding.searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val q = s?.toString()?.trim().orEmpty()
                if (q.isEmpty()) {
                    postAdapter.updateList(postList)
                } else {
                    val filtered = postList.filter {
                        it.title.contains(q, true) ||
                                it.description.contains(q, true) ||
                                it.location.contains(q, true)
                    }
                    postAdapter.updateList(ArrayList(filtered))
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    override fun onStop() {
        postsListener?.let { postsRef.removeEventListener(it) }
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
        postsListener = null
        pollsListener = null
        votesListener = null
        super.onStop()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
