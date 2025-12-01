package com.example.buzzboardfinalproject

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.buzzboardfinalproject.databinding.FragmentHomeBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var postsRef: DatabaseReference
    private lateinit var pollsRef: DatabaseReference

    private lateinit var feedAdapter: UnifiedFeedAdapter

    private var allPosts = ArrayList<Post>()
    private var allPolls = ArrayList<Poll>()

    private var postsLoaded = false
    private var pollsLoaded = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)

        postsRef = FirebaseDatabase.getInstance().getReference("Posts")
        pollsRef = FirebaseDatabase.getInstance().getReference("Polls")

        feedAdapter = UnifiedFeedAdapter(
            onVote = { poll, index -> voteOnPoll(poll, index) },
            onPostClicked = { post ->
                val i = android.content.Intent(context, PostDetailActivity::class.java)
                i.putExtra("post_id", post.postid)
                startActivity(i)
            }
        )

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = feedAdapter

        loadPosts()
        loadPolls()
        loadUserVotes()
        setupSearch()

        return binding.root
    }

    // ----------------------------------------------------
    // LOAD POSTS — NO MORE FAKE createdAt VALUES
    // ----------------------------------------------------
    private fun loadPosts() {
        postsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val temp = ArrayList<Post>()

                for (snap in snapshot.children) {
                    val p = snap.getValue(Post::class.java)
                    if (p != null) {
                        // do NOT set createdAt artificially anymore
                        temp.add(p)
                    }
                }

                allPosts = temp
                postsLoaded = true
                if (pollsLoaded) updateFeed()
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    // ----------------------------------------------------
    // LOAD POLLS
    // ----------------------------------------------------
    private fun loadPolls() {
        pollsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val temp = ArrayList<Poll>()
                for (snap in snapshot.children) {
                    val p = snap.getValue(Poll::class.java)
                    if (p != null) temp.add(p)
                }

                allPolls = temp
                pollsLoaded = true
                if (postsLoaded) updateFeed()
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    // ----------------------------------------------------
    // LOAD USER POLL VOTES
    // ----------------------------------------------------
    private fun loadUserVotes() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val ref = FirebaseDatabase.getInstance()
            .getReference("UserPollVotes")
            .child(uid)

        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val map = mutableMapOf<String, Int>()
                for (c in snapshot.children) {
                    val pollId = c.key ?: continue
                    val idx = c.getValue(Int::class.java) ?: continue
                    map[pollId] = idx
                }
                feedAdapter.userVotes = map
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    // ----------------------------------------------------
    // MERGE + SORT
    //   • createdAt > 0 = correct sorted order (newest first)
    //   • createdAt == 0 = ALWAYS pushed to bottom
    // ----------------------------------------------------
    private fun updateFeed() {
        val combined = ArrayList<UnifiedFeedItem>()

        allPosts.forEach { combined.add(UnifiedFeedItem.PostItem(it)) }
        allPolls.forEach { combined.add(UnifiedFeedItem.PollItem(it)) }

        val sorted = combined.sortedWith { a, b ->
            val tA = when (a) {
                is UnifiedFeedItem.PostItem -> a.post.createdAt
                is UnifiedFeedItem.PollItem -> a.poll.createdAt
            }
            val tB = when (b) {
                is UnifiedFeedItem.PostItem -> b.post.createdAt
                is UnifiedFeedItem.PollItem -> b.poll.createdAt
            }

            when {
                tA == 0L && tB == 0L -> 0      // both old → keep relative order
                tA == 0L -> 1                  // A has no timestamp → A goes LOWER
                tB == 0L -> -1                 // B has no timestamp → B goes LOWER
                else -> tB.compareTo(tA)       // both valid → newest first
            }
        }

        feedAdapter.submitList(sorted)
    }

    // ----------------------------------------------------
    // VOTE ON POLL SAFELY (NO DOUBLE VOTES)
    // ----------------------------------------------------
    private fun voteOnPoll(poll: Poll, index: Int) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val userVoteRef = FirebaseDatabase.getInstance()
            .getReference("UserPollVotes")
            .child(uid)
            .child(poll.id)

        userVoteRef.runTransaction(object : Transaction.Handler {
            override fun doTransaction(m: MutableData): Transaction.Result {
                if (m.getValue(Int::class.java) != null) return Transaction.abort()
                m.value = index
                return Transaction.success(m)
            }

            override fun onComplete(e: DatabaseError?, committed: Boolean, snap: DataSnapshot?) {
                if (committed) {
                    pollsRef.child(poll.id)
                        .child("totals")
                        .child(index.toString())
                        .runTransaction(object : Transaction.Handler {
                            override fun doTransaction(m: MutableData): Transaction.Result {
                                val current = m.getValue(Int::class.java) ?: 0
                                m.value = current + 1
                                return Transaction.success(m)
                            }

                            override fun onComplete(
                                e: DatabaseError?,
                                committed: Boolean,
                                snap: DataSnapshot?
                            ) {}
                        })
                }
            }
        })
    }

    // ----------------------------------------------------
    // SEARCH
    // ----------------------------------------------------
    private fun setupSearch() {
        binding.searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val q = s.toString().trim()
                if (q.isEmpty()) {
                    if (postsLoaded && pollsLoaded) updateFeed()
                    return
                }

                val combined = ArrayList<UnifiedFeedItem>()

                allPosts.filter {
                    it.title.contains(q, true) ||
                            it.description.contains(q, true) ||
                            it.location.contains(q, true)
                }.forEach {
                    combined.add(UnifiedFeedItem.PostItem(it))
                }

                allPolls.filter {
                    it.question.contains(q, true)
                }.forEach {
                    combined.add(UnifiedFeedItem.PollItem(it))
                }

                val sorted = combined.sortedWith { a, b ->
                    val tA = when (a) {
                        is UnifiedFeedItem.PostItem -> a.post.createdAt
                        is UnifiedFeedItem.PollItem -> a.poll.createdAt
                    }
                    val tB = when (b) {
                        is UnifiedFeedItem.PostItem -> b.post.createdAt
                        is UnifiedFeedItem.PollItem -> b.poll.createdAt
                    }

                    when {
                        tA == 0L && tB == 0L -> 0
                        tA == 0L -> 1
                        tB == 0L -> -1
                        else -> tB.compareTo(tA)
                    }
                }

                feedAdapter.submitList(sorted)
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
