package com.example.educanet.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun LoginScreen(
    onGoRegister: () -> Unit,
    onSuccess: () -> Unit,
    snackbarHostState: SnackbarHostState
) {
    val auth = remember { FirebaseAuth.getInstance() }
    var email by rememberSaveable { mutableStateOf("") }
    var pass by rememberSaveable { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()

    fun doLogin() {
        error = null
        if (email.isBlank() || pass.length < 6) {
            error = "Completa email y clave (mín. 6 caracteres)"
            return
        }
        loading = true

        // Modificaremos la lógica de éxito para que sea asíncrona
        scope.launch {
            try {
                // 3. Inicia sesión y espera el resultado
                val authResult = auth.signInWithEmailAndPassword(email.trim(), pass).await()
                val user = authResult.user

                // 4. Si el login es exitoso, busca el nombre en Firestore
                var mensajeBienvenida = "¡Bienvenido/a de vuelta!" // Mensaje por defecto
                if (user != null) {
                    try {
                        val docUsuario = Firebase.firestore.collection("users").document(user.uid).get().await()
                        // Asumimos que tienes un campo llamado "name" en tu documento de usuario
                        val nombreUsuario = docUsuario.getString("name")
                        if (!nombreUsuario.isNullOrBlank()) {
                            mensajeBienvenida = "¡Bienvenido/a de vuelta, $nombreUsuario!"
                        }
                    } catch (e: Exception) {

                    }
                }

                snackbarHostState.showSnackbar(mensajeBienvenida)
                delay(4500)
                onSuccess()

            } catch (e: Exception) {

                error = e.message

            } finally {
                loading = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Educanet", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = email, onValueChange = { email = it },
            label = { Text("Correo") }, singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = pass, onValueChange = { pass = it },
            label = { Text("Contraseña") }, singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        if (error != null) {
            Spacer(Modifier.height(8.dp))
            Text(error ?: "", color = MaterialTheme.colorScheme.error)
        }

        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { doLogin() },
            enabled = !loading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (loading) "Ingresando..." else "Ingresar")
        }

        TextButton(onClick = onGoRegister, modifier = Modifier.fillMaxWidth()) {
            Text("Crear cuenta")
        }
    }
}
