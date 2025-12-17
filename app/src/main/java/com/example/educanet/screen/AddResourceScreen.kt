package com.example.educanet.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.educanet.item.ResourceItem
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddResourceScreen(
    onBack: () -> Unit
) {
    val db = remember { FirebaseFirestore.getInstance() }
    val scope = rememberCoroutineScope()
    val snack = remember { SnackbarHostState() }

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }

    val addResource: () -> Unit = {
        scope.launch {
            loading = true
            try {
                val newResource = ResourceItem(
                    title = title,
                    description = description,
                    url = url,
                    type = type.lowercase(),
                    createdAt = Timestamp.now()
                )
                db.collection("resources").add(newResource).await()
                onBack()
            } catch (e: Exception) {
                snack.showSnackbar("Error al guardar: ${e.message}")
            } finally {
                loading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nuevo Recurso") },
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
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Título") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Descripción") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text("URL del recurso") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = type,
                onValueChange = { type = it },
                label = { Text("Tipo (video, pdf, link)") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = addResource,
                enabled = !loading && title.isNotBlank() && url.isNotBlank() && type.isNotBlank()
            ) {
                if (loading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else {
                    Text("Guardar Recurso")
                }
            }
        }
    }
}