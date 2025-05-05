package com.example.buzzboardfinalproject

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.buzzboardfinalproject.databinding.FragmentReminderBinding
import java.text.SimpleDateFormat
import java.util.*

class ReminderFragment : Fragment() {

    private var _binding: FragmentReminderBinding? = null
    private val binding get() = _binding!!
    private val calendar = Calendar.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReminderBinding.inflate(inflater, container, false)

        setupToolbar()
        setupDateTimePicker()
        setupButtons()

        return binding.root
    }

    private fun setupToolbar() {
        val toolbar = binding.reminderToolbar
        (activity as? AppCompatActivity)?.setSupportActionBar(toolbar)
        toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material)
        toolbar.setNavigationOnClickListener {
            activity?.onBackPressed()
        }
    }

    private fun setupDateTimePicker() {
        binding.reminderDateTime.setOnClickListener {
            val context = requireContext()

            DatePickerDialog(context, { _, year, month, dayOfMonth ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)

                TimePickerDialog(context, { _, hourOfDay, minute ->
                    calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                    calendar.set(Calendar.MINUTE, minute)

                    val format = SimpleDateFormat("EEE, MMM d yyyy - h:mm a", Locale.getDefault())
                    binding.reminderDateTime.setText(format.format(calendar.time))

                }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false).show()

            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }
    }

    private fun setupButtons() {
        binding.cancelReminderButton.setOnClickListener {
            Toast.makeText(context, "Reminder canceled", Toast.LENGTH_SHORT).show()
            activity?.onBackPressed()
        }

        binding.createReminderButton.setOnClickListener {
            val title = binding.reminderTitle.text.toString()
            val description = binding.reminderDescription.text.toString()
            val dateTime = binding.reminderDateTime.text.toString()

            if (title.isBlank() || dateTime.isBlank()) {
                Toast.makeText(context, "Title and Date/Time are required", Toast.LENGTH_SHORT).show()
            } else {
                // You could save the reminder to a database here
                Toast.makeText(context, "Reminder set for $dateTime", Toast.LENGTH_LONG).show()
                activity?.onBackPressed()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
