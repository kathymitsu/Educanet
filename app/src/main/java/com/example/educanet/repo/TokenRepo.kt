package com.example.educanet.repo

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class TokenRepo {
    private val auth get() = FirebaseAuth.getInstance()
    private val db   get() = FirebaseFirestore.getInstance()

    fun saveToken(token: String) {
        val uid = auth.currentUser?.uid ?: return
        val data = mapOf(
            "token" to token,
            "updatedAt" to Timestamp.now()
        )
        db.collection("userTokens").document(uid).set(data)
    }

    suspend fun loadUserRoleAndChildren(): Pair<String?, List<String>> {
        val uid = auth.currentUser?.uid ?: return null to emptyList()
        val doc = db.collection("users").document(uid).get().await()
        val role = doc.getString("role")
        val children = (doc.get("children") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
        return role to children
    }
}
