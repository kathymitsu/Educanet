package com.example.educanet.screen

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

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

    // Portada (galería)
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> imageUri = uri }

    // Dropdown de profesores
    var profs by remember { mutableStateOf(listOf<Pair<String, String>>()) } // uid -> nombre
    var profMenu by remember { mutableStateOf(false) }
    var selectedProfessor by remember { mutableStateOf<String?>(null) }

    // Checkboxes de alumnos
    var students by remember { mutableStateOf(listOf<Pair<String, String>>()) } // uid -> nombre
    var selectedStudents by remember { mutableStateOf(setOf<String>()) }

    var loading by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val snack = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Cargar usuarios (profes y alumnos)
    LaunchedEffect(Unit) {
        loading = true
        error = null
        try {
            val profQs = db.collection("users")
                .whereEqualTo("role", "profesor")
                .get()
                .await()
            profs = profQs.documents.map { d ->
                val id = d.id
                val name = d.getString("name") ?: d.getString("email") ?: id
                id to name
            }.sortedBy { it.second.lowercase() }
            if (selectedProfessor == null && profs.isNotEmpty()) {
                selectedProfessor = profs.first().first
            }
        } catch (e: Exception) {
            error = "Error cargando profesores: ${e.message}"
        }

        try {
            val stuQs = db.collection("users")
                .whereEqualTo("role", "estudiante")
                .get()
                .await()
            students = stuQs.documents.map { d ->
                val id = d.id
                val name = d.getString("name") ?: d.getString("email") ?: id
                id to name
            }.sortedBy { it.second.lowercase() }
        } catch (e: Exception) {
            error = (error?.let { "$it\n" } ?: "") + "Error cargando alumnos: ${e.message}"
        }
        loading = false
    }

    fun save() {
        error = null
        if (title.isBlank() || selectedProfessor == null || selectedStudents.isEmpty()) {
            error = "Título, profesor y al menos un estudiante son obligatorios."
            return
        }
        saving = true
        scope.launch {
            try {
                var imageUrl = ""
                imageUri?.let { local ->
                    val fileName = "class_images/${UUID.randomUUID()}.jpg"
                    val imageRef = Firebase.storage.reference.child(fileName)
                    imageRef.putFile(local).await()
                    imageUrl = imageRef.downloadUrl.await().toString()
                }

                val data = mapOf(
                    "title" to title.trim(),
                    "description" to description.trim(),
                    "videoLink" to videoLink.trim(),
                    "professorId" to selectedProfessor!!,
                    "assignedStudents" to selectedStudents.toList(),
                    "imageUrl" to imageUrl,
                    "createdBy" to myUid,
                    "createdAt" to Timestamp.now(),
                    "isActive" to true
                )

                db.collection("classes").add(data).await()
                snack.showSnackbar("Clase creada")
                onSaved()
            } catch (e: Exception) {
                error = e.localizedMessage ?: "Error al guardar"
            } finally {
                saving = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nueva clase (Admin)") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = null) }
                }
            )
        },
        snackbarHost = { SnackbarHost(snack) }
    ) { pad ->
        if (loading) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(pad),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .padding(pad)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Título") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Descripción") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = videoLink,
                onValueChange = { videoLink = it },
                label = { Text("Link video (opcional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Profesor asignado
            ExposedDropdownMenuBox(
                expanded = profMenu,
                onExpandedChange = { profMenu = !profMenu }
            ) {
                OutlinedTextField(
                    value = selectedProfessor?.let { uid ->
                        profs.find { it.first == uid }?.second
                    } ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Profesor asignado") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = profMenu) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = profMenu,
                    onDismissRequest = { profMenu = false }
                ) {
                    profs.forEach { (uid, name) ->
                        DropdownMenuItem(
                            text = { Text(name) },
                            onClick = {
                                selectedProfessor = uid
                                profMenu = false
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            OutlinedButton(
                onClick = { imagePickerLauncher.launch("image/*") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Seleccionar imagen")
            }

            imageUri?.let {
                Spacer(Modifier.height(16.dp))
                AsyncImage(
                    model = it,
                    contentDescription = "Imagen Seleccionada",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(Modifier.height(16.dp))

            Text("Estudiantes asignados", style = MaterialTheme.typography.titleMedium)

            // Lista simple (sin scroll anidado)
            Column(Modifier.fillMaxWidth()) {
                students.forEach { (uid, name) ->
                    val checked = uid in selectedStudents
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(name, modifier = Modifier.weight(1f))
                        Checkbox(
                            checked = checked,
                            onCheckedChange = { isChecked ->
                                selectedStudents = if (isChecked) {
                                    selectedStudents + uid
                                } else {
                                    selectedStudents - uid
                                }
                            }
                        )
                    }
                    Divider()
                }
            }

            error?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { save() },
                enabled = !saving,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (saving) "Guardando..." else "Crear clase")
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}
