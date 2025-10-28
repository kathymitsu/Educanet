package com.example.educanet

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateClassScreen(
    onCancel: () -> Unit,
    onSaved: () -> Unit
) {
    val db = remember { FirebaseFirestore.getInstance() }
    val auth = remember { FirebaseAuth.getInstance() }

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var videoLink by remember { mutableStateOf("") }

    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun save() {
        val uid = auth.currentUser?.uid ?: run { error = "Sesión inválida"; return }
        if (title.isBlank()) { error = "El título es obligatorio"; return }

        saving = true; error = null

        val data = hashMapOf(
            "title" to title.trim(),
            "description" to description.trim(),
            "videoLink" to videoLink.trim(),
            "createdBy" to uid,
            "createdAt" to com.google.firebase.Timestamp.now()
        )

        db.collection("classes").add(data)
            .addOnSuccessListener { onSaved() }
            .addOnFailureListener { e -> error = e.message }
            .addOnCompleteListener { saving = false }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Nueva clase") },
                navigationIcon = { TextButton(onClick = onCancel) { Text("Cancelar") } }
            )
        }
    ) { pad ->
        Column(
            modifier = Modifier.padding(pad).padding(16.dp).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (error != null) Text("Error: $error", color = MaterialTheme.colorScheme.error)

            OutlinedTextField(title, { title = it }, label = { Text("Título") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(description, { description = it }, label = { Text("Descripción") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
            OutlinedTextField(videoLink, { videoLink = it }, label = { Text("Link de video (opcional)") }, modifier = Modifier.fillMaxWidth())

            Button(
                onClick = { save() },
                enabled = !saving && title.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (saving) "Guardando..." else "Guardar") }
        }
    }
}
