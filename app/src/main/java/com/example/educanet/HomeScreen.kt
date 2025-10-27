package com.example.educanet

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onLogout: () -> Unit,
    onNewClass: () -> Unit,
    onOpenClass: (String) -> Unit
) {
    val auth = remember { FirebaseAuth.getInstance() }
    val db = remember { FirebaseFirestore.getInstance() }

    val uid = auth.currentUser?.uid
    var role by remember { mutableStateOf<String?>(null) }
    var name by remember { mutableStateOf<String?>(null) }
    var classes by remember { mutableStateOf(listOf<Pair<String, ClassItem>>()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    // dialogo confirmación
    var toDeleteId by remember { mutableStateOf<String?>(null) }
    var deleting by remember { mutableStateOf(false) }
    var deleteError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val myUid = uid ?: return@LaunchedEffect

        db.collection("users").document(myUid).get()
            .addOnSuccessListener {
                role = it.getString("role")
                name = it.getString("name")
            }
            .addOnFailureListener { e -> error = e.message }

        db.collection("classes")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, e ->
                if (e != null) { error = e.message; loading = false; return@addSnapshotListener }
                classes = snap?.documents?.mapNotNull { d ->
                    d.toObject(ClassItem::class.java)?.let { d.id to it }
                } ?: emptyList()
                loading = false
            }
    }

    // elimina subcolección 'comments' y luego la clase
    fun deleteClassWithComments(classId: String) {
        deleting = true; deleteError = null
        val classRef = db.collection("classes").document(classId)
        classRef.collection("comments").get()
            .addOnSuccessListener { qs ->
                val batch = db.batch()
                qs.documents.forEach { batch.delete(it.reference) }
                batch.commit()
                    .addOnCompleteListener {
                        classRef.delete()
                            .addOnSuccessListener { toDeleteId = null }
                            .addOnFailureListener { e -> deleteError = e.message }
                            .addOnCompleteListener { deleting = false }
                    }
            }
            .addOnFailureListener { e ->
                deleteError = e.message
                deleting = false
            }
    }

    Scaffold(
        topBar = { CenterAlignedTopAppBar(title = { Text("Educanet") }) },
        floatingActionButton = {
            if (role == "profesor") {
                FloatingActionButton(onClick = onNewClass) { Text("+") }
            }
        }
    ) { pad ->
        Column(
            modifier = Modifier.padding(pad).fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when {
                loading -> CircularProgressIndicator()
                error != null -> Text("Error: $error", color = MaterialTheme.colorScheme.error)
                else -> {
                    Text("Hola ${name ?: ""} 👋", style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.height(12.dp))

                    if (classes.isEmpty()) {
                        Text("No hay clases aún")
                    } else {
                        LazyColumn(Modifier.fillMaxWidth()) {
                            items(classes.size) { i ->
                                val (id, c) = classes[i]
                                ElevatedCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp)
                                        .clickable { onOpenClass(id) }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(Modifier.weight(1f)) {
                                            Text(c.title, style = MaterialTheme.typography.titleMedium)
                                            if (c.description.isNotBlank()) {
                                                Spacer(Modifier.height(4.dp)); Text(c.description)
                                            }
                                        }

                                        // Solo profesor creador ve el icono de eliminar
                                        if (role == "profesor" && c.createdBy == uid) {
                                            IconButton(onClick = { toDeleteId = id }) {
                                                Icon(Icons.Default.Delete, contentDescription = "Eliminar")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(24.dp))
                    Button(onClick = onLogout, modifier = Modifier.fillMaxWidth()) {
                        Text("Cerrar sesión")
                    }
                }
            }
        }
    }

    // Diálogo de confirmación
    if (toDeleteId != null) {
        AlertDialog(
            onDismissRequest = { if (!deleting) toDeleteId = null },
            title = { Text("Eliminar clase") },
            text = {
                if (deleteError != null)
                    Text("Error: $deleteError")
                else
                    Text(if (deleting) "Eliminando..." else "¿Seguro que quieres eliminar esta clase? Se borrarán también sus comentarios.")
            },
            confirmButton = {
                TextButton(
                    onClick = { toDeleteId?.let { deleteClassWithComments(it) } },
                    enabled = !deleting
                ) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { toDeleteId = null }, enabled = !deleting) { Text("Cancelar") }
            }
        )
    }
}
