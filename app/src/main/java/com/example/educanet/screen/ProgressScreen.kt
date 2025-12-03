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
fun ProgressScreen(
    onBack: () -> Unit,
    studentId: String? // si viene null, se usa el usuario logeado
) {
    val auth = remember { FirebaseAuth.getInstance() }
    val db = remember { FirebaseFirestore.getInstance() }

    val currentUid = auth.currentUser?.uid
    val effectiveUserId = studentId ?: currentUid

    var items by remember { mutableStateOf(listOf<ProgressItem>()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    val dateFormat = remember {
        SimpleDateFormat("dd 'de' MMMM 'del' yyyy", Locale("es", "CL"))
    }

    LaunchedEffect(effectiveUserId) {
        if (effectiveUserId == null) {
            error = "No se encontró el usuario para cargar el progreso."
            loading = false
            return@LaunchedEffect
        }

        db.collection("progress")
            .whereEqualTo("userId", effectiveUserId)
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, e ->
                if (e != null) {
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
                title = {
                    Text(
                        if (studentId == null) "Mi progreso"
                        else "Progreso del alumno"
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
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
                        "Error: $error",
                        color = MaterialTheme.colorScheme.error
                    )
                }

                items.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No hay progreso para mostrar.")
                    }
                }

                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(items) { item ->
                            ProgressCard(item = item, dateFormat = dateFormat)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProgressCard(
    item: ProgressItem,
    dateFormat: SimpleDateFormat
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = "Estado",
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

                // Nota si existe (puede ser Int o Double según tu data class)
                if (item.score != null) {
                    Text(
                        text = "Nota: ${item.score}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(2.dp))
                }

                val formattedDate = item.updatedAt?.toDate()?.let {
                    dateFormat.format(it)
                } ?: "Fecha desconocida"

                val statusPretty = item.status.replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                }

                Text(
                    text = "$statusPretty • $formattedDate",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
