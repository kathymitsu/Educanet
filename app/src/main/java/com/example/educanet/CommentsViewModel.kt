package com.example.educanet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class CommentsViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _state = MutableStateFlow<List<Comment>>(emptyList())
    val state: StateFlow<List<Comment>> = _state

    fun listen(classId: String) {
        db.collection("classes").document(classId)
            .collection("comments")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snap, _ ->
                _state.value = snap?.documents?.map { d ->
                    Comment(
                        id = d.id,
                        text = d.getString("text") ?: "",
                        userId = d.getString("userId") ?: "",
                        userName = d.getString("userName") ?: "",
                        createdAt = d.getTimestamp("createdAt")
                    )
                } ?: emptyList()
            }
    }

    fun send(classId: String, text: String, userName: String) {
        val uid = auth.currentUser?.uid ?: return
        val data = hashMapOf(
            "text" to text.trim(),
            "userId" to uid,
            "userName" to userName,
            "createdAt" to com.google.firebase.Timestamp.now()
        )
        db.collection("classes").document(classId)
            .collection("comments").add(data)
    }

    fun delete(classId: String, commentId: String) {
        db.collection("classes").document(classId)
            .collection("comments").document(commentId).delete()
    }
}
