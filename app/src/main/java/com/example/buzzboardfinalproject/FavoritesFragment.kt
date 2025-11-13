package com.example.buzzboardfinalproject

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.buzzboardfinalproject.databinding.FragmentFavoritesBinding
import java.util.Calendar

class FavoritesFragment : Fragment() {

    private var _binding: FragmentFavoritesBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFavoritesBinding.inflate(inflater, container, false)

        // Default calendar date = today
        val cal = Calendar.getInstance()
        binding.calendarView.date = cal.timeInMillis

        // When user taps a date → open list screen
        binding.calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val c = Calendar.getInstance()
            c.set(Calendar.YEAR, year)
            c.set(Calendar.MONTH, month)          // month is 0-based already
            c.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            c.set(Calendar.HOUR_OF_DAY, 0)
            c.set(Calendar.MINUTE, 0)
            c.set(Calendar.SECOND, 0)
            c.set(Calendar.MILLISECOND, 0)

            val selectedDateMillis = c.timeInMillis

            val intent = Intent(requireContext(), FavoritesDayActivity::class.java).apply {
                putExtra("selectedDateMillis", selectedDateMillis)
            }
            startActivity(intent)
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
