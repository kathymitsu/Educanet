package com.example.educanet.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.educanet.item.ProgressItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressScreen(onBack: () -> Unit, studentId: String?) {
    val auth = remember { FirebaseAuth.getInstance() }
    val db = remember { FirebaseFirestore.getInstance() }
    val uid = auth.currentUser?.uid ?: return

    var items by remember { mutableStateOf(listOf<ProgressItem>()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val reinicioFecha = remember{ SimpleDateFormat("dd 'de' MMMM 'del' yyyy", Locale("es", "CL")) }
    // escucha progreso
    LaunchedEffect(uid) {
        db.collection("progress")
            .whereEqualTo("userId", uid)
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, e ->
                if (e != null){
                    error = e.message
                    loading = false
                    return@addSnapshotListener
                }
                items = snap?.toObjects(ProgressItem::class.java) ?: emptyList()
                loading = false
            }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Mi progreso") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }

            )
        }
    ) { pad ->
        Column(Modifier
            .padding(pad)
            .padding(16.dp)) {
            when {
                loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                error != null -> Text("Error: $error", color = MaterialTheme.colorScheme.error)
                items.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center){
                        Text("No hay progreso para mostrar")
                    }
                }
                    else -> {
                    LazyColumn(
                            contentPadding = PaddingValues(vertical = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                        items(items) { item ->
                            ProgressCard(item = item, reinicioFecha = reinicioFecha)
                        }
                    }
                }
            }
        }
    }
}
@Composable
private fun ProgressCard(item: ProgressItem, reinicioFecha: SimpleDateFormat) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Completado",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    text = item.classTitle.ifBlank { "Clase sin título" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))

                // --> 12. Muestra la fecha formateada si existe.
                val formattedDate = item.updatedAt?.toDate()?.let {
                    reinicioFecha.format(it)
                } ?: "Fecha desconocida"
                Text(
                    text = "${item.status.replaceFirstChar { it.uppercase() }} • $formattedDate",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
