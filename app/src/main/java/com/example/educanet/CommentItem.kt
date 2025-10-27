package com.example.educanet

import com.google.firebase.Timestamp

data class CommentItem(
    val text: String = "",
    val userId: String = "",
    val userName: String = "",
    val createdAt: Timestamp? = null
)
