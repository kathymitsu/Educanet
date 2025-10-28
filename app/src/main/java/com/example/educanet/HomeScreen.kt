package com.example.educanet

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onLogout: () -> Unit,
    onNewClass: () -> Unit,
    onOpenClass: (String) -> Unit,
    onOpenResources: () -> Unit,
    onOpenProgress: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val db = remember { FirebaseFirestore.getInstance() }
    val auth = remember { FirebaseAuth.getInstance() }
    val uid = auth.currentUser?.uid.orEmpty()

    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    var name by remember { mutableStateOf("") }
    var role by remember { mutableStateOf<String?>(null) }

    var loading by remember { mutableStateOf(true) }
    var classes by remember { mutableStateOf(listOf<Pair<String, ClassItem>>()) }

    // Suscripción a notificaciones (tópico general)
    LaunchedEffect(Unit) {
        FirebaseMessaging.getInstance().subscribeToTopic("classes")
    }

    // Cargar perfil (nombre + rol)
    LaunchedEffect(uid) {
        if (uid.isBlank()) return@LaunchedEffect
        db.collection("users").document(uid).get()
            .addOnSuccessListener {
                name = it.getString("name") ?: ""
                role = it.getString("role")
                // Si quieres notificaciones por profesor:
                if (role == "profesor") {
                    FirebaseMessaging.getInstance().subscribeToTopic("teacher-$uid")
                }
            }
    }

    // Escuchar clases (profe: solo propias / alumno: todas)
    LaunchedEffect(role, uid) {
        if (role == null) return@LaunchedEffect
        loading = true

        val base = db.collection("classes")
        val query = if (role == "profesor") {
            base.whereEqualTo("createdBy", uid)
                .orderBy("createdAt", Query.Direction.DESCENDING) // requiere índice (ya lo creaste)
        } else {
            base.orderBy("createdAt", Query.Direction.DESCENDING)
        }

        query.addSnapshotListener { snap, _ ->
            classes = snap?.documents?.map { d ->
                d.id to ClassItem(
                    title = d.getString("title") ?: "",
                    description = d.getString("description") ?: "",
                    videoLink = d.getString("videoLink") ?: "",
                    createdBy = d.getString("createdBy") ?: "",
                    createdAt = d.getTimestamp("createdAt")
                )
            } ?: emptyList()
            loading = false
        }
    }

    fun deleteClass(id: String) {
        // elimina comentarios y luego la clase
        val classRef = db.collection("classes").document(id)
        classRef.collection("comments").get()
            .addOnSuccessListener { comments ->
                val batch = db.batch()
                comments.documents.forEach { batch.delete(it.reference) }
                batch.commit().addOnCompleteListener {
                    classRef.delete()
                        .addOnSuccessListener {
                            scope.launch { snackbar.showSnackbar("Clase eliminada") }
                        }
                        .addOnFailureListener { e ->
                            scope.launch { snackbar.showSnackbar("Error eliminando: ${e.message}") }
                        }
                }
            }
            .addOnFailureListener { e ->
                scope.launch { snackbar.showSnackbar("Error eliminando comentarios: ${e.message}") }
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Educanet") },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Ajustes")
                    }
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Default.Logout, contentDescription = "Salir")
                    }
                }
            )
        },
        floatingActionButton = {
            if (role == "profesor") {
                FloatingActionButton(onClick = onNewClass) {
                    Icon(Icons.Default.Add, contentDescription = "Nueva clase")
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { pad ->
        Column(
            modifier = Modifier
                .padding(pad)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Encabezado
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (name.isBlank()) "Bienvenido/a" else "Hola, $name",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f)
                )
                if (role != null) {
                    AssistChip(
                        onClick = {},
                        label = { Text(role!!.replaceFirstChar { it.uppercase() }) }
                    )
                }
            }

            // Accesos
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                ElevatedButton(
                    onClick = onOpenResources,
                    modifier = Modifier.weight(1f)
                ) { Text("Recursos") }

                ElevatedButton(
                    onClick = onOpenProgress,
                    modifier = Modifier.weight(1f)
                ) { Text("Progreso") }
            }

            Divider()

            Text("Clases", style = MaterialTheme.typography.titleMedium)

            if (loading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (classes.isEmpty()) {
                Text("No hay clases aún.")
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(classes.size) { i ->
                        val (id, c) = classes[i]
                        ElevatedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenClass(id) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(12.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(c.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    if (c.description.isNotBlank()) {
                                        Text(
                                            c.description,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }

                                // Basurero si el profe es dueño
                                if (role == "profesor" && c.createdBy == uid) {
                                    IconButton(onClick = { deleteClass(id) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Eliminar")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
