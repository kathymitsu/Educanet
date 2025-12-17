package com.example.educanet.screen

import android.Manifest
import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.educanet.ui.ui.StorageImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.launch
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.ui.platform.LocalContext
import com.example.educanet.data.UserPrefs
import com.example.educanet.item.ClassItem
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.firebase.Timestamp
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun HomeScreen(
    onLogout: () -> Unit,
    onNewClass: () -> Unit,
    onCreateProfessor: () -> Unit, // <--- Nueva función
    onOpenClass: (String) -> Unit,
    onOpenResources: () -> Unit,
    onOpenProgress: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenCart: () -> Unit,
    onOpenMyClasses: () -> Unit = {},
    onOpenNotifications: () -> Unit
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

    val isStudent = role == "estudiante"
    val isProfessor = role == "profesor"
    val isAdmin = role == "admin" // <--- Variable para admin

    var loading by remember { mutableStateOf(true) }
    var classes by remember { mutableStateOf(listOf<Pair<String, ClassItem>>()) }
    var errorText by remember { mutableStateOf<String?>(null) }

    // Solicitar permiso de notificaciones en Android 13+
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val permissionState = rememberPermissionState(permission = Manifest.permission.POST_NOTIFICATIONS)
        LaunchedEffect(Unit) {
            if (!permissionState.status.isGranted) {
                permissionState.launchPermissionRequest()
            }
        }
    }

    // 🔔 Suscripción básica a topic
    LaunchedEffect(Unit) {
        runCatching { FirebaseMessaging.getInstance().subscribeToTopic("classes").await() }
    }

    // 👤 Si no hay rol/nombre en DataStore, los trae de Firestore y los guarda local
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

    // 📚 Cargar clases según rol (y escuchar cambios)
    LaunchedEffect(role, uid) {
        if (role == null || uid.isBlank()) return@LaunchedEffect
        loading = true
        errorText = null

        val base = db.collection("classes")

        val attachListener: (com.google.firebase.firestore.Query) -> Unit = { q ->
            q.addSnapshotListener { snap, err ->
                if (err != null) {
                    loading = false
                    classes = emptyList()
                    errorText = when ((err as? FirebaseFirestoreException)?.code) {
                        FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                            "PERMISSION_DENIED: Revisa reglas de Firestore para /classes."
                        else -> "Error cargando clases: ${err.message}"
                    }
                    return@addSnapshotListener
                }

                val list = snap?.documents?.map { d ->
                    d.id to (d.toObject(ClassItem::class.java) ?: ClassItem())
                } ?: emptyList()

                classes = list.sortedByDescending { it.second.createdAt?.toDate()?.time ?: 0L }
                loading = false
                errorText = null
            }
        }

        when (role) {
            "admin" -> attachListener(base)
            "profesor" -> attachListener(base.whereEqualTo("professorId", uid))
            else -> attachListener(base.whereEqualTo("isActive", true))
        }
    }

    // 🧭 UI
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Educanet") },
                actions = {
                    IconButton(onClick = onOpenNotifications) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notificaciones"
                        )
                    }
                    if (isStudent) {
                        IconButton(onClick = onOpenCart) {
                            Icon(
                                imageVector = Icons.Default.ShoppingCart,
                                contentDescription = "Carrito"
                            )
                        }
                    }
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
            // FAB de crear clase para profesor o admin
            if (isProfessor || isAdmin) {
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
                role?.let {
                    AssistChip(onClick = {}, label = { Text(it.replaceFirstChar { c -> c.uppercase() }) })
                }
            }

            // Botón para crear profesor (solo admin)
            if (isAdmin) {
                ElevatedButton(
                    onClick = onCreateProfessor,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Crear Profesor") }
            }

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

            // si quieres mostrar "Mis clases" solo al alumno:
            if (isStudent) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ElevatedButton(
                        onClick = onOpenMyClasses,
                        modifier = Modifier.weight(1f)
                    ) { Text("Mis clases") }
                }
            }

            HorizontalDivider()
            Text(
                if (isStudent) "Clases disponibles"
                else if (isProfessor) "Mis Cursos"
                else "Clases",
                style = MaterialTheme.typography.titleMedium
            )

            AnimatedVisibility(visible = loading, enter = fadeIn(), exit = fadeOut()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            Crossfade(targetState = Pair(loading, classes)) { state ->
                val (isLoading, list) = state
                if (!isLoading) {

                    // alumno: solo activas con cupos y que no esté inscrito; otros roles: la lista tal cual
                    val visibleList =
                        if (isStudent) list.filter {
                            (it.second.availableSeats ?: 0L) > 0 &&
                                    it.second.isActive &&
                                    !it.second.assignedStudents.contains(uid)
                        }
                        else list

                    when {
                        errorText != null ->
                            Text(errorText!!, color = MaterialTheme.colorScheme.error)

                        visibleList.isEmpty() ->
                            Text(
                                if (isStudent) "No hay clases con cupos disponibles."
                                else if (isProfessor) "Aún no has creado ningún curso."
                                else "No hay clases para tu rol."
                            )

                        else -> {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                items(visibleList.size) { i ->
                                    val (id, c) = visibleList[i]
                                    ElevatedCard(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onOpenClass(id) }
                                    ) {
                                        Column(Modifier.padding(12.dp)) {
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
                                                    style = MaterialTheme.typography.bodySmall,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            
                                            Spacer(Modifier.height(8.dp))
                                            
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                val format = NumberFormat.getCurrencyInstance(Locale("es", "MX"))
                                                Text(
                                                    text = format.format(c.price),
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    "Cupos disponibles: ${c.availableSeats ?: 0}",
                                                    style = MaterialTheme.typography.bodyMedium
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
