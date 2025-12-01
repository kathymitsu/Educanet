package com.example.educanet.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

// 👇 NUEVOS IMPORTS
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit
) {
    val auth = remember { FirebaseAuth.getInstance() }
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbar) }
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
                Text("Educanet", style = MaterialTheme.typography.headlineMedium)

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Correo") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        // 👇 Appium lo verá como content-desc = "input_email"
                        .semantics { contentDescription = "input_email" }
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Contraseña") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier
                        .fillMaxWidth()
                        // 👇 Appium lo verá como "input_password"
                        .semantics { contentDescription = "input_password" }
                )

                Button(
                    onClick = {
                        if (email.isBlank() || password.isBlank()) {
                            scope.launch {
                                snackbar.showSnackbar("Completa correo y contraseña.")
                            }
                            return@Button
                        }
                        loading = true
                        auth.signInWithEmailAndPassword(email, password)
                            .addOnSuccessListener {
                                loading = false
                                onLoginSuccess()
                            }
                            .addOnFailureListener { e ->
                                loading = false
                                scope.launch {
                                    snackbar.showSnackbar(
                                        e.message ?: "Error al iniciar sesión"
                                    )
                                }
                            }
                    },
                    enabled = !loading,
                    modifier = Modifier
                        .fillMaxWidth()
                        // 👇 Botón login visible para Appium
                        .semantics { contentDescription = "btn_login" }
                ) {
                    Text(if (loading) "Ingresando..." else "Iniciar sesión")
                }
            }
        }
    }
}
