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
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: (role: String) -> Unit,
    onRegisterClick: () -> Unit
) {
    val auth = remember { FirebaseAuth.getInstance() }
    val db = remember { FirebaseFirestore.getInstance() }
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }

    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }

    val allowedDomains = listOf("@educanet.cl", "@admineducanet.cl", "@profesoreducanet.cl", "@apoeducanet.cl")

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
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Educanet", style = MaterialTheme.typography.headlineMedium)
                Spacer(modifier = Modifier.height(16.dp))

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

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        var isValid = true

                        // 1. Validar correo
                        if (email.isBlank()) {
                            emailError = "El correo es obligatorio."
                            isValid = false
                        } else if (allowedDomains.none { email.endsWith(it, ignoreCase = true) }) {
                            emailError = "El dominio del correo no es válido."
                            isValid = false
                        }

                        // 2. Validar contraseña
                        if (password.isBlank()) {
                            passwordError = "La contraseña es obligatoria."
                            isValid = false
                        }

                        if (!isValid) return@Button

                        loading = true
                        auth.signInWithEmailAndPassword(email, password)
                            .addOnSuccessListener { authResult ->
                                val user = authResult.user
                                if (user == null) {
                                    loading = false
                                    scope.launch { snackbar.showSnackbar("Error de autenticación.") }
                                    return@addOnSuccessListener
                                }

                                db.collection("users").document(user.uid).get()
                                    .addOnSuccessListener { document ->
                                        loading = false
                                        if (document != null && document.exists()) {
                                            val role = document.getString("role") ?: "estudiante"
                                            onLoginSuccess(role)
                                        } else {
                                            scope.launch { snackbar.showSnackbar("Error: No se encontraron datos de usuario.") }
                                        }
                                    }
                                    .addOnFailureListener {
                                        loading = false
                                        scope.launch { snackbar.showSnackbar("Error al obtener datos del usuario.") }
                                    }
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
