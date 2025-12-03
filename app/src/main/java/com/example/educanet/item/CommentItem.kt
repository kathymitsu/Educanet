package com.example.educanet.item

import com.google.firebase.Timestamp

data class CommentItem(
    val text: String = "",
    val userId: String = "",
    val userName: String = "",
    val createdAt: Timestamp? = null
)