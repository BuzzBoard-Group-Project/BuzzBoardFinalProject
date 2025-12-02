package com.example.buzzboardfinalproject

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class ProfileTabAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> ProfilePostsTabFragment()       // Your posts
            1 -> ProfileFavoritesTabFragment()   // Favorites
            else -> ProfileRegisteredTabFragment() // Registered events
        }
    }
}
