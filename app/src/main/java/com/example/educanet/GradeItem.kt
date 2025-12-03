package com.example.educanet

data class GradeItem(
    val id: String = "",
    val studentId: String = "",
    val studentName: String = "",
    val score: Double = 0.0,
    val comment: String = "",
    val createdAt: com.google.firebase.Timestamp? = null,
    val updatedAt: com.google.firebase.Timestamp? = null,
    val updatedBy: String = ""
)
