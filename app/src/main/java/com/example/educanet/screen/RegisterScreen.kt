package com.example.educanet.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

    val minChars = 6 // 👉 mínimo de caracteres

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
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Crear cuenta", style = MaterialTheme.typography.headlineMedium)

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Correo") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Contraseña") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("Confirmar contraseña") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        //CAMPOS VACÍOS
                        if (name.isBlank() || email.isBlank() || password.isBlank() || confirmPassword.isBlank()) {
                            scope.launch {
                                snackbar.showSnackbar("Por favor no dejes campos vacíos.")
                            }
                            return@Button
                        }

                        //DOMINIO DEL CORREO
                        if (!email.endsWith("@educanet.cl", ignoreCase = true)) {
                            scope.launch {
                                snackbar.showSnackbar("El correo debe tener el dominio @educanet.cl.")
                            }
                            return@Button
                        }

                        //CONTRASEÑAS NO COINCIDEN
                        if (password != confirmPassword) {
                            scope.launch {
                                snackbar.showSnackbar("Las contraseñas no coinciden.")
                            }
                            return@Button
                        }

                        //MÍNIMO DE CARACTERES
                        if (email.length < minChars || password.length < minChars) {
                            scope.launch {
                                snackbar.showSnackbar(
                                    "El correo y la contraseña deben tener al menos $minChars caracteres."
                                )
                            }
                            return@Button
                        }

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
                                    snackbar.showSnackbar(
                                        e.message ?: "Error al crear cuenta"
                                    )
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
