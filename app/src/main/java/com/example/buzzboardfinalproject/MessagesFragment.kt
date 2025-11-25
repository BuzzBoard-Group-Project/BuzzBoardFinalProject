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


data class SimpleChatRoom(
    val chatId: String = "",
    val title: String = "",
    val postId: String = "",
    val participantCount: Int = 0,
    val lastActive: Long = 0L
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

        adapter = SimpleChatAdapter(
            requireContext(),
            rooms
        ) { room ->
            val i = Intent(requireContext(), EventChatActivity::class.java)
            // EventChatActivity expects "post_id" (which we use as chatId)
            i.putExtra("post_id", room.chatId)
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

        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                rooms.clear()
                for (chatSnap in snapshot.children) {
                    val chatId = chatSnap.key ?: continue

                    val participants = chatSnap.child("participants")
                    if (participants.hasChild(uid)) {

                        val title = chatSnap.child("title")
                            .getValue(String::class.java) ?: "Event chat"

                        val postId = chatSnap.child("postId")
                            .getValue(String::class.java) ?: chatId

                        val count = participants.childrenCount.toInt()

                        // 👇 read last message time (might be null for older chats)
                        val lastActive = chatSnap.child("lastMessageTime")
                            .getValue(Long::class.java) ?: 0L

                        rooms.add(
                            SimpleChatRoom(
                                chatId = chatId,
                                title = title,
                                postId = postId,
                                participantCount = count,
                                lastActive = lastActive
                            )
                        )
                    }
                }

// 👇 sort by lastActive DESC (most recent at the top)
                rooms.sortByDescending { it.lastActive }

                adapter.notifyDataSetChanged()
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
