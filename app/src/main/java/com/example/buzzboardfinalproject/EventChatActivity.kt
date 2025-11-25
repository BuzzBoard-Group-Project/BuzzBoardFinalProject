package com.example.buzzboardfinalproject

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.buzzboardfinalproject.databinding.ActivityEventChatBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

data class ChatMessage(
    val senderId: String = "",
    val text: String = "",
    val timestamp: Long = 0L
)

class EventChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEventChatBinding
    private lateinit var chatId: String
    private val db by lazy { FirebaseDatabase.getInstance() }
    private val currentUid get() = FirebaseAuth.getInstance().currentUser?.uid

    private val messages = ArrayList<ChatMessage>()
    private lateinit var adapter: MessageAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEventChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        chatId = intent.getStringExtra("post_id") ?: run {
            Toast.makeText(this, "No chat id", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        adapter = MessageAdapter(messages, currentUid)
        binding.rvMessages.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        binding.rvMessages.adapter = adapter

        binding.btnBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }

        binding.btnSend.setOnClickListener {
            sendMessage()
        }

        listenForMessages()
    }

    private fun sendMessage() {
        val text = binding.etMessage.text.toString().trim()
        val uid = currentUid ?: return
        if (text.isEmpty()) return

        val msgRef = db.getReference("EventChats").child(chatId).child("messages").push()
        val now = System.currentTimeMillis()

        val msg = ChatMessage(
            senderId = uid,
            text = text,
            timestamp = now
        )

        msgRef.setValue(msg).addOnSuccessListener {
            val chatMetaRef = db.getReference("EventChats").child(chatId)

            // store last message text
            chatMetaRef.child("lastMessage").setValue(text)
            // 👇 store last activity time (used for sorting)
            chatMetaRef.child("lastMessageTime").setValue(now)

            binding.etMessage.setText("")
        }
    }


    private fun listenForMessages() {
        val msgsRef = db.getReference("EventChats").child(chatId).child("messages")
        msgsRef.orderByChild("timestamp").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                messages.clear()
                for (snap in snapshot.children) {
                    val m = snap.getValue(ChatMessage::class.java)
                    if (m != null) messages.add(m)
                }
                adapter.notifyDataSetChanged()
                if (messages.isNotEmpty()) {
                    binding.rvMessages.scrollToPosition(messages.size - 1)
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }
}
