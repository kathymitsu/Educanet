package com.example.educanet.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
    var loading by remember { mutableStateOf(true) }
    var processing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

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
                        // 👇 tomamos primero classTitle, si no viene, usamos title
                        classTitle = d.getString("classTitle")
                            ?: d.getString("title")
                            ?: "Clase sin título",
                        price = d.getDouble("price") ?: 0.0,
                        imageUrl = d.getString("imageUrl") ?: "",
                        createdAt = d.getTimestamp("createdAt")
                    )
                } ?: emptyList()

                loading = false
                error = null
            }
    }

    val totalPrice = remember(items) {
        items.sumOf { it.price }
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
            modifier = Modifier
                .padding(pad)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when {
                loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator() }
                }

                error != null -> {
                    Text("Error: $error", color = MaterialTheme.colorScheme.error)
                }

                items.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Tu carrito está vacío.")
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(items, key = { it.id }) { item ->
                            ElevatedCard(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(12.dp)) {
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
                                    checkoutCart(db, uid, items)
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
                        enabled = !processing
                    ) {
                        Text(if (processing) "Procesando..." else "Confirmar inscripción")
                    }
                }
            }
        }
    }
}

/**
 * Procesa el carrito:
 * - Descuenta cupos en la clase
 * - Agrega el alumno a assignedStudents
 * - Registra la clase en /users/{uid}/myClasses
 * - Vacía el carrito
 */
suspend fun checkoutCart(
    db: FirebaseFirestore,
    uid: String,
    items: List<CartItem>
) {
    if (uid.isBlank() || items.isEmpty()) return

    val userDoc = db.collection("users").document(uid)
    val cartRef = userDoc.collection("cart")
    val myClassesRef = userDoc.collection("myClasses")

    for (item in items) {
        if (item.classId.isBlank()) continue

        val classRef = db.collection("classes").document(item.classId)
        val snap = classRef.get().await()
        if (!snap.exists()) continue

        val seats = snap.getLong("availableSeats")?.toInt()
        val description = snap.getString("description") ?: ""
        val imageUrl = snap.getString("imageUrl") ?: item.imageUrl
        val title = item.classTitle

        // Descontar cupos y asignar alumno
        if (seats != null) {
            if (seats <= 0) {
                // sin cupos → solo seguimos con la siguiente
                continue
            }
            classRef.update(
                mapOf(
                    "availableSeats" to seats - 1,
                    "assignedStudents" to FieldValue.arrayUnion(uid)
                )
            ).await()
        } else {
            classRef.update(
                "assignedStudents",
                FieldValue.arrayUnion(uid)
            ).await()
        }

        // Registrar en /users/{uid}/myClasses/{classId}
        val myData = mapOf(
            "classId" to item.classId,
            "title" to title,
            "description" to description,
            "imageUrl" to imageUrl,
            "price" to item.price,
            "createdAt" to Timestamp.now()
        )

        myClassesRef.document(item.classId)
            .set(myData, SetOptions.merge())
            .await()
    }

    // Vaciar carrito
    val cartSnap = cartRef.get().await()
    cartSnap.documents.forEach { d ->
        d.reference.delete()
    }
}
