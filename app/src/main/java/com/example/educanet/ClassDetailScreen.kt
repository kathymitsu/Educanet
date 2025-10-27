package com.example.educanet

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import android.content.Intent
import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassDetailScreen(
    classId: String,
    onBack: () -> Unit
) {
    val db = remember { FirebaseFirestore.getInstance() }
    val auth = remember { FirebaseAuth.getInstance() }
    val ctx = LocalContext.current

    var item by remember { mutableStateOf<ClassItem?>(null) }
    var comments by remember { mutableStateOf(listOf<CommentItem>()) }
    var input by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    // Carga clase y comentarios en tiempo real
    LaunchedEffect(classId) {
        db.collection("classes").document(classId).get()
            .addOnSuccessListener { item = it.toObject(ClassItem::class.java) }
            .addOnFailureListener { e -> error = e.message }

        db.collection("classes").document(classId)
            .collection("comments")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snap, e ->
                if (e != null) { error = e.message; loading = false; return@addSnapshotListener }
                comments = snap?.toObjects(CommentItem::class.java) ?: emptyList()
                loading = false
            }
    }

    fun sendComment() {
        val uid = auth.currentUser?.uid ?: return
        val name = auth.currentUser?.displayName ?: ""   // o desde /users si lo deseas
        val data = mapOf(
            "text" to input.trim(),
            "userId" to uid,
            "userName" to name,
            "createdAt" to FieldValue.serverTimestamp()
        )
        if (input.isBlank()) return
        db.collection("classes").document(classId)
            .collection("comments")
            .add(data)
            .addOnSuccessListener { input = "" }
    }

    fun openLink(url: String) {
        if (url.isBlank()) return
        ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(item?.title ?: "Clase") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Atrás") }
                }
            )
        }
    ) { pad ->
        Column(
            modifier = Modifier
                .padding(pad)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            if (loading) { CircularProgressIndicator(); return@Column }
            if (error != null) { Text("Error: $error", color = MaterialTheme.colorScheme.error); return@Column }
            val c = item ?: return@Column

            // Info de la clase
            Text(c.title, style = MaterialTheme.typography.titleLarge)
            if (c.description.isNotBlank()) {
                Spacer(Modifier.height(4.dp)); Text(c.description)
            }
            if (c.videoLink.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Button(onClick = { openLink(c.videoLink) }) { Text("Abrir clase ▶") }
            }

            Spacer(Modifier.height(16.dp))
            Divider()
            Spacer(Modifier.height(8.dp))
            Text("Comentarios", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            // Lista comentarios
            if (comments.isEmpty()) {
                Text("Sé el primero en comentar.")
            } else {
                LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                    items(comments.size) { i ->
                        val cm = comments[i]
                        Column(Modifier.padding(vertical = 6.dp)) {
                            Text(cm.userName.ifBlank { "Usuario" }, style = MaterialTheme.typography.labelLarge)
                            Text(cm.text)
                        }
                        Divider()
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = input, onValueChange = { input = it },
                label = { Text("Escribe un comentario") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Button(onClick = { sendComment() }, modifier = Modifier.fillMaxWidth()) {
                Text("Enviar")
            }
        }
    }
}
