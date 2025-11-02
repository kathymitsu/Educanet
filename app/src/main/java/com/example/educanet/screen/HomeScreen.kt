package com.example.educanet.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.educanet.item.ClassItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onLogout: () -> Unit,
    onNewClass: () -> Unit,
    onOpenClass: (String) -> Unit,
    onOpenResources: () -> Unit,
    onOpenProgress: (userId: String) -> Unit,
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

    // 🔔 Suscripción a notificaciones
    LaunchedEffect(Unit) {
        FirebaseMessaging.getInstance().subscribeToTopic("classes")
    }

    // 👤 Cargar perfil
    LaunchedEffect(uid) {
        if (uid.isBlank()) return@LaunchedEffect
        db.collection("users").document(uid).get()
            .addOnSuccessListener {
                name = it.getString("name") ?: ""
                role = it.getString("role")
                if (role == "profesor") {
                    FirebaseMessaging.getInstance().subscribeToTopic("teacher-$uid")
                }
            }
    }

    // 📚 Cargar clases según el rol del usuario
    LaunchedEffect(role, uid) {
        if (role == null || uid.isBlank()) return@LaunchedEffect
        loading = true
        val base = db.collection("classes")

        when (role) {
            "admin" -> {
                // Admin ve todas las clases
                base.addSnapshotListener { snap, err ->
                    if (err != null) { loading = false; return@addSnapshotListener }
                    val list = snap?.documents?.map { d ->
                        d.id to ClassItem(
                            title = d.getString("title") ?: "",
                            description = d.getString("description") ?: "",
                            videoLink = d.getString("videoLink") ?: "",
                            professorId = d.getString("professorId") ?: "",
                            assignedStudents = (d.get("assignedStudents") as? List<*>)?.filterIsInstance<String>()
                                ?: emptyList(),
                            createdBy = d.getString("createdBy") ?: "",
                            createdAt = d.getTimestamp("createdAt"),
                            isActive = d.getBoolean("isActive") ?: true
                        )
                    } ?: emptyList()
                    classes = list.sortedByDescending { it.second.createdAt?.toDate()?.time ?: 0L }
                    loading = false
                }
            }

            "profesor" -> {
                // Profesor ve solo sus clases asignadas
                base.whereEqualTo("professorId", uid)
                    .addSnapshotListener { snap, err ->
                        if (err != null) { loading = false; return@addSnapshotListener }
                        val list = snap?.documents?.map { d ->
                            d.id to ClassItem(
                                title = d.getString("title") ?: "",
                                description = d.getString("description") ?: "",
                                videoLink = d.getString("videoLink") ?: "",
                                professorId = d.getString("professorId") ?: "",
                                assignedStudents = (d.get("assignedStudents") as? List<*>)?.filterIsInstance<String>()
                                    ?: emptyList(),
                                createdBy = d.getString("createdBy") ?: "",
                                createdAt = d.getTimestamp("createdAt"),
                                isActive = d.getBoolean("isActive") ?: true
                            )
                        } ?: emptyList()
                        classes = list.sortedByDescending { it.second.createdAt?.toDate()?.time ?: 0L }
                        loading = false
                    }
            }

            else -> {
                // Estudiante ve clases donde esté asignado
                base.whereArrayContains("assignedStudents", uid)
                    .addSnapshotListener { snap, err ->
                        if (err != null) { loading = false; return@addSnapshotListener }
                        val list = snap?.documents?.map { d ->
                            d.id to ClassItem(
                                title = d.getString("title") ?: "",
                                description = d.getString("description") ?: "",
                                videoLink = d.getString("videoLink") ?: "",
                                professorId = d.getString("professorId") ?: "",
                                assignedStudents = (d.get("assignedStudents") as? List<*>)?.filterIsInstance<String>()
                                    ?: emptyList(),
                                createdBy = d.getString("createdBy") ?: "",
                                createdAt = d.getTimestamp("createdAt"),
                                isActive = d.getBoolean("isActive") ?: true
                            )
                        } ?: emptyList()
                        classes = list.sortedByDescending { it.second.createdAt?.toDate()?.time ?: 0L }
                        loading = false
                    }
            }
        }
    }

    // 🧭 UI
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
            // Solo admin puede crear clases
            if (role == "admin") {
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

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                ElevatedButton(onClick = onOpenResources, modifier = Modifier.weight(1f)) {
                    Text("Recursos")
                }
                ElevatedButton(
                onClick = {
                    if (uid.isNotBlank()) {
                        onOpenProgress(uid)
                    }
                },
                modifier = Modifier.weight(1f)
                ) {
                Text("Progreso")
            }
            }

            HorizontalDivider()
            Text("Clases", style = MaterialTheme.typography.titleMedium)

            if (loading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (classes.isEmpty()) {
                Text("No hay clases para tu rol.")
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(classes.size) { i ->
                        val (id, c) = classes[i]
                        ElevatedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenClass(id) }
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Text(
                                    c.title.ifBlank { "Clase: $id" },
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (c.description.isNotBlank()) {
                                    Text(
                                        c.description,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                if (!c.isActive) {
                                    Spacer(Modifier.height(4.dp))
                                    AssistChip(onClick = {}, label = { Text("Inactiva") })
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
