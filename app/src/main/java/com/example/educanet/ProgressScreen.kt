package com.example.educanet

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressScreen(onBack: () -> Unit) {
    val auth = remember { FirebaseAuth.getInstance() }
    val db = remember { FirebaseFirestore.getInstance() }
    val uid = auth.currentUser?.uid ?: return

    var items by remember { mutableStateOf(listOf<Pair<String, ProgressItem>>()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    // escucha progreso
    LaunchedEffect(true) {
        db.collection("progress").document(uid).collection("items")
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, e ->
                if (e != null) { error = e.message; loading = false; return@addSnapshotListener }
                items = snap?.documents?.mapNotNull { d ->
                    d.toObject(ProgressItem::class.java)?.let { d.id to it }
                } ?: emptyList()
                loading = false
            }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Mi progreso") },
                navigationIcon = { TextButton(onClick = onBack) { Text("←") } }
            )
        }
    ) { pad ->
        Column(Modifier.padding(pad).padding(16.dp)) {
            when {
                loading -> CircularProgressIndicator()
                error != null -> Text("Error: $error", color = MaterialTheme.colorScheme.error)
                items.isEmpty() -> Text("Aún no marcas clases como completadas.")
                else -> {
                    LazyColumn {
                        items(items.size) { i ->
                            val (_, it) = items[i]
                            ElevatedCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                            ) {
                                Column(Modifier.padding(12.dp)) {
                                    Text(
                                        "Clase: ${it.classTitle ?: it.classId}",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text("Estado: ${it.status}")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
