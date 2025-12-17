package com.example.educanet.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateProfessorScreen(
    onBack: () -> Unit
) {
    // La instancia principal de DB usará las credenciales del admin
    val db = remember { FirebaseFirestore.getInstance() }
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }

    var nameError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }

    val minChars = 6

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Crear Profesor") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás")
                    }
                }
            )
        }
    ) { pad ->
        Column(
            modifier = Modifier
                .padding(pad)
                .padding(24.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- CAMPOS DE TEXTO (SIN CAMBIOS) ---
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
                    nameError = null
                    emailError = null
                    passwordError = null

                    if (name.isBlank()) {
                        nameError = "El nombre es obligatorio."
                        isValid = false
                    }

                    val expectedDomain = "@profesoreducanet.cl"
                    if (!email.endsWith(expectedDomain, ignoreCase = true)) {
                        emailError = "El correo de profesor debe tener el dominio $expectedDomain."
                        isValid = false
                    }

                    if (password.length < minChars) {
                        passwordError = "La contraseña debe tener al menos $minChars caracteres."
                        isValid = false
                    }

                    if (!isValid) return@Button

                    loading = true
                    scope.launch {
                        var secondaryApp: FirebaseApp? = null
                        try {
                            val options = FirebaseApp.getInstance().options
                            val appName = "secondary-auth-${UUID.randomUUID()}"
                            secondaryApp = FirebaseApp.initializeApp(context, options, appName)
                            val secondaryAuth = FirebaseAuth.getInstance(secondaryApp)

                            val result = secondaryAuth.createUserWithEmailAndPassword(email, password).await()
                            val uid = result.user?.uid ?: throw IllegalStateException("No se pudo obtener el UID del usuario creado")

                            val data = mapOf(
                                "uid" to uid,
                                "name" to name,
                                "email" to email,
                                "role" to "profesor"
                            )
                            
                            db.collection("users").document(uid).set(data).await()

                            loading = false
                            snackbar.showSnackbar("Profesor creado con éxito.")
                            onBack()

                        } catch (e: Exception) {
                            loading = false
                            snackbar.showSnackbar(e.message ?: "Error al crear la cuenta del profesor")
                        } finally {
                            secondaryApp?.delete()
                        }
                    }
                },
                enabled = !loading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (loading) "Creando..." else "Crear Profesor")
            }
        }
    }
}