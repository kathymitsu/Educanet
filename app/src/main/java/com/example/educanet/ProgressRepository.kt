package com.example.educanet

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object ProgressRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private fun doc(uid: String, classId: String) =
        db.collection("progress").document(uid)
            .collection("items").document(classId)

    suspend fun setStatus(classId: String, status: String) {
        val uid = auth.currentUser?.uid ?: return
        val data = hashMapOf(
            "classId" to classId,
            "status" to status,
            "updatedAt" to Timestamp.now()
        )
        doc(uid, classId).set(data).await()
    }

    suspend fun toggle(classId: String, current: String?) {
        val next = if (current == "done") "pending" else "done"
        setStatus(classId, next)
    }
}
