// app/src/main/java/com/example/educanet/screen/CreateClassScreenAdmin.kt
package com.example.educanet.screen

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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

    var imageUri by remember { mutableStateOf<Uri?>(null) }
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? -> imageUri = uri }
    )

    // profesores
    var profs by remember { mutableStateOf(listOf<Pair<String, String>>()) } // (uid to name)
    var profMenu by remember { mutableStateOf(false) }
    var selectedProfessor by remember { mutableStateOf<String?>(null) }

    // alumnos
    var students by remember { mutableStateOf(listOf<Pair<String, String>>()) } // (uid to name)
    var selectedStudents by remember { mutableStateOf(setOf<String>()) }

    var loading by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val snack = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Cargar usuarios
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

    suspend fun uploadCoverAndGetUrl(localUri: Uri?): String {
        if (localUri == null) return ""
        val fileName = "class_images/${UUID.randomUUID()}.jpg"
        val ref = Firebase.storage.reference.child(fileName)

        // Añade contentType para que el visor lo sirva bien (no es obligatorio, pero ayuda)
        val metadata = com.google.firebase.storage.StorageMetadata.Builder()
            .setContentType("image/jpeg")
            .build()

        // Subir y obtener URL de descarga (la parte que suele fallar si no se hace así)
        ref.putFile(localUri, metadata).await()
        return ref.downloadUrl.await().toString()
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
                val imageUrl = uploadCoverAndGetUrl(imageUri) // "" si no hay imagen

                val data = mapOf(
                    "title" to title.trim(),
                    "description" to description.trim(),
                    "videoLink" to videoLink.trim(),
                    "professorId" to selectedProfessor!!,
                    "assignedStudents" to selectedStudents.toList(),
                    "imageUrl" to imageUrl,               // <<--- URL COMPLETA https
                    "createdBy" to myUid,
                    "createdAt" to Timestamp.now(),
                    "isActive" to true
                )

                db.collection("classes").add(data).await()
                snack.showSnackbar("Clase creada")
                onSaved()
            } catch (e: Exception) {
                error = e.localizedMessage ?: "Error al guardar"
                snack.showSnackbar("No se pudo crear: ${error}")
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
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null) }
                }
            )
        },
        snackbarHost = { SnackbarHost(snack) }
    ) { pad ->
        if (loading) {
            Box(
                Modifier.fillMaxSize().padding(pad),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .padding(pad)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(Modifier.height(16.dp))
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

            // Profesor asignado
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

            Spacer(Modifier.height(16.dp))
            OutlinedButton(
                onClick = { imagePickerLauncher.launch("image/*") },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Seleccionar imagen") }

            imageUri?.let {
                Spacer(Modifier.height(12.dp))
                AsyncImage(
                    model = it,
                    contentDescription = "Portada seleccionada",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(Modifier.height(16.dp))
            Text("Estudiantes asignados", style = MaterialTheme.typography.titleMedium)
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(students, key = { it.first }) { (uid, name) ->
                    val checked = uid in selectedStudents
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
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

            if (error != null) {
                Text(error!!, color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(8.dp))
            }

            Button(
                onClick = { save() },
                enabled = !saving,
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (saving) "Guardando..." else "Crear clase") }

            Spacer(Modifier.height(24.dp))
        }
    }
}
