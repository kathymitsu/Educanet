package com.example.educanet

import com.google.firebase.Timestamp

data class ProgressItem(
    val classId: String = "",
    val classTitle: String? = null,   // nombre de la clase (opcional)
    val status: String = "pending",   // pending | done
    val score: Int? = null,
    val updatedAt: Timestamp? = null
)
