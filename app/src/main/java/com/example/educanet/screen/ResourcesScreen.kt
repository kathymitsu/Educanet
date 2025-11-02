package com.example.educanet.screen

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.educanet.item.ResourceItem
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResourcesScreen(onBack: () -> Unit) {
    val db = remember { FirebaseFirestore.getInstance() }
    val auth = remember { FirebaseAuth.getInstance() }
    val uid = auth.currentUser?.uid

    var role by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var resources by remember { mutableStateOf(listOf<Pair<String, ResourceItem>>()) }

    var showDialog by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("article") }
    var url by remember { mutableStateOf("") }

    // rol
    LaunchedEffect(Unit) {
        uid ?: return@LaunchedEffect
        db.collection("users").document(uid).get()
            .addOnSuccessListener { role = it.getString("role") }
            .addOnFailureListener { e -> error = e.message }
    }

    // listener
    LaunchedEffect(true) {
        db.collection("resources").orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, e ->
                if (e != null) { error = e.message; loading = false; return@addSnapshotListener }
                resources = snap?.documents?.mapNotNull { d ->
                    d.toObject(ResourceItem::class.java)?.let { d.id to it }
                } ?: emptyList()
                loading = false
            }
    }

    val ctx = LocalContext.current
    fun openUrl(link: String) = ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link)))

    fun saveResource() {
        val data = hashMapOf(
            "title" to title.trim(),
            "type" to type.trim(),
            "url" to url.trim(),
            "createdBy" to (uid ?: ""),
            "createdAt" to Timestamp.now()
        )
        db.collection("resources").add(data)
        showDialog = false; title = ""; type = "article"; url = ""
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Recursos") },
                navigationIcon = { IconButton(onClick = onBack) { Text("←") } }
            )
        },
        floatingActionButton = {
            if (role == "profesor") FloatingActionButton(onClick = { showDialog = true }) { Text("+") }
        }
    ) { pad ->
        Column(Modifier.padding(pad).padding(16.dp)) {
            when {
                loading -> CircularProgressIndicator()
                error != null -> Text("Error: $error", color = MaterialTheme.colorScheme.error)
                resources.isEmpty() -> Text("No hay recursos aún")
                else -> {
                    LazyColumn {
                        items(resources.size) { i ->
                            val (id, r) = resources[i]
                            ElevatedCard(
                                onClick = { openUrl(r.url) },
                                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
                            ) {
                                Column(Modifier.padding(12.dp)) {
                                    Text(r.title, style = MaterialTheme.typography.titleMedium)
                                    Spacer(Modifier.height(4.dp))
                                    Text("${r.type.uppercase()} • ${r.url}")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Nuevo recurso") },
            text = {
                Column {
                    OutlinedTextField(title, { title = it }, label = { Text("Título") }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(type, { type = it }, label = { Text("Tipo (book/article/video)") }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(url, { url = it }, label = { Text("URL") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                TextButton(onClick = { saveResource() }, enabled = title.isNotBlank() && url.isNotBlank()) { Text("Guardar") }
            },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Cancelar") } }
        )
    }
}
