// app/src/main/java/com/example/educanet/CreateClassScreenAdmin.kt
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
import androidx.compose.ui.unit.dp
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateClassScreenAdmin(
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val db = remember { FirebaseFirestore.getInstance() }
    val auth = remember { FirebaseAuth.getInstance() }
    val myUid = auth.currentUser?.uid ?: ""

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var videoLink by remember { mutableStateOf("") }

    // dropdown de profesores
    var profs by remember { mutableStateOf(listOf<Pair<String,String>>()) } // uid to name
    var profMenu by remember { mutableStateOf(false) }
    var selectedProfessor by remember { mutableStateOf<String?>(null) }

    // checkboxes de alumnos
    var students by remember { mutableStateOf(listOf<Pair<String,String>>()) } // uid to name
    var selectedStudents by remember { mutableStateOf(setOf<String>()) }

    var loading by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val snack = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Cargar usuarios (profes y alumnos)
    LaunchedEffect(Unit) {
        loading = true
        db.collection("users").whereEqualTo("role", "profesor").get()
            .addOnSuccessListener { qs ->
                profs = qs.documents.map {
                    val id = it.id
                    val name = it.getString("name") ?: it.getString("email") ?: id
                    id to name
                }.sortedBy { it.second.lowercase() }
            }
            .addOnFailureListener { e -> error = e.message }

        db.collection("users").whereEqualTo("role", "estudiante").get()
            .addOnSuccessListener { qs ->
                students = qs.documents.map {
                    val id = it.id
                    val name = it.getString("name") ?: it.getString("email") ?: id
                    id to name
                }.sortedBy { it.second.lowercase() }
                loading = false
            }
            .addOnFailureListener { e ->
                error = e.message
                loading = false
            }
    }

    fun save() {
        error = null
        if (title.isBlank() || selectedProfessor == null || selectedStudents.isEmpty()) {
            error = "Título, profesor y al menos un estudiante son obligatorios."
            return
        }
        saving = true
        val data = mapOf(
            "title" to title.trim(),
            "description" to description.trim(),
            "videoLink" to videoLink.trim(),
            "professorId" to selectedProfessor!!,
            "assignedStudents" to selectedStudents.toList(),
            "createdBy" to myUid,
            "createdAt" to Timestamp.now(),
            "isActive" to true
        )
        db.collection("classes").add(data)
            .addOnSuccessListener {
                scope.launch { snack.showSnackbar("Clase creada") }
                onSaved()
            }
            .addOnFailureListener { e ->
                error = e.message
            }
            .addOnCompleteListener { saving = false }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nueva clase (Admin)") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null) }
                }
            )
        },
        snackbarHost = { SnackbarHost(snack) }
    ) { pad ->
        if (loading) {
            Box(Modifier.fillMaxSize().padding(pad), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .padding(pad)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = title, onValueChange = { title = it },
                label = { Text("Título") }, singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = description, onValueChange = { description = it },
                label = { Text("Descripción") }, modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = videoLink, onValueChange = { videoLink = it },
                label = { Text("Link video (opcional)") }, singleLine = true, modifier = Modifier.fillMaxWidth()
            )

            // selector profesor
            ExposedDropdownMenuBox(
                expanded = profMenu,
                onExpandedChange = { profMenu = !profMenu }
            ) {
                OutlinedTextField(
                    value = selectedProfessor?.let { uid -> profs.find { it.first == uid }?.second } ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Profesor asignado") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = profMenu) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = profMenu, onDismissRequest = { profMenu = false }) {
                    profs.forEach { (uid, name) ->
                        DropdownMenuItem(
                            text = { Text(name) },
                            onClick = { selectedProfessor = uid; profMenu = false }
                        )
                    }
                }
            }

            Text("Estudiantes asignados", style = MaterialTheme.typography.titleMedium)
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f, fill = false)
            ) {
                items(students, key = { it.first }) { (uid, name) ->
                    val checked = uid in selectedStudents
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(name, modifier = Modifier.weight(1f))
                        Checkbox(
                            checked = checked,
                            onCheckedChange = {
                                selectedStudents =
                                    if (it == true) selectedStudents + uid else selectedStudents - uid
                            }
                        )
                    }
                    Divider()
                }
            }

            if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error)

            Button(
                onClick = { save() },
                enabled = !saving,
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (saving) "Guardando..." else "Crear clase") }
        }
    }
}
