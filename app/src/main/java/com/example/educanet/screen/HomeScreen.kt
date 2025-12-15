
package com.example.educanet.screen

import com.example.educanet.ui.ui.StorageImage
import androidx.compose.ui.layout.ContentScale
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
import com.google.firebase.Timestamp
import com.example.educanet.item.ClassItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onLogout: () -> Unit,
    onNewClass: () -> Unit,
    onOpenClass: (String) -> Unit,
    onOpenResources: () -> Unit,
    onOpenProgress: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenCart: () -> Unit,
    onOpenMyClasses: () -> Unit = {}
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

    var loading by remember { mutableStateOf(true) }
    var classes by remember { mutableStateOf(listOf<Pair<String, ClassItem>>()) }
    var errorText by remember { mutableStateOf<String?>(null) }

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
            "admin" -> attachListener(base)                       // ve todas
            "profesor" -> attachListener(base.whereEqualTo("isActive", true))
            else -> attachListener(base.whereEqualTo("isActive", true))
        }
    }

    // 🛒 función para que SOLO el alumno agregue al carro
    fun addToCart(classId: String, item: ClassItem) {
        if (!isStudent || uid.isBlank()) return

        val cartRef = db.collection("users")
            .document(uid)
            .collection("cart")
            .document(classId)

        db.runTransaction { tr ->
            val snap = tr.get(cartRef)
            if (snap.exists()) {
                val currentQty = (snap.getLong("quantity") ?: 1L).toInt()
                tr.update(cartRef, "quantity", currentQty + 1)
            } else {
                val data = mapOf(
                    "classId" to classId,
                    "classTitle" to item.title,
                    "price" to item.price,
                    "imageUrl" to item.imageUrl,
                    "quantity" to 1,
                    "createdAt" to Timestamp.now()
                )
                tr.set(cartRef, data)
            }
        }.addOnSuccessListener {
            scope.launch { snackbar.showSnackbar("Clase agregada al carrito") }
        }.addOnFailureListener { e ->
            scope.launch { snackbar.showSnackbar("Error al agregar al carrito: ${e.message}") }
        }
    }

    // 🧭 UI
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Educanet") },
                actions = {
                    if (isStudent) {
                        IconButton(onClick = onOpenCart) {
                            Icon(
                                imageVector = Icons.Default.Add, // cámbialo por ícono de carrito si quieres
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
            // FAB de crear clase SOLO para admin/profesor
            if (role == "admin" || role == "profesor") {
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
                if (isStudent) "Clases disponibles" else "Clases",
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
                                else "No hay clases para tu rol."
                            )

                        else -> {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                items(visibleList.size) { i ->
                                    val (id, c) = visibleList[i]
                                    ElevatedCard(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable(enabled = !isStudent) { onOpenClass(id) }
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
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis,
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                            }

                                            Spacer(Modifier.height(4.dp))
                                            Text(
                                                "Precio: $${"%.0f".format(c.price)}",
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                            Text(
                                                "Cupos disponibles: ${c.availableSeats ?: 0}",
                                                style = MaterialTheme.typography.bodySmall
                                            )

                                            // profesor puede ver qué UID está asignado
                                            if (isProfessor) {
                                                Spacer(Modifier.height(4.dp))
                                                Text(
                                                    "Profesor asignado: ${c.professorId.ifBlank { "Sin asignar" }}",
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                            }

                                            if (!c.isActive) {
                                                Spacer(Modifier.height(4.dp))
                                                AssistChip(onClick = {}, label = { Text("Inactiva") })
                                            }

                                            if (isStudent) {
                                                Spacer(Modifier.height(8.dp))
                                                Button(
                                                    onClick = { addToCart(id, c) },
                                                    enabled = (c.availableSeats ?: 0L) > 0
                                                ) {
                                                    Text(
                                                        if ((c.availableSeats ?: 0L) > 0)
                                                            "Agregar al carrito"
                                                        else
                                                            "Sin cupos"
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
}
