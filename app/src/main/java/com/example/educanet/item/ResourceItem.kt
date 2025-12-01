package com.example.educanet.item

import com.google.firebase.Timestamp

data class ResourceItem(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val url: String = "",
    val type: String = "",          // "video", "pdf", "link", etc.
    val createdAt: Timestamp? = null
)
