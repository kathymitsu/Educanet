package com.example.educanet.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.example.educanet.data.UserPrefs
import com.example.educanet.item.ClassItem
import com.example.educanet.ui.ui.StorageImage
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onLogout: () -> Unit,
    onNewClass: () -> Unit,
    onOpenClass: (String) -> Unit,
    onOpenResources: () -> Unit,
    onOpenProgress: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenCart: () -> Unit
) {
    val ctx = LocalContext.current
    val db = remember { FirebaseFirestore.getInstance() }
    val auth = remember { FirebaseAuth.getInstance() }
    val uid = auth.currentUser?.uid.orEmpty()

    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Perfil desde DataStore
    val profile by UserPrefs.profileFlow(ctx).collectAsState(initial = UserPrefs.Profile())
    var name by remember { mutableStateOf(profile.name) }
    var role by remember { mutableStateOf(profile.role.ifBlank { null }) }

    var loading by remember { mutableStateOf(true) }
    var classes by remember { mutableStateOf(listOf<Pair<String, ClassItem>>()) }
    var errorText by remember { mutableStateOf<String?>(null) }

    // Suscripción básica a topic de clases
    LaunchedEffect(Unit) {
        runCatching { FirebaseMessaging.getInstance().subscribeToTopic("classes").await() }
    }

    // Si no hay nombre/rol en DataStore → los trae de Firestore y los guarda
    LaunchedEffect(uid) {
        if (uid.isBlank()) return@LaunchedEffect
        if (profile.role.isBlank() || profile.name.isBlank()) {
            runCatching {
                val snap = db.collection("users").document(uid).get().await()
                val nm = snap.getString("name") ?: (snap.getString("email") ?: "")
                val rl = snap.getString("role") ?: "estudiante"
                name = nm
                role = rl
                UserPrefs.saveProfile(ctx, nm, rl)
            }
        } else {
            name = profile.name
            role = profile.role
        }
    }

    // Cargar clases según rol
    LaunchedEffect(role, uid) {
        if (role == null || uid.isBlank()) return@LaunchedEffect
        loading = true
        errorText = null

        val base = db.collection("classes")

        fun attachListener(q: com.google.firebase.firestore.Query) {
            q.addSnapshotListener { snap, err ->
                if (err != null) {
                    loading = false
                    classes = emptyList()
                    errorText = when ((err as? FirebaseFirestoreException)?.code) {
                        FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                            "PERMISSION_DENIED: revisa reglas de Firestore para /classes."
                        else -> "Error cargando clases: ${err.message}"
                    }
                    return@addSnapshotListener
                }

                val list = snap?.documents?.map { d ->
                    // Usamos el data class ClassItem global
                    val ci = d.toObject(ClassItem::class.java) ?: ClassItem()
                    d.id to ci.copy(id = d.id)
                } ?: emptyList()

                classes = list.sortedByDescending {
                    it.second.createdAt?.toDate()?.time ?: 0L
                }
                loading = false
                errorText = null
            }
        }

        when (role) {
            "admin" -> attachListener(base)
            "profesor" -> attachListener(base.whereEqualTo("professorId", uid))
            else -> attachListener(base.whereArrayContains("assignedStudents", uid))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Educanet") },
                actions = {
                    IconButton(onClick = onOpenCart) {
                        Icon(Icons.Filled.ShoppingCart, contentDescription = "Carrito")
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Ajustes")
                    }
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Filled.Logout, contentDescription = "Salir")
                    }
                }
            )
        },
        floatingActionButton = {
            if (role == "admin") {
                FloatingActionButton(onClick = onNewClass) {
                    Icon(Icons.Filled.Add, contentDescription = "Nueva clase")
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
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (name.isBlank()) "Bienvenido/a" else "Hola, $name",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f)
                )
                role?.let {
                    AssistChip(
                        onClick = {},
                        label = { Text(it.replaceFirstChar { c -> c.uppercase() }) }
                    )
                }
            }

            // Botones rápidos
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                ElevatedButton(
                    onClick = onOpenResources,
                    modifier = Modifier.weight(1f)
                ) { Text("Recursos") }

                ElevatedButton(
                    onClick = { if (uid.isNotBlank()) onOpenProgress(uid) },
                    modifier = Modifier.weight(1f)
                ) { Text("Progreso") }
            }

            Divider()
            Text("Clases", style = MaterialTheme.typography.titleMedium)

            AnimatedVisibility(
                visible = loading,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            Crossfade(targetState = Pair(loading, classes)) { state ->
                val (isLoading, list) = state
                if (!isLoading) {
                    when {
                        errorText != null -> Text(
                            errorText!!,
                            color = MaterialTheme.colorScheme.error
                        )

                        list.isEmpty() -> Text("No hay clases para tu rol.")

                        else -> {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(list.size) { i ->
                                    val (id, c) = list[i]
                                    ElevatedCard(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onOpenClass(id) }
                                    ) {
                                        Column(Modifier.padding(12.dp)) {
                                            // Portada si viene imageUrl
                                            if (c.imageUrl.isNotBlank()) {
                                                StorageImage(
                                                    url = c.imageUrl,
                                                    contentDescription = "Portada",
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(160.dp),
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
                                                Text(
                                                    c.description,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis,
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                            }

                                            Spacer(Modifier.height(4.dp))

                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Text(
                                                    text = "$${"%.0f".format(c.price)}",
                                                    style = MaterialTheme.typography.bodyMedium
                                                )

                                                val seats = c.availableSeats?.toInt()
                                                Text(
                                                    text = when {
                                                        seats == null -> "Cupos: ∞"
                                                        seats <= 0 -> "Sin cupos"
                                                        else -> "Cupos: $seats"
                                                    },
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                            }

                                            if (!c.isActive) {
                                                Spacer(Modifier.height(4.dp))
                                                AssistChip(
                                                    onClick = {},
                                                    label = { Text("Inactiva") }
                                                )
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
    }
}
