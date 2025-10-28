package com.example.educanet

import com.google.firebase.Timestamp

data class Comment(
    val id: String = "",
    val text: String = "",
    val userId: String = "",
    val userName: String = "",
    val createdAt: Timestamp? = null
)
