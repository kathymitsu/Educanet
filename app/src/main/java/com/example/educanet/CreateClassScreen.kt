package com.example.educanet

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateClassScreen(onDone: () -> Unit) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var link by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun save() {
        error = null
        if (title.isBlank()) { error = "Título obligatorio"; return }
        loading = true
        val uid = auth.currentUser?.uid ?: return
        val data = mapOf(
            "title" to title.trim(),
            "description" to desc.trim(),
            "videoLink" to link.trim(),
            "createdBy" to uid,
            "createdAt" to FieldValue.serverTimestamp()
        )
        db.collection("classes").add(data)
            .addOnSuccessListener { onDone() }
            .addOnFailureListener { e -> error = e.message }
            .addOnCompleteListener { loading = false }
    }

    Scaffold(topBar = { CenterAlignedTopAppBar(title = { Text("Nueva clase") }) }) { pad ->
        Column(Modifier.padding(pad).padding(16.dp)) {
            OutlinedTextField(
                value = title, onValueChange = { title = it },
                label = { Text("Título") }, modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = desc, onValueChange = { desc = it },
                label = { Text("Descripción") }, modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = link, onValueChange = { link = it },
                label = { Text("Link Meet/YouTube (opcional)") }, modifier = Modifier.fillMaxWidth()
            )
            if (error != null) { Spacer(Modifier.height(6.dp)); Text(error!!, color = MaterialTheme.colorScheme.error) }
            Spacer(Modifier.height(12.dp))
            Button(onClick = { save() }, enabled = !loading, modifier = Modifier.fillMaxWidth()) {
                Text(if (loading) "Guardando..." else "Guardar")
            }
        }
    }
}

