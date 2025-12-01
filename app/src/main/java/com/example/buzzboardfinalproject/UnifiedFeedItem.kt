package com.example.buzzboardfinalproject

sealed class UnifiedFeedItem {
    data class PostItem(val post: Post) : UnifiedFeedItem()
    data class PollItem(val poll: Poll) : UnifiedFeedItem()
}
