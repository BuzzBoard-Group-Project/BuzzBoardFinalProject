package com.example.buzzboardfinalproject

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import kotlin.math.roundToInt

class UnifiedFeedAdapter(
    private val onVote: (poll: Poll, selectedIndex: Int) -> Unit,
    private val onPostClicked: (post: Post) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val items = ArrayList<UnifiedFeedItem>()

    companion object {
        private const val TYPE_POST = 1
        private const val TYPE_POLL = 2
    }

    // Stores user poll votes so we can disable UI after voting
    var userVotes: Map<String, Int> = emptyMap()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    fun submitList(newList: List<UnifiedFeedItem>) {
        items.clear()
        items.addAll(newList)
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is UnifiedFeedItem.PostItem -> TYPE_POST
            is UnifiedFeedItem.PollItem -> TYPE_POLL
        }
    }

    override fun getItemCount(): Int = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_POST -> {
                val v = LayoutInflater.from(parent.context)
                    .inflate(R.layout.recycler_item, parent, false)
                PostVH(v)
            }
            else -> {
                val v = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_poll, parent, false)
                PollVH(v)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is UnifiedFeedItem.PostItem -> (holder as PostVH).bind(item.post)
            is UnifiedFeedItem.PollItem -> (holder as PollVH).bind(item.poll)
        }
    }

    // ---------------------------------------------------
    // POST HOLDER
    // ---------------------------------------------------
    inner class PostVH(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val img: ImageView = itemView.findViewById(R.id.recyclerImage)
        private val title: TextView = itemView.findViewById(R.id.recyclerTitle)
        private val desc: TextView = itemView.findViewById(R.id.recyclerCaption)
        private val location: TextView = itemView.findViewById(R.id.recyclerLocation)
        private val username: TextView = itemView.findViewById(R.id.recyclerUsername)

        fun bind(post: Post) {
            title.text = post.title
            desc.text = post.description
            location.text = post.location
            username.text = post.username.ifEmpty { "BuzzBoard User" }

            val imgStr = post.postimage

            // HTTP Image
            if (imgStr.startsWith("http")) {
                Glide.with(itemView.context).load(imgStr).into(img)
            }
            // Base64 Image
            else if (imgStr.isNotEmpty()) {
                try {
                    val bytes = android.util.Base64.decode(imgStr, android.util.Base64.DEFAULT)
                    val bmp = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    img.setImageBitmap(bmp)
                } catch (e: Exception) {
                    img.setImageResource(R.drawable.add_image_icon)
                }
            }
            // No image
            else {
                img.setImageResource(R.drawable.add_image_icon)
            }

            itemView.setOnClickListener { onPostClicked(post) }
        }
    }

    // ---------------------------------------------------
    // POLL HOLDER
    // ---------------------------------------------------
    inner class PollVH(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val question: TextView = itemView.findViewById(R.id.questionText)
        private val statusText: TextView = itemView.findViewById(R.id.statusText)
        private val optionsContainer: RadioGroup = itemView.findViewById(R.id.optionsContainer)

        fun bind(poll: Poll) {

            question.text = poll.question

            val totals = poll.totals
            val totalVotes = totals.sum()
            val denom = if (totalVotes == 0) 1 else totalVotes
            val percents = totals.map { ((it * 100f) / denom).roundToInt() }

            val alreadyVotedIndex = userVotes[poll.id]
            val votingEnabled = (alreadyVotedIndex == null)

            // Reset
            optionsContainer.setOnCheckedChangeListener(null)
            optionsContainer.removeAllViews()

            // Build buttons
            poll.options.forEachIndexed { idx, option ->
                val rb = RadioButton(itemView.context).apply {
                    id = View.generateViewId()
                    text = "$option  ${percents[idx]}%"
                    isEnabled = votingEnabled
                }
                optionsContainer.addView(rb)
            }

            statusText.text = ""

            // User already voted
            if (alreadyVotedIndex != null &&
                alreadyVotedIndex in 0 until optionsContainer.childCount
            ) {
                val rb = optionsContainer.getChildAt(alreadyVotedIndex) as RadioButton
                rb.isChecked = true

                for (i in 0 until optionsContainer.childCount) {
                    val view = optionsContainer.getChildAt(i)
                    view.alpha = if (i == alreadyVotedIndex) 1f else 0.4f
                    view.isEnabled = false
                }
            }

            // Voting listener
            optionsContainer.setOnCheckedChangeListener { group, checkedId ->
                if (!votingEnabled) return@setOnCheckedChangeListener

                val idx = group.indexOfChild(group.findViewById(checkedId))
                if (idx != -1) onVote(poll, idx)
            }
        }
    }
}

