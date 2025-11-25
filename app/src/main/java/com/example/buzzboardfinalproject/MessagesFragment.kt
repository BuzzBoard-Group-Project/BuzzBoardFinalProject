package com.example.buzzboardfinalproject

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.buzzboardfinalproject.databinding.FragmentMessagesBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import android.content.Context


data class SimpleChatRoom(
    val chatId: String = "",
    val title: String = "",
    val postId: String = "",
    val participantCount: Int = 0,
    val lastMessage: String = "",
    val lastActive: Long = 0L,
    val hasUnread: Boolean = false
)

class MessagesFragment : Fragment() {

    private var _binding: FragmentMessagesBinding? = null
    private val binding get() = _binding!!

    private val rooms = ArrayList<SimpleChatRoom>()
    private lateinit var adapter: SimpleChatAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMessagesBinding.inflate(inflater, container, false)

        adapter = SimpleChatAdapter(requireContext(), rooms) { room ->
            val i = Intent(requireContext(), EventChatActivity::class.java)
            i.putExtra("post_id", room.chatId)          // chatId == postId
            i.putExtra("event_title", room.title)       // show title in header
            startActivity(i)
        }

        binding.rvChatRooms.layoutManager = LinearLayoutManager(requireContext())
        binding.rvChatRooms.adapter = adapter

        loadMyChats()

        return binding.root
    }

    private fun loadMyChats() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val ref = FirebaseDatabase.getInstance().getReference("EventChats")

        // 🔹 Read local flags for chats you’ve left
        val prefs = requireContext().getSharedPreferences("chat_prefs", Context.MODE_PRIVATE)

        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                rooms.clear()

                for (chatSnap in snapshot.children) {
                    val chatId = chatSnap.key ?: continue

                    // 🚫 1) If we have locally left this chat, skip it
                    if (prefs.getBoolean("left_$chatId", false)) {
                        continue
                    }

                    val participantsSnap = chatSnap.child("participants")

                    // 🚫 2) Also skip if you’re no longer in participants on the server
                    if (!participantsSnap.hasChild(uid)) {
                        continue
                    }

                    val title = chatSnap.child("title")
                        .getValue(String::class.java) ?: "Event chat"

                    val postId = chatSnap.child("postId")
                        .getValue(String::class.java) ?: chatId

                    val count = participantsSnap.childrenCount.toInt()

                    val lastMessage = chatSnap.child("lastMessage")
                        .getValue(String::class.java) ?: ""

                    val lastActive = chatSnap.child("lastMessageTime")
                        .getValue(Long::class.java) ?: 0L

                    val lastSeenTime = participantsSnap.child(uid)
                        .child("lastSeenTime")
                        .getValue(Long::class.java) ?: 0L

                    val hasUnread = lastActive > lastSeenTime

                    rooms.add(
                        SimpleChatRoom(
                            chatId = chatId,
                            title = title,
                            postId = postId,
                            participantCount = count,
                            lastMessage = lastMessage,
                            lastActive = lastActive,
                            hasUnread = hasUnread
                        )
                    )
                }

                rooms.sortByDescending { it.lastActive }

                adapter.submitList(rooms.toList())
                binding.tvEmpty.visibility =
                    if (rooms.isEmpty()) View.VISIBLE else View.GONE
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
