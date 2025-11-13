package com.example.buzzboardfinalproject

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.roundToInt

class PollAdapter(
    private val onVote: (poll: Poll, selectedIndex: Int) -> Unit
) : ListAdapter<Poll, PollAdapter.PollViewHolder>(DIFF) {

    // pollId -> selectedOptionIndex
    var userVotes: Map<String, Int> = emptyMap()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Poll>() {
            override fun areItemsTheSame(oldItem: Poll, newItem: Poll) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: Poll, newItem: Poll) = oldItem == newItem
        }
    }

    inner class PollViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val questionText: TextView = itemView.findViewById(R.id.questionText)
        val statusText: TextView = itemView.findViewById(R.id.statusText)
        val optionsContainer: RadioGroup = itemView.findViewById(R.id.optionsContainer)

        fun highlightOption(index: Int) {
            for (i in 0 until optionsContainer.childCount) {
                val v = optionsContainer.getChildAt(i)
                v.isEnabled = false
                v.alpha = if (i == index) 1f else 0.5f
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PollViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_poll, parent, false)
        return PollViewHolder(view)
    }

    override fun onBindViewHolder(holder: PollViewHolder, position: Int) {
        val poll = getItem(position)
        holder.questionText.text = poll.question

        // compute percentages
        val totals = poll.totals
        val totalVotes = totals.sum()
        val denom = if (totalVotes == 0) 1 else totalVotes
        val percents = totals.map { ((it * 100f) / denom).roundToInt() }

        // reset group
        holder.optionsContainer.setOnCheckedChangeListener(null)
        holder.optionsContainer.removeAllViews()

        val alreadyVotedIndex = userVotes[poll.id]
        val votingEnabled = (alreadyVotedIndex == null)

        // add options with percentages in labels
        poll.options.forEachIndexed { idx, option ->
            val rb = RadioButton(holder.itemView.context).apply {
                id = View.generateViewId()
                text = "$option  ${percents.getOrElse(idx) { 0 }}%"
                isEnabled = votingEnabled
            }
            holder.optionsContainer.addView(rb)
        }

        // hide raw vote count
        holder.statusText.text = ""

        // reflect previous vote if exists
        if (alreadyVotedIndex != null && alreadyVotedIndex in 0 until holder.optionsContainer.childCount) {
            val rb = holder.optionsContainer.getChildAt(alreadyVotedIndex) as RadioButton
            rb.isChecked = true
            holder.highlightOption(alreadyVotedIndex)
        }

        // vote handler
        holder.optionsContainer.setOnCheckedChangeListener { group, checkedId ->
            if (!votingEnabled) return@setOnCheckedChangeListener
            val idx = group.indexOfChild(group.findViewById(checkedId))
            if (idx != -1) onVote(poll, idx)
        }
    }
}
