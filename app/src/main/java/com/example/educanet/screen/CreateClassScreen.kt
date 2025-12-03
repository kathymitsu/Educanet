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
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateClassScreen(
    onCancel: () -> Unit,
    onSaved: () -> Unit
) {
    val auth = remember { FirebaseAuth.getInstance() }
    val db = remember { FirebaseFirestore.getInstance() }
    val uid = auth.currentUser?.uid.orEmpty()

    val scope = rememberCoroutineScope()
    val snack = remember { SnackbarHostState() }

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var priceText by remember { mutableStateOf("") }
    var seatsText by remember { mutableStateOf("") }
    var imageUrl by remember { mutableStateOf("") }

    var loading by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }

    fun saveClass() {
        errorText = null

        if (title.isBlank() || description.isBlank() ||
            priceText.isBlank() || seatsText.isBlank()
        ) {
            errorText = "Debes completar todos los campos antes de continuar."
            return
        }

        val price = priceText.toDoubleOrNull()
        val seats = seatsText.toIntOrNull()

        if (price == null || price < 0.0) {
            errorText = "El precio debe ser un número válido."
            return
        }
        if (seats == null || seats <= 0) {
            errorText = "Los cupos deben ser un número mayor a 0."
            return
        }

        loading = true
        val now = Timestamp.now()

        val data = mapOf(
            "title" to title,
            "description" to description,
            "price" to price,
            "availableSeats" to seats,
            "imageUrl" to imageUrl,
            "professorId" to uid,
            "createdBy" to uid,
            "createdAt" to now,
            "isActive" to true,
            "assignedStudents" to emptyList<String>()
        )

        db.collection("classes")
            .add(data)
            .addOnSuccessListener {
                loading = false
                scope.launch {
                    snack.showSnackbar("Clase creada correctamente.")
                }
                onSaved()
            }
            .addOnFailureListener { e ->
                loading = false
                scope.launch {
                    snack.showSnackbar("Error al crear clase: ${e.message}")
                }
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Crear clase") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
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
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Título de la clase") },
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
                value = priceText,
                onValueChange = { priceText = it },
                label = { Text("Precio") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = seatsText,
                onValueChange = { seatsText = it },
                label = { Text("Cupos disponibles") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = imageUrl,
                onValueChange = { imageUrl = it },
                label = { Text("URL de imagen (opcional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            if (errorText != null) {
                Text(
                    text = errorText!!,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancelar")
                }

                Button(
                    onClick = { if (!loading) saveClass() },
                    modifier = Modifier.weight(1f),
                    enabled = !loading
                ) {
                    Text(if (loading) "Guardando..." else "Guardar")
                }
            }
        }
    }
}
