package com.example.buzzboardfinalproject

import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.buzzboardfinalproject.databinding.ActivityEventChatBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

data class ChatMessage(
    val senderId: String = "",
    val senderName: String = "",
    val text: String = "",
    val timestamp: Long = 0L
)

class EventChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEventChatBinding
    private lateinit var chatId: String       // == postId for this chat
    private lateinit var eventTitle: String

    private val db by lazy { FirebaseDatabase.getInstance() }
    private val currentUid get() = FirebaseAuth.getInstance().currentUser?.uid

    private val messages = ArrayList<ChatMessage>()
    private lateinit var adapter: MessageAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEventChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Match status bar to header yellow
        window.statusBarColor = Color.parseColor("#FFC107")

        // Read chat id and title from intent
        chatId = intent.getStringExtra("post_id") ?: run {
            Toast.makeText(this, "No chat id", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        eventTitle = intent.getStringExtra("event_title") ?: "Event Chat"
        binding.tvChatTitle.text = eventTitle

        // Setup messages list
        adapter = MessageAdapter(messages, currentUid)
        binding.rvMessages.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        binding.rvMessages.adapter = adapter

        // Back
        binding.btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // Participants list
        binding.btnParticipants.setOnClickListener {
            showParticipantsDialog()
        }

        // 🚪 Leave chat + unregister
        binding.btnLeaveChat.setOnClickListener {
            confirmLeaveChat()
        }

        // Send message
        binding.btnSend.setOnClickListener {
            sendMessage()
        }

        listenForMessages()
    }

    override fun onResume() {
        super.onResume()
        updateLastSeen()
    }

    override fun onPause() {
        super.onPause()
        updateLastSeen()
    }

    // Track that user has viewed chat & store their name
    private fun updateLastSeen() {
        val uid = currentUid ?: return
        val user = FirebaseAuth.getInstance().currentUser
        val name = user?.displayName ?: user?.email ?: "Student"

        val participantRef = db.getReference("EventChats")
            .child(chatId)
            .child("participants")
            .child(uid)

        participantRef.child("lastSeenTime").setValue(System.currentTimeMillis())
        participantRef.child("name").setValue(name)
    }

    // Send a message
    private fun sendMessage() {
        val text = binding.etMessage.text.toString().trim()
        val uid = currentUid ?: return
        if (text.isEmpty()) return

        val now = System.currentTimeMillis()
        val msgRef = db.getReference("EventChats")
            .child(chatId)
            .child("messages")
            .push()

        val user = FirebaseAuth.getInstance().currentUser
        val senderName = user?.displayName ?: user?.email ?: "You"

        val msg = ChatMessage(
            senderId = uid,
            senderName = senderName,
            text = text,
            timestamp = now
        )

        msgRef.setValue(msg).addOnSuccessListener {
            val chatMetaRef = db.getReference("EventChats").child(chatId)
            chatMetaRef.child("lastMessage").setValue(text)
            chatMetaRef.child("lastMessageTime").setValue(now)

            binding.etMessage.setText("")
            updateLastSeen()
        }
    }

    // Listen for all messages in this chat
    private fun listenForMessages() {
        val msgsRef = db.getReference("EventChats")
            .child(chatId)
            .child("messages")

        msgsRef.orderByChild("timestamp")
            .addValueEventListener(object : ValueEventListener {
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

    // Show all participants in this chat
    private fun showParticipantsDialog() {
        val participantsRef = db.getReference("EventChats")
            .child(chatId)
            .child("participants")

        val myUid = currentUid

        participantsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    Toast.makeText(
                        this@EventChatActivity,
                        "No participants yet.",
                        Toast.LENGTH_SHORT
                    ).show()
                    return
                }

                val names = mutableListOf<String>()

                for (child in snapshot.children) {
                    val uid = child.key ?: continue
                    val storedName = child.child("name").getValue(String::class.java)

                    val label = when {
                        myUid != null && uid == myUid -> "You"
                        !storedName.isNullOrBlank() -> storedName
                        else -> "Student"
                    }

                    names.add(label)
                }

                if (names.isEmpty()) {
                    Toast.makeText(
                        this@EventChatActivity,
                        "No participants yet.",
                        Toast.LENGTH_SHORT
                    ).show()
                    return
                }

                val arr = names.toTypedArray()

                AlertDialog.Builder(this@EventChatActivity)
                    .setTitle("Chat participants (${names.size})")
                    .setItems(arr, null)
                    .setPositiveButton("OK", null)
                    .show()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    this@EventChatActivity,
                    "Failed to load participants.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    // 🔔 Ask user before leaving chat + unregistering
    private fun confirmLeaveChat() {
        AlertDialog.Builder(this)
            .setTitle("Leave chat?")
            .setMessage("You will be removed from this chat and unregistered from this event.")
            .setPositiveButton("Leave") { _, _ ->
                leaveChatAndUnregister()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // 🚪 Remove user from EventChats + EventRegistrations and close screen
    private fun leaveChatAndUnregister() {
        val uid = currentUid ?: return
        val rootRef = db.reference

        // In your app, chatId is the chat’s key and also the postId for registration
        val updates = hashMapOf<String, Any?>(
            // remove from chat participants
            "/EventChats/$chatId/participants/$uid" to null,
            // unregister from event
            "/EventRegistrations/$chatId/$uid" to null
        )

        rootRef.updateChildren(updates).addOnCompleteListener {
            // ✅ ALSO mark locally that this user left this chat
            val prefs = getSharedPreferences("chat_prefs", MODE_PRIVATE)
            prefs.edit()
                .putBoolean("left_$chatId", true)
                .apply()

            Toast.makeText(
                this,
                "You left the chat and were unregistered.",
                Toast.LENGTH_SHORT
            ).show()

            finish()   // back to Messages
        }
    }



}
