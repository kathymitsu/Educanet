package com.example.educanet

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassDetailScreen(
    classId: String,
    onBack: () -> Unit
) {
    val auth = remember { FirebaseAuth.getInstance() }
    val db = remember { FirebaseFirestore.getInstance() }
    val uid = auth.currentUser?.uid

    var role by remember { mutableStateOf<String?>(null) }

    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var snapshot by remember { mutableStateOf<DocumentSnapshot?>(null) }
    var item by remember { mutableStateOf<ClassItem?>(null) }

    // progreso
    var isDone by remember { mutableStateOf<Boolean?>(null) }
    var saving by remember { mutableStateOf(false) }

    // comentarios
    val commentsVM: CommentsViewModel = viewModel()
    val comments by commentsVM.state.collectAsState()
    var myName by remember { mutableStateOf("") }
    var draft by remember { mutableStateOf("") }

    // rol + nombre
    LaunchedEffect(Unit) {
        uid ?: return@LaunchedEffect
        db.collection("users").document(uid).get()
            .addOnSuccessListener {
                role = it.getString("role")
                myName = it.getString("name") ?: "Usuario"
            }
            .addOnFailureListener { e -> error = e.message }
    }

    // clase
    LaunchedEffect(classId) {
        loading = true; error = null
        db.collection("classes").document(classId)
            .addSnapshotListener { snap, e ->
                if (e != null) { error = e.message; loading = false; return@addSnapshotListener }
                snapshot = snap
                item = snap?.toObject<ClassItem>()
                loading = false
            }
    }

    // progreso escucha
    LaunchedEffect(uid, classId) {
        if (uid == null) return@LaunchedEffect
        db.collection("progress").document(uid)
            .collection("items").document(classId)
            .addSnapshotListener { s, _ ->
                isDone = if (s != null && s.exists()) (s.getString("status") == "done") else false
            }
    }

    // comentarios escucha
    LaunchedEffect(classId) { commentsVM.listen(classId) }

    fun markDone() {
        val myUid = uid ?: return
        saving = true
        val ref = db.collection("progress").document(myUid)
            .collection("items").document(classId)
        val data = hashMapOf(
            "classId" to classId,
            "classTitle" to (item?.title ?: ""),
            "status" to "done",
            "updatedAt" to com.google.firebase.Timestamp.now()
        )
        ref.set(data).addOnCompleteListener { saving = false }
    }

    fun undoDone() {
        val myUid = uid ?: return
        saving = true
        db.collection("progress").document(myUid)
            .collection("items").document(classId)
            .delete().addOnCompleteListener { saving = false }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Detalle de clase") },
                navigationIcon = { TextButton(onClick = onBack) { Text("← Atrás") } }
            )
        }
    ) { pad ->
        Column(
            modifier = Modifier.padding(pad).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when {
                loading -> CircularProgressIndicator()
                error != null -> Text("Error: $error", color = MaterialTheme.colorScheme.error)
                item == null -> Text("Clase no encontrada")
                else -> {
                    Text(item!!.title, style = MaterialTheme.typography.headlineSmall)
                    if (item!!.description.isNotBlank()) Text(item!!.description)
                    if (item!!.videoLink.isNotBlank()) Text("Video: ${item!!.videoLink}")

                    // ------- Progreso alumno ------
                    AnimatedVisibility(visible = role == "estudiante" && isDone != null) {
                        val finished = isDone == true
                        Button(
                            onClick = { if (!finished) markDone() else undoDone() },
                            enabled = !saving
                        ) {
                            Text(if (finished) "Desmarcar como completada" else "Marcar como completada")
                        }
                    }

                    Divider(thickness = 1.dp)
                    Text("Comentarios", style = MaterialTheme.typography.titleMedium)

                    // Lista de comentarios
                    LazyColumn(
                        modifier = Modifier.weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(comments.size) { i ->
                            val c = comments[i]
                            ElevatedCard(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(10.dp)) {
                                    Text("${c.userName}", style = MaterialTheme.typography.labelLarge)
                                    Text(c.text)
                                    val isOwner = c.userId == uid
                                    val isTeacherOwner = snapshot?.getString("createdBy") == uid
                                    if (isOwner || isTeacherOwner) {
                                        TextButton(onClick = { commentsVM.delete(classId, c.id) }) {
                                            Text("Eliminar")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Input comentar (ambos roles)
                    OutlinedTextField(
                        value = draft,
                        onValueChange = { draft = it },
                        label = { Text("Escribe un comentario") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            if (draft.isNotBlank()) {
                                commentsVM.send(classId, draft, myName)
                                draft = ""
                            }
                        },
                        enabled = draft.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Enviar") }
                }
            }
        }
    }
}
