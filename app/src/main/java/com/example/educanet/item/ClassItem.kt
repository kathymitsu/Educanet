package com.example.educanet.item

import com.google.firebase.Timestamp

data class ClassItem(
    val title: String = "",
    val description: String = "",
    val videoLink: String = "",
    val professorId: String = "",
    val assignedStudents: List<String> = emptyList(),
    val createdBy: String = "",
    val createdAt: Timestamp? = null,
    val isActive: Boolean = true
)