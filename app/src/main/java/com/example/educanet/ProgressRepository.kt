package com.example.educanet

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object ProgressRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val progressCollection = db.collection("progress")

    suspend fun setStatus(classId: String, classTitle: String , status: String) {
        val uid = auth.currentUser?.uid ?: return
        val querySnapshot = progressCollection
            .whereEqualTo("userId", uid)
            .whereEqualTo("classId", classId)
            .limit(1)
            .get()
            .await()
        val progressData = mapOf(
            "userId" to uid,
            "classId" to classId,
            "classTitle" to classTitle,
            "status" to status,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (querySnapshot.isEmpty) {
            progressCollection.add(progressData).await()
        } else {
            val docId = querySnapshot.documents.first().id
            progressCollection.document(docId).update(progressData).await()
        }
    }
    suspend fun toggle(classId: String, classTitle: String, currentStatus: String?) {
        val nextStatus = if (currentStatus != "completado") "completado" else "visto"
        setStatus(classId, classTitle, nextStatus)
    }
}
