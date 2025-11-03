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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

data class ClassItemLite(
    val title: String = "",
    val description: String = "",
    val professorId: String = "",
    val assignedStudents: List<String> = emptyList(),
    val isActive: Boolean = true,
    val imageUrl: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onLogout: () -> Unit,
    onNewClass: () -> Unit,
    onOpenClass: (String) -> Unit,
    onOpenResources: () -> Unit,
    onOpenProgress: (String) -> Unit,
    onOpenSettings: () -> Unit
) {
    val db = remember { FirebaseFirestore.getInstance() }
    val uid = remember { FirebaseAuth.getInstance().currentUser?.uid.orEmpty() }

    var name by remember { mutableStateOf("") }
    var role by remember { mutableStateOf<String?>(null) }

    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var classes by remember { mutableStateOf(listOf<Pair<String, ClassItemLite>>()) }

    // PERFIL robusto (crea doc si no existe para evitar spinner infinito)
    LaunchedEffect(uid) {
        if (uid.isBlank()) return@LaunchedEffect
        try {
            val ref = db.collection("users").document(uid)
            val snap = ref.get().await()
            if (!snap.exists()) {
                val fallback = mapOf(
                    "uid" to uid,
                    "name" to (FirebaseAuth.getInstance().currentUser?.displayName
                        ?: FirebaseAuth.getInstance().currentUser?.email ?: "Usuario"),
                    "role" to "estudiante"
                )
                ref.set(fallback).await()
                name = fallback["name"] as String
                role = fallback["role"] as String
            } else {
                name = snap.getString("name") ?: "Usuario"
                role = snap.getString("role") ?: "estudiante"
            }
        } catch (e: Exception) {
            name = FirebaseAuth.getInstance().currentUser?.displayName
                ?: FirebaseAuth.getInstance().currentUser?.email ?: "Usuario"
            role = "estudiante"
        }
    }

    // Carga de clases según rol — siempre termina loading aunque falle
    LaunchedEffect(role, uid) {
        if (role == null || uid.isBlank()) return@LaunchedEffect
        loading = true
        error = null

        val base = db.collection("classes")
        val q = when (role) {
            "admin"    -> base
            "profesor" -> base.whereEqualTo("professorId", uid)
            else       -> base.whereArrayContains("assignedStudents", uid)
        }
        q.addSnapshotListener { snap, err ->
            if (err != null) {
                classes = emptyList()
                loading = false
                error = "No se pudieron cargar las clases (${err.code})."
                return@addSnapshotListener
            }
            classes = snap?.documents?.map { d ->
                d.id to ClassItemLite(
                    title = d.getString("title") ?: "",
                    description = d.getString("description") ?: "",
                    professorId = d.getString("professorId") ?: "",
                    assignedStudents = (d.get("assignedStudents") as? List<*>)?.filterIsInstance<String>()
                        ?: emptyList(),
                    isActive = d.getBoolean("isActive") ?: true,
                    imageUrl = d.getString("imageUrl") ?: ""
                )
            } ?: emptyList()
            loading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Educanet") },
                actions = {
                    IconButton(onClick = onOpenSettings) { Icon(Icons.Default.Settings, null) }
                    IconButton(onClick = onLogout) { Icon(Icons.Default.Logout, null) }
                }
            )
        },
        floatingActionButton = {
            if (role == "admin") {
                FloatingActionButton(onClick = onNewClass) { Icon(Icons.Default.Add, null) }
            }
        }
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
                    AssistChip(onClick = {}, label = { Text(role!!.replaceFirstChar { it.uppercase() }) })
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ElevatedButton(onClick = onOpenResources, modifier = Modifier.weight(1f)) { Text("Recursos") }
                ElevatedButton(onClick = { if (uid.isNotBlank()) onOpenProgress(uid) }, modifier = Modifier.weight(1f)) {
                    Text("Progreso")
                }
            }

            HorizontalDivider()
            Text("Clases", style = MaterialTheme.typography.titleMedium)

            when {
                loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                error != null -> Text(error!!, color = MaterialTheme.colorScheme.error)
                classes.isEmpty() -> Text("No hay clases para tu rol.")
                else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(classes.size) { i ->
                        val (id, c) = classes[i]
                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth().clickable { onOpenClass(id) }
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                if (c.imageUrl.isNotBlank()) {
                                    val ctx = androidx.compose.ui.platform.LocalContext.current
                                    AsyncImage(
                                        model = ImageRequest.Builder(ctx).data(c.imageUrl).crossfade(true).build(),
                                        contentDescription = "Portada ${c.title}",
                                        modifier = Modifier.fillMaxWidth().height(140.dp),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(Modifier.height(8.dp))
                                }
                                Text(
                                    c.title.ifBlank { "Clase: $id" },
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (c.description.isNotBlank()) {
                                    Text(c.description, maxLines = 2, overflow = TextOverflow.Ellipsis)
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
