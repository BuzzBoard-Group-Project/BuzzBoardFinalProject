package com.example.buzzboardfinalproject

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView


class HomeFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var postAdapter: PostAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        recyclerView = view.findViewById(R.id.postRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Dummy posts
        val posts = listOf(
            Post("alex123", "Dorm Room Decor", "Check out my new dorm setup!", "Moore Hall", R.drawable.sample1),
            Post("sarah_m", "Free Pizza", "Giving away free pizza in Hunter Mc-Daniel!", "HM 24E", R.drawable.sample2),
            Post("vsuknitters", "Knitting Club Meeting", "Join our knitting club meeting today!", "Library Room 203", R.drawable.sample3)
        )

        postAdapter = PostAdapter(posts)
        recyclerView.adapter = postAdapter

        return view
    }
}
