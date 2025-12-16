package com.example.educanet.item

import com.google.firebase.Timestamp

data class NotificationItem(
    val title: String = "",
    val body: String = "",
    val type: String = "", // "enrollment", "progress_update"
    val relatedId: String = "", // classId or studentId
    val createdAt: Timestamp = Timestamp.now()
)