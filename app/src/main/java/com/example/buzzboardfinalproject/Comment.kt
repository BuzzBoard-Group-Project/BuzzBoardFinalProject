package com.example.buzzboardfinalproject

data class Comment(
    var commentId: String? = null,
    var postId: String? = null,
    var userId: String? = null,
    var username: String? = null,
    var text: String? = null,
    var timestamp: Long? = null
)
