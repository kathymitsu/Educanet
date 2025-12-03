package com.example.educanet.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.educanet.ui.ui.StorageImage
import androidx.compose.ui.layout.ContentScale
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.launch

data class MyClassItem(
    val classId: String = "",
    val title: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val price: Double = 0.0
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyClassesScreen(
    onBack: () -> Unit,
    onOpenClass: (String) -> Unit
) {
    val auth = remember { FirebaseAuth.getInstance() }
    val db = remember { FirebaseFirestore.getInstance() }
    val uid = auth.currentUser?.uid.orEmpty()

    val scope = rememberCoroutineScope()
    val snack = remember { SnackbarHostState() }

    var loading by remember { mutableStateOf(true) }
    var items by remember { mutableStateOf(listOf<MyClassItem>()) }
    var error by remember { mutableStateOf<String?>(null) }

    // Escuchar mis clases compradas
    LaunchedEffect(uid) {
        if (uid.isBlank()) {
            loading = false
            error = "No hay usuario autenticado."
            return@LaunchedEffect
        }

        db.collection("users")
            .document(uid)
            .collection("myClasses")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    loading = false
                    error = err.message
                    scope.launch { snack.showSnackbar("Error al cargar mis clases: ${err.message}") }
                    return@addSnapshotListener
                }

                items = snap?.documents?.map { d ->
                    MyClassItem(
                        classId = d.getString("classId") ?: d.id,
                        title = d.getString("title") ?: "",
                        description = d.getString("description") ?: "",
                        imageUrl = d.getString("imageUrl") ?: "",
                        price = d.getDouble("price") ?: 0.0
                    )
                } ?: emptyList()

                loading = false
                error = null
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis clases") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
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
                .fillMaxSize()
        ) {
            when {
                loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                error != null -> {
                    Text(
                        text = "Error: $error",
                        color = MaterialTheme.colorScheme.error
                    )
                }

                items.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Aún no tienes clases pagadas.")
                    }
                }

                else -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(items.size) { i ->
                            val item = items[i]
                            ElevatedCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onOpenClass(item.classId) }
                            ) {
                                Column(Modifier.padding(12.dp)) {
                                    if (item.imageUrl.isNotBlank()) {
                                        StorageImage(
                                            url = item.imageUrl,
                                            contentDescription = "Portada",
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(140.dp),
                                            contentScale = ContentScale.Crop
                                        )
                                        Spacer(Modifier.height(8.dp))
                                    }

                                    Text(
                                        text = item.title.ifBlank { "Clase sin título" },
                                        style = MaterialTheme.typography.titleMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (item.description.isNotBlank()) {
                                        Text(
                                            text = item.description,
                                            style = MaterialTheme.typography.bodyMedium,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = "Precio pagado: $${"%.0f".format(item.price)}",
                                        style = MaterialTheme.typography.bodySmall
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
