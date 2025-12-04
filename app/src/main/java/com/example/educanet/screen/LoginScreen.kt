package com.example.educanet.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onRegisterClick: () -> Unit
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

                Button(
                    onClick = {
                        // 💬 Mensaje cuando hay campos vacíos
                        if (email.isBlank() || password.isBlank()) {
                            scope.launch {
                                snackbar.showSnackbar("Por favor no dejes campos vacíos.")
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

                                val fbEx = e as? FirebaseAuthException
                                val userMsg = when (fbEx?.errorCode) {
                                    "ERROR_INVALID_EMAIL" ->
                                        "El formato del correo no es válido."
                                    "ERROR_USER_NOT_FOUND" ->
                                        "No existe una cuenta registrada con este correo."
                                    "ERROR_WRONG_PASSWORD",
                                    "ERROR_INVALID_CREDENTIAL" ->
                                        "Correo o contraseña incorrectos. Intenta nuevamente."
                                    "ERROR_USER_DISABLED" ->
                                        "Esta cuenta ha sido deshabilitada."
                                    else ->
                                        "No se pudo iniciar sesión. Revisa tus datos e inténtalo nuevamente."
                                }

                                scope.launch {
                                    snackbar.showSnackbar(userMsg)
                                }
                            }
                    },
                    enabled = !loading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (loading) "Ingresando..." else "Iniciar sesión")
                }

                TextButton(onClick = onRegisterClick) {
                    Text("¿No tienes cuenta? Regístrate")
                }
            }
        }
    }
}
