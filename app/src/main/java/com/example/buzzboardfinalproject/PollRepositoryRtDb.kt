package com.example.buzzboardfinalproject

import com.google.firebase.database.*

object PollRepositoryRtDb {

    private val db: DatabaseReference
        get() = FirebaseDatabase.getInstance().reference

    fun vote(poll: Poll, optionIndex: Int, uid: String, callback: (Boolean, String?) -> Unit) {
        if (poll.id.isBlank()) {
            callback(false, "Invalid poll id")
            return
        }
        val userVotePath = db.child("UserPollVotes").child(uid).child(poll.id)
        userVotePath.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snap: DataSnapshot) {
                if (snap.exists()) {
                    callback(false, "Already voted")
                    return
                }
                userVotePath.setValue(optionIndex).addOnCompleteListener { setTask ->
                    if (!setTask.isSuccessful) {
                        callback(false, setTask.exception?.message)
                        return@addOnCompleteListener
                    }
                    val totalRef = db.child("Polls")
                        .child(poll.id)
                        .child("totals")
                        .child(optionIndex.toString())

                    totalRef.runTransaction(object : Transaction.Handler {
                        override fun doTransaction(currentData: MutableData): Transaction.Result {
                            val current = (currentData.getValue(Int::class.java) ?: 0)
                            currentData.value = current + 1
                            return Transaction.success(currentData)
                        }
                        override fun onComplete(
                            error: DatabaseError?,
                            committed: Boolean,
                            currentData: DataSnapshot?
                        ) {
                            if (error != null) callback(false, error.message)
                            else callback(true, null)
                        }
                    })
                }
            }
            override fun onCancelled(error: DatabaseError) {
                callback(false, error.message)
            }
        })
    }
}
