package com.example.buzzboardfinalproject

data class Poll(
    var id: String = "",
    var question: String = "",
    var options: List<String> = emptyList(),
    var totals: List<Int> = emptyList(),
    var endTime: Long? = null,
    var createdAt: Long = 0L,
    var createdBy: String? = null
)
