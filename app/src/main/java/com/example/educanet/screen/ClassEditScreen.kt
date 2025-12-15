package com.example.educanet.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.educanet.item.ClassItem
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassEditScreen(
    classId: String?,
    onBack: () -> Unit,
) {
    val db = remember { FirebaseFirestore.getInstance() }
    val auth = remember { FirebaseAuth.getInstance() }
    val uid = auth.currentUser?.uid.orEmpty()

    var item by remember { mutableStateOf<ClassItem?>(null) }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var imageUrl by remember { mutableStateOf("") }
    var videoLink by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("0.0") }
    var availableSeats by remember { mutableStateOf("0") }
    var isActive by remember { mutableStateOf(true) }

    var titleError by remember { mutableStateOf<String?>(null) }
    var descriptionError by remember { mutableStateOf<String?>(null) }
    var priceError by remember { mutableStateOf<String?>(null) }
    var stockError by remember { mutableStateOf<String?>(null) }

    var isLoading by remember { mutableStateOf(true) }
    var errorText by remember { mutableStateOf<String?>(null) }
    val showDeleteDialog = remember { mutableStateOf(false) }

    // Cargar datos si es una edición
    LaunchedEffect(classId) {
        if (classId == null) {
            isLoading = false
            item = ClassItem(professorId = uid, createdBy = uid) // Pre-rellenar para nueva clase
            return@LaunchedEffect
        }

        isLoading = true
        try {
            val doc = db.collection("classes").document(classId).get().await()
            if (doc.exists()) {
                val loadedItem = doc.toObject<ClassItem>()?.copy(id = doc.id)
                if (loadedItem != null) {
                    item = loadedItem
                    title = loadedItem.title
                    description = loadedItem.description
                    imageUrl = loadedItem.imageUrl
                    videoLink = loadedItem.videoLink
                    price = loadedItem.price.toString()
                    availableSeats = loadedItem.availableSeats.toString()
                    isActive = loadedItem.isActive
                } else {
                    errorText = "Error al decodificar la clase."
                }
            } else {
                errorText = "No se encontró la clase."
            }
        } catch (e: Exception) {
            errorText = "Error al cargar la clase: ${e.message}"
        } finally {
            isLoading = false
        }
    }

    fun saveClass() {
        titleError = null
        descriptionError = null
        priceError = null
        stockError = null
        var isValid = true

        if (title.isBlank()) {
            titleError = "El título es obligatorio."
            isValid = false
        }

        if (description.isBlank()) {
            descriptionError = "La descripción es obligatoria."
            isValid = false
        }

        val priceValue = price.toDoubleOrNull()
        if (price.isBlank()) {
            priceError = "El precio es obligatorio."
            isValid = false
        } else if (priceValue == null) {
            priceError = "El precio debe ser un número válido."
            isValid = false
        } else if (priceValue <= 1) {
            priceError = "El precio debe ser mayor a 1."
            isValid = false
        }

        val stockValue = availableSeats.toLongOrNull()
        if (availableSeats.isBlank()) {
            stockError = "El cupo es obligatorio."
            isValid = false
        } else if (stockValue == null) {
            stockError = "El cupo debe ser un número válido."
            isValid = false
        } else if (stockValue <= 1) {
            stockError = "El cupo debe ser mayor a 1."
            isValid = false
        }

        if (!isValid || uid.isBlank()) {
            return
        }

        isLoading = true

        val data = hashMapOf(
            "title" to title,
            "description" to description,
            "imageUrl" to imageUrl,
            "videoLink" to videoLink,
            "price" to priceValue,
            "availableSeats" to stockValue,
            "isActive" to isActive,
            "professorId" to item?.professorId.orEmpty().ifBlank { uid },
            "createdBy" to item?.createdBy.orEmpty().ifBlank { uid },
            "createdAt" to (item?.createdAt ?: Timestamp.now())
        )

        val task = if (classId == null) {
            db.collection("classes").add(data)
        } else {
            db.collection("classes").document(classId).set(data)
        }

        task.addOnSuccessListener { onBack() }
            .addOnFailureListener { e ->
                errorText = "Error al guardar: ${e.message}"
                isLoading = false
            }
    }

    fun deleteClass() {
        if (classId == null) return
        isLoading = true
        db.collection("classes").document(classId).delete()
            .addOnSuccessListener { onBack() }
            .addOnFailureListener { e ->
                errorText = "Error al eliminar: ${e.message}"
                isLoading = false
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (classId == null) "Nueva Clase" else "Editar Clase") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(value = title, onValueChange = { title = it; titleError = null }, label = { Text("Título") }, modifier = Modifier.fillMaxWidth(), isError = titleError != null)
                if (titleError != null) {
                    Text(titleError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                OutlinedTextField(value = description, onValueChange = { description = it; descriptionError = null }, label = { Text("Descripción") }, modifier = Modifier.fillMaxWidth(), isError = descriptionError != null)
                if (descriptionError != null) {
                    Text(descriptionError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                OutlinedTextField(value = imageUrl, onValueChange = { imageUrl = it }, label = { Text("URL de la Imagen") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = videoLink, onValueChange = { videoLink = it }, label = { Text("Link del Video") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = price, onValueChange = { price = it; priceError = null }, label = { Text("Precio") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth(), isError = priceError != null)
                if (priceError != null) {
                    Text(priceError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                OutlinedTextField(value = availableSeats, onValueChange = { availableSeats = it; stockError = null }, label = { Text("Cupos Disponibles") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(), isError = stockError != null)
                if (stockError != null) {
                    Text(stockError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = isActive, onCheckedChange = { isActive = it })
                    Spacer(Modifier.width(8.dp))
                    Text("Clase Activa")
                }

                errorText?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                }

                Spacer(Modifier.height(16.dp))

                Button(onClick = ::saveClass, modifier = Modifier.fillMaxWidth(), enabled = !isLoading) {
                    Text("Guardar")
                }

                if (classId != null) {
                    Spacer(Modifier.height(8.dp))
                    TextButton(
                        onClick = { showDeleteDialog.value = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Eliminar clase", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }

        if (showDeleteDialog.value) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog.value = false },
                title = { Text("Confirmar Eliminación") },
                text = { Text("¿Estás seguro de que quieres eliminar esta clase? Esta acción no se puede deshacer.") },
                confirmButton = {
                    TextButton(onClick = { 
                        showDeleteDialog.value = false
                        deleteClass() 
                    }) {
                        Text("Eliminar")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog.value = false }) {
                        Text("Cancelar")
                    }
                }
            )
        }
    }
}
