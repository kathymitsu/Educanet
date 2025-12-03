package com.example.educanet.item

import com.google.firebase.Timestamp

data class GradeItem(
    val id: String = "",
    val studentId: String = "",
    val studentName: String = "",
    val score: Double = 0.0,
    val comment: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
    val updatedBy: String = ""
)