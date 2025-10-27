package com.example.educanet

import com.google.firebase.Timestamp

data class ClassItem(
    val title: String = "",
    val description: String = "",
    val videoLink: String = "",
    val createdBy: String = "",
    val createdAt: Timestamp? = null
)
