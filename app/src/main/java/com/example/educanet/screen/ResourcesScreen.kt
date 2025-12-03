package com.example.educanet.screen

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.educanet.item.ResourceItem
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResourcesScreen(
    onBack: () -> Unit
) {
    val db = remember { FirebaseFirestore.getInstance() }
    val scope = rememberCoroutineScope()
    val snack = remember { SnackbarHostState() }

    var items by remember { mutableStateOf<List<ResourceItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    // Cargar recursos (para cualquier usuario autenticado)
    LaunchedEffect(Unit) {
        db.collection("resources")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, e ->
                if (e != null) {
                    error = e.message
                    loading = false
                    return@addSnapshotListener
                }

                items = snap?.documents?.map { d ->
                    val r = d.toObject(ResourceItem::class.java) ?: ResourceItem()
                    r.copy(id = d.id)
                } ?: emptyList()

                error = null
                loading = false
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recursos") },
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
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Aún no hay recursos disponibles.")
                    }
                }

                else -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(items, key = { it.id }) { res ->
                            ResourceCard(res) { url ->
                                // si quieres abrir el link con un Intent:
                                // val ctx = LocalContext.current
                                // val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                // ctx.startActivity(intent)
                                scope.launch {
                                    snack.showSnackbar("Abrir: $url")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ResourceCard(
    item: ResourceItem,
    onClick: (String) -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = item.url.isNotBlank()) {
                onClick(item.url)
            }
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(
                text = item.title.ifBlank { "Recurso sin título" },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (item.description.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(4.dp))

            val label = when (item.type.lowercase()) {
                "video" -> "Video"
                "pdf"   -> "PDF"
                "link"  -> "Enlace"
                else    -> "Recurso"
            }

            AssistChip(
                onClick = { if (item.url.isNotBlank()) onClick(item.url) },
                label = { Text(label) }
            )
        }
    }
}
