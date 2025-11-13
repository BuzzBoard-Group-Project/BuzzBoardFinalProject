package com.example.buzzboardfinalproject

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.buzzboardfinalproject.databinding.ActivityFavoritesDayBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FavoritesDayActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFavoritesDayBinding

    private lateinit var favRef: DatabaseReference
    private lateinit var postsRef: DatabaseReference

    private lateinit var adapter: PostAdapter2
    private val dayPosts = ArrayList<Post>()

    private var selectedDateMillis: Long = 0L
    private lateinit var selectedKey: String   // yyyy-MM-dd

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFavoritesDayBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Back
        binding.btnBack.setOnClickListener { finish() }

        // Get date from intent
        selectedDateMillis = intent.getLongExtra("selectedDateMillis", 0L)
        if (selectedDateMillis == 0L) {
            Toast.makeText(this, "No date selected", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Show human-readable date
        val displayFormat = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
        binding.tvSelectedDate.text = displayFormat.format(Date(selectedDateMillis))

        // Key for comparing days
        selectedKey = keyFromMillis(selectedDateMillis)

        // Firebase refs
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            Toast.makeText(this, "Not signed in", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        favRef = FirebaseDatabase.getInstance().getReference("Favorites").child(uid)
        postsRef = FirebaseDatabase.getInstance().getReference("Posts")

        // Recycler
        adapter = PostAdapter2(this, ArrayList())
        binding.rvFavoritesDay.layoutManager = LinearLayoutManager(this)
        binding.rvFavoritesDay.adapter = adapter

        loadFavoritesForDay()
    }

    private fun loadFavoritesForDay() {
        binding.tvEmptyDay.text = "Loading favorite events..."

        favRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                dayPosts.clear()

                if (!snapshot.hasChildren()) {
                    showEmpty("No favorite events on this date.")
                    return
                }

                var remaining = snapshot.childrenCount.toInt()

                for (child in snapshot.children) {
                    val postId = child.key
                    if (postId == null) {
                        remaining--
                        if (remaining == 0) finishLoading()
                        continue
                    }

                    postsRef.child(postId)
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(ps: DataSnapshot) {
                                val post = ps.getValue(Post::class.java)
                                if (post != null && isSameDay(post)) {
                                    dayPosts.add(post)
                                }
                                remaining--
                                if (remaining == 0) finishLoading()
                            }

                            override fun onCancelled(error: DatabaseError) {
                                remaining--
                                if (remaining == 0) finishLoading()
                            }
                        })
                }
            }

            override fun onCancelled(error: DatabaseError) {
                showEmpty("Error loading favorites.")
            }
        })
    }

    /** Compare stored eventDateMillis to selected day */
    private fun isSameDay(post: Post): Boolean {
        if (post.eventDateMillis == null || post.eventDateMillis == 0L) return false
        val postKey = keyFromMillis(post.eventDateMillis!!)
        return postKey == selectedKey
    }

    private fun keyFromMillis(millis: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date(millis))
    }

    private fun finishLoading() {
        if (dayPosts.isEmpty()) {
            showEmpty("No favorite events on this date.")
        } else {
            binding.tvEmptyDay.text = ""
            adapter.updateList(ArrayList(dayPosts.reversed())) // newest first
        }
    }

    private fun showEmpty(message: String) {
        adapter.updateList(ArrayList())
        binding.tvEmptyDay.text = message
    }
}
