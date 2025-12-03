package com.example.educanet.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit
) {
    val auth = remember { FirebaseAuth.getInstance() }
    val db = remember { FirebaseFirestore.getInstance() }
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }

    var nameError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var confirmPasswordError by remember { mutableStateOf<String?>(null) }

    val minChars = 6

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) }
    ) { pad ->
        Box(
            modifier = Modifier
                .padding(pad)
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Crear cuenta", style = MaterialTheme.typography.headlineMedium)
                Spacer(modifier = Modifier.height(16.dp))

                // --- CAMPO NOMBRE ---
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; nameError = null },
                    label = { Text("Nombre") },
                    singleLine = true,
                    isError = nameError != null,
                    modifier = Modifier.fillMaxWidth()
                )
                nameError?.let {
                    Text(text = it, color = Color.Red, style = MaterialTheme.typography.bodySmall)
                }

                // --- CAMPO CORREO ---
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it; emailError = null },
                    label = { Text("Correo") },
                    singleLine = true,
                    isError = emailError != null,
                    modifier = Modifier.fillMaxWidth()
                )
                emailError?.let {
                    Text(text = it, color = Color.Red, style = MaterialTheme.typography.bodySmall)
                }

                // --- CAMPO CONTRASEÑA ---
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; passwordError = null },
                    label = { Text("Contraseña") },
                    singleLine = true,
                    isError = passwordError != null,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                passwordError?.let {
                    Text(text = it, color = Color.Red, style = MaterialTheme.typography.bodySmall)
                }

                // --- CAMPO CONFIRMAR CONTRASEÑA ---
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it; confirmPasswordError = null },
                    label = { Text("Confirmar contraseña") },
                    singleLine = true,
                    isError = confirmPasswordError != null,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                confirmPasswordError?.let {
                    Text(text = it, color = Color.Red, style = MaterialTheme.typography.bodySmall)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        var isValid = true

                        // 1. Validar nombre
                        if (name.isBlank()) {
                            nameError = "El nombre es obligatorio."
                            isValid = false
                        }

                        // 2. Validar correo
                        if (email.isBlank()) {
                            emailError = "El correo es obligatorio."
                            isValid = false
                        } else if (!email.endsWith("@educanet.cl", ignoreCase = true)) {
                            emailError = "El correo debe tener el dominio @educanet.cl."
                            isValid = false
                        } else if (email.length < minChars) {
                            emailError = "El correo debe tener al menos $minChars caracteres."
                            isValid = false
                        }

                        // 3. Validar contraseña
                        if (password.isBlank()) {
                            passwordError = "La contraseña es obligatoria."
                            isValid = false
                        } else if (password.length < minChars) {
                            passwordError = "La contraseña debe tener al menos $minChars caracteres."
                            isValid = false
                        }

                        // 4. Validar confirmación de contraseña
                        if (confirmPassword.isBlank()) {
                            confirmPasswordError = "Confirma tu contraseña."
                            isValid = false
                        } else if (password != confirmPassword) {
                            confirmPasswordError = "Las contraseñas no coinciden."
                            isValid = false
                        }

                        if (!isValid) return@Button

                        loading = true
                        auth.createUserWithEmailAndPassword(email, password)
                            .addOnSuccessListener { res ->
                                val uid = res.user?.uid ?: ""
                                val data = mapOf(
                                    "uid" to uid,
                                    "name" to name,
                                    "email" to email,
                                    "role" to "estudiante"
                                )
                                db.collection("users").document(uid).set(data)
                                    .addOnSuccessListener {
                                        loading = false
                                        onRegisterSuccess()
                                    }
                                    .addOnFailureListener { e ->
                                        loading = false
                                        scope.launch {
                                            snackbar.showSnackbar(
                                                e.message ?: "Error al guardar usuario"
                                            )
                                        }
                                    }
                            }
                            .addOnFailureListener { e ->
                                loading = false
                                scope.launch {
                                    snackbar.showSnackbar(e.message ?: "Error al crear cuenta")
                                }
                            }
                    },
                    enabled = !loading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (loading) "Creando cuenta..." else "Registrarse")
                }
            }
        }
    }
}
