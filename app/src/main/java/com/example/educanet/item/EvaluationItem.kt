package com.example.educanet.item

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

data class EvaluationItem(
    val id: String = "",
    val classId: String = "",
    val title: String = "",
    val description: String = "",
    val questions: List<Map<String, Any>> = emptyList(), // Lista de preguntas
    @ServerTimestamp
    val createdAt: Timestamp? = null
)
