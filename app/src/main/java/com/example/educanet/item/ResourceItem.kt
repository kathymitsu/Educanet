package com.example.educanet.item

import com.google.firebase.Timestamp

data class ResourceItem(
    val title: String = "",
    val type: String = "",   // "book" | "article" | "video"
    val url: String = "",
    val createdBy: String = "",
    val createdAt: Timestamp? = null
)