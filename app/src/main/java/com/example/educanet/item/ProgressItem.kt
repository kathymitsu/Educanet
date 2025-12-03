package com.example.educanet.item

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

data class ProgressItem(
    val classId: String = "",
    val classTitle: String = "",   // nombre de la clase (opcional)
    val status: String = "visto",   // visto | completado
    val score: Int? = null,
    @ServerTimestamp
    val updatedAt: Timestamp? = null,
    val userId: String = ""
)