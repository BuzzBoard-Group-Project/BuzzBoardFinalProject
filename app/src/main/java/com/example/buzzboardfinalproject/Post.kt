package com.example.buzzboardfinalproject

data class Post(
    val postid: String = "",
    val title: String = "",
    val description: String = "",
    val location: String = "",
    val postimage: String = "",
    val publisher: String = "",
    val time: String ="",
    var isFavorite: Boolean = false

)
        