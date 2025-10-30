package com.example.educanet

data class ClassItem(
    val title: String = "",
    val description: String = "",
    val videoLink: String = "",
    val professorId: String = "",
    val assignedStudents: List<String> = emptyList(),
    val createdBy: String = "",
    val createdAt: com.google.firebase.Timestamp? = null,
    val isActive: Boolean = true
)
