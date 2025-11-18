package com.example.educanet.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.example.educanet.util.AuthValidators
import com.example.educanet.data.UserPrefs
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onSuccess: () -> Unit,
    onGoLogin: () -> Unit
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val auth = remember { FirebaseAuth.getInstance() }
    val db = remember { FirebaseFirestore.getInstance() }

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }

    var nameErr by remember { mutableStateOf<String?>(null) }
    var emailErr by remember { mutableStateOf<String?>(null) }
    var passErr by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }

    fun validate(): Boolean {
        // Validate name: Check if it is empty first for a clearer message.
        if (name.isBlank()) {
            nameErr = "El nombre es obligatorio."
        } else {
            nameErr = AuthValidators.validateName(name)?.message
        }

        // Validate email: Check for emptiness, then format, then the specific domain.
        if (email.isBlank()) {
            emailErr = "El correo es obligatorio."
        } else {
            emailErr = AuthValidators.validateEmail(email)?.message
            // If the format is valid, check for the required domain.
            if (emailErr == null && !email.endsWith("@educanet.cl", ignoreCase = true)) {
                emailErr = "El correo debe pertenecer al dominio @educanet.cl."
            }
        }

        // Validate password: Check for emptiness first.
        if (pass.isBlank()) {
            passErr = "La contraseña es obligatoria."
        } else {
            passErr = AuthValidators.validatePassword(pass)?.message
        }

        // Return true only if all error messages are null.
        return listOf(nameErr, emailErr, passErr).all { it == null }
    }

    Scaffold { pad ->
        Column(
            Modifier
                .padding(pad)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Crear cuenta", style = MaterialTheme.typography.titleLarge)

            OutlinedTextField(
                value = name, onValueChange = { name = it; nameErr = null },
                label = { Text("Nombre") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                isError = nameErr != null,
                supportingText = { if (nameErr != null) Text(nameErr!!, color = MaterialTheme.colorScheme.error) },
                trailingIcon = { if (nameErr != null) Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
            )

            OutlinedTextField(
                value = email, onValueChange = { email = it; emailErr = null },
                label = { Text("Correo") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                isError = emailErr != null,
                supportingText = { if (emailErr != null) Text(emailErr!!, color = MaterialTheme.colorScheme.error) },
                trailingIcon = { if (emailErr != null) Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
            )

            OutlinedTextField(
                value = pass, onValueChange = { pass = it; passErr = null },
                label = { Text("Contraseña") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                isError = passErr != null,
                supportingText = { if (passErr != null) Text(passErr!!, color = MaterialTheme.colorScheme.error) },
                trailingIcon = { if (passErr != null) Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                visualTransformation = PasswordVisualTransformation()
            )

            Button(
                onClick = {
                    if (!validate()) return@Button
                    loading = true
                    scope.launch {
                        try {
                            val res = auth.createUserWithEmailAndPassword(email, pass).await()
                            val uid = res.user?.uid ?: throw Exception("UID inválido")
                            db.collection("users").document(uid).set(
                                mapOf(
                                    "uid" to uid,
                                    "name" to name.trim(),
                                    "email" to email.trim(),
                                    "role" to "estudiante" // por defecto
                                )
                            ).await()
                            UserPrefs.saveProfile(ctx, name.trim(), "estudiante")
                            onSuccess()
                        } catch (e: Exception) {
                            // Snackbar no pasado como parámetro aquí: mostrar un texto simple
                        } finally {
                            loading = false
                        }
                    }
                },
                enabled = !loading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (loading) CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (loading) "Creando..." else "Crear cuenta")
            }

            TextButton(onClick = onGoLogin) { Text("Ya tengo cuenta") }
        }
    }
}

