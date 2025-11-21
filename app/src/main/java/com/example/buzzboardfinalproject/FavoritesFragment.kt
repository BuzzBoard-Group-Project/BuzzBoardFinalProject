package com.example.buzzboardfinalproject

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.buzzboardfinalproject.databinding.FragmentFavoritesBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.util.Calendar

class FavoritesFragment : Fragment() {

    private var _binding: FragmentFavoritesBinding? = null
    private val binding get() = _binding!!

    private lateinit var favRef: DatabaseReference
    private lateinit var postsRef: DatabaseReference

    private val allFavoritePosts = ArrayList<Post>()
    private lateinit var adapter: FavoriteEventAdapter   // 👈 changed

    private var selectedDayMillis: Long = 0L

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFavoritesBinding.inflate(inflater, container, false)

        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            binding.tvEmpty.visibility = View.VISIBLE
            binding.tvEmpty.text = "Please log in to see favorites."
            return binding.root
        }

        favRef = FirebaseDatabase.getInstance()
            .getReference("Favorites")
            .child(uid)

        postsRef = FirebaseDatabase.getInstance()
            .getReference("Posts")

        // Recycler setup
        adapter = FavoriteEventAdapter(requireContext(), ArrayList())
        binding.rvFavoritesEvents.layoutManager = LinearLayoutManager(requireContext())
        binding.rvFavoritesEvents.adapter = adapter

        val today = Calendar.getInstance()
        selectedDayMillis = normalizeToDay(today.timeInMillis)
        binding.calendarView.date = today.timeInMillis

        binding.calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val cal = Calendar.getInstance()
            cal.set(Calendar.YEAR, year)
            cal.set(Calendar.MONTH, month)
            cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            selectedDayMillis = normalizeToDay(cal.timeInMillis)
            filterForSelectedDate()
        }

        loadFavorites()

        return binding.root
    }

    private fun loadFavorites() {
        allFavoritePosts.clear()
        binding.tvEmpty.visibility = View.GONE
        adapter.updateList(ArrayList())

        favRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                allFavoritePosts.clear()

                if (!snapshot.hasChildren()) {
                    filterForSelectedDate()
                    return
                }

                var remaining = snapshot.childrenCount.toInt()

                for (child in snapshot.children) {
                    val postId = child.key
                    if (postId == null) {
                        remaining--
                        if (remaining == 0) filterForSelectedDate()
                        continue
                    }

                    postsRef.child(postId)
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(ps: DataSnapshot) {
                                ps.getValue(Post::class.java)?.let { allFavoritePosts.add(it) }
                                remaining--
                                if (remaining == 0) filterForSelectedDate()
                            }

                            override fun onCancelled(error: DatabaseError) {
                                remaining--
                                if (remaining == 0) filterForSelectedDate()
                            }
                        })
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun filterForSelectedDate() {
        if (selectedDayMillis == 0L) {
            adapter.updateList(ArrayList())
            binding.tvEmpty.visibility = View.VISIBLE
            binding.tvEmpty.text = "Tap a date to see your favorite events."
            return
        }

        val filtered = allFavoritePosts.filter { post ->
            val eventMillis = post.eventDateMillis ?: 0L
            eventMillis != 0L && normalizeToDay(eventMillis) == selectedDayMillis
        }

        if (filtered.isEmpty()) {
            binding.tvEmpty.visibility = View.VISIBLE
            binding.tvEmpty.text = "No favorite events on this day."
            adapter.updateList(ArrayList())
        } else {
            binding.tvEmpty.visibility = View.GONE
            val sorted = filtered.sortedBy { it.eventDateMillis ?: 0L }
            adapter.updateList(ArrayList(sorted))
        }
    }

    private fun normalizeToDay(millis: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = millis
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
