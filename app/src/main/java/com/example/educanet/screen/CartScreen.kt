package com.example.educanet.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class CartItem(
    val id: String = "",
    val classId: String = "",
    val classTitle: String = "",
    val price: Double = 0.0,
    val imageUrl: String = "",
    val createdAt: Timestamp? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(
    onBack: () -> Unit,
    onCheckoutSuccess: () -> Unit
) {
    val auth = remember { FirebaseAuth.getInstance() }
    val db = remember { FirebaseFirestore.getInstance() }
    val uid = auth.currentUser?.uid.orEmpty()

    val scope = rememberCoroutineScope()
    val snack = remember { SnackbarHostState() }

    var items by remember { mutableStateOf<List<CartItem>>(emptyList()) }
    var enrolledClassIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var loading by remember { mutableStateOf(true) }
    var processing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    // Carga los items del carrito
    LaunchedEffect(uid) {
        if (uid.isBlank()) {
            error = "Usuario no autenticado."
            loading = false
            return@LaunchedEffect
        }

        db.collection("users").document(uid)
            .collection("cart")
            .addSnapshotListener { snap, e ->
                if (e != null) {
                    error = e.message
                    loading = false
                    return@addSnapshotListener
                }

                items = snap?.documents?.map { d ->
                    CartItem(
                        id = d.id,
                        classId = d.getString("classId") ?: "",
                        classTitle = d.getString("classTitle") ?: d.getString("title") ?: "Clase sin título",
                        price = d.getDouble("price") ?: 0.0,
                        imageUrl = d.getString("imageUrl") ?: "",
                        createdAt = d.getTimestamp("createdAt")
                    )
                } ?: emptyList()

                loading = false
                error = null
            }
    }

    // Carga las clases en las que el usuario ya está inscrito
    LaunchedEffect(uid) {
        if (uid.isBlank()) return@LaunchedEffect
        db.collection("users").document(uid).collection("myClasses")
            .addSnapshotListener { snap, e ->
                if (e != null) {
                    scope.launch { snack.showSnackbar("Error al cargar tus clases: ${e.message}") }
                    return@addSnapshotListener
                }
                enrolledClassIds = snap?.documents?.map { it.id }?.toSet() ?: emptySet()
            }
    }

    // Filtra el carrito para no mostrar clases ya inscritas
    val filteredItems = remember(items, enrolledClassIds) {
        items.filter { it.classId !in enrolledClassIds }
    }

    val totalPrice = remember(filteredItems) {
        filteredItems.sumOf { it.price }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Carrito de clases") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snack) }
    ) { pad ->
        Column(
            modifier = Modifier.padding(pad).padding(16.dp).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when {
                loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                error != null -> {
                    Text("Error: $error", color = MaterialTheme.colorScheme.error)
                }

                filteredItems.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Tu carrito está vacío o ya estás inscrito en estas clases.")
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredItems, key = { it.id }) { item ->
                            ElevatedCard(Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(Modifier.weight(1f).padding(end = 8.dp)) {
                                        Text(
                                            item.classTitle,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            "Precio: $${"%.0f".format(item.price)}",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            if (processing) return@IconButton
                                            scope.launch {
                                                try {
                                                    db.collection("users").document(uid)
                                                        .collection("cart").document(item.id)
                                                        .delete().await()
                                                    snack.showSnackbar("Clase eliminada.")
                                                } catch (e: Exception) {
                                                    snack.showSnackbar("Error: ${e.message}")
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Filled.Delete, contentDescription = "Eliminar")
                                    }
                                }
                            }
                        }
                    }

                    Divider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Total:", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "$${"%.0f".format(totalPrice)}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    Button(
                        onClick = {
                            if (processing) return@Button
                            scope.launch {
                                processing = true
                                try {
                                    checkoutCart(db, uid, filteredItems)
                                    snack.showSnackbar("Inscripción confirmada.")
                                    onCheckoutSuccess()
                                } catch (e: Exception) {
                                    snack.showSnackbar("Error al procesar: ${e.message}")
                                } finally {
                                    processing = false
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !processing && filteredItems.isNotEmpty()
                    ) {
                        Text(if (processing) "Procesando..." else "Confirmar inscripción")
                    }
                }
            }
        }
    }
}

/**
 * Procesa el carrito de forma transaccional y segura:
 * - Descuenta cupos en la clase y asigna al alumno atómicamente.
 * - Registra la clase en /users/{uid}/myClasses.
 * - Vacía los items procesados del carrito.
 */
suspend fun checkoutCart(
    db: FirebaseFirestore,
    uid: String,
    items: List<CartItem>
) {
    if (uid.isBlank() || items.isEmpty()) return

    val userDoc = db.collection("users").document(uid)
    val myClassesRef = userDoc.collection("myClasses")

    // Procesar cada inscripción en una transacción para asegurar consistencia
    for (item in items) {
        if (item.classId.isBlank()) continue
        val classRef = db.collection("classes").document(item.classId)

        db.runTransaction { transaction ->
            val classSnap = transaction.get(classRef)

            // Salvaguarda: no procesar si el usuario ya está inscrito
            val assignedStudents = classSnap.get("assignedStudents") as? List<*> ?: emptyList<Any>()
            if (uid in assignedStudents) {
                return@runTransaction
            }

            // Verificar y descontar cupo atómicamente
            val seats = classSnap.getLong("availableSeats")
            if (seats != null) {
                if (seats <= 0) {
                    // Sin cupos, no se puede inscribir
                    return@runTransaction
                }
                transaction.update(classRef, "availableSeats", FieldValue.increment(-1))
            }

            // Asignar alumno
            transaction.update(classRef, "assignedStudents", FieldValue.arrayUnion(uid))

            // Registrar en /users/{uid}/myClasses/{classId}
            val myData = mapOf(
                "classId" to item.classId,
                "title" to item.classTitle,
                "description" to (classSnap.getString("description") ?: ""),
                "imageUrl" to (classSnap.getString("imageUrl") ?: item.imageUrl),
                "price" to item.price,
                "createdAt" to Timestamp.now()
            )
            val myClassDoc = myClassesRef.document(item.classId)
            transaction.set(myClassDoc, myData, SetOptions.merge())

        }.await()
    }

    // Vaciar solo los items procesados del carrito usando un batch write
    val cartRef = userDoc.collection("cart")
    db.runBatch { batch ->
        items.forEach { item ->
            batch.delete(cartRef.document(item.id))
        }
    }.await()
}
