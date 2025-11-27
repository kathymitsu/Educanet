package com.example.educanet.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.educanet.data.UserPrefs
import com.example.educanet.util.AuthValidators
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onSuccess: () -> Unit,
    onGoRegister: () -> Unit,
    snackbarHostState: SnackbarHostState
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val auth = remember { FirebaseAuth.getInstance() }
    val db = remember { FirebaseFirestore.getInstance() }
    val focusManager = LocalFocusManager.current

    var email by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }

    var emailErr by remember { mutableStateOf<String?>(null) }
    var passErr by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }

    var emailTouched by remember { mutableStateOf(false) }
    var passTouched by remember { mutableStateOf(false) }

    fun validate(isSubmit: Boolean = false): Boolean {
        if (emailTouched || isSubmit) {
            emailErr = AuthValidators.validateEmail(email)?.message
        }
        if (passTouched || isSubmit) {
            passErr = AuthValidators.validatePassword(pass)?.message
        }
        return emailErr == null && passErr == null
    }

    // Convertir la lambda en una función local
    fun handleSubmit() {
        if (!validate(isSubmit = true)) return

        loading = true
        focusManager.clearFocus() // Oculta el teclado
        scope.launch {
            try {
                auth.signInWithEmailAndPassword(email, pass).await()
                val uid = auth.currentUser?.uid.orEmpty()
                val doc = db.collection("users").document(uid).get().await()
                val name = doc.getString("name") ?: (doc.getString("email") ?: "")
                val role = doc.getString("role") ?: "estudiante"
                UserPrefs.saveProfile(ctx, name, role)
                onSuccess()
            } catch (e: Exception) {
                snackbarHostState.showSnackbar(e.message ?: "Error de autenticación")
            } finally {
                loading = false
            }
        }
    }


    Scaffold { pad ->
        Column(
            Modifier
                .padding(pad)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Iniciar sesión", style = MaterialTheme.typography.titleLarge)

            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    if (emailTouched) validate()
                },
                label = { Text("Correo") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { focusState ->
                        if (!focusState.isFocused && email.isNotEmpty()) {
                            emailTouched = true
                            validate()
                        }
                    },
                isError = emailErr != null,
                supportingText = { if (emailErr != null) Text(emailErr!!, color = MaterialTheme.colorScheme.error) },
                trailingIcon = { if (emailErr != null) Icon(Icons.Default.Error, contentDescription = "Error de correo", tint = MaterialTheme.colorScheme.error) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                )
            )

            OutlinedTextField(
                value = pass,
                onValueChange = {
                    pass = it
                    if (passTouched) validate()
                },
                label = { Text("Contraseña") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { focusState ->
                        if (!focusState.isFocused && pass.isNotEmpty()) {
                            passTouched = true
                            validate()
                        }
                    },
                isError = passErr != null,
                supportingText = { if (passErr != null) Text(passErr!!, color = MaterialTheme.colorScheme.error) },
                trailingIcon = { if (passErr != null) Icon(Icons.Default.Error, contentDescription = "Error de contraseña", tint = MaterialTheme.colorScheme.error) },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { handleSubmit() } // Se llama a la nueva función
                )
            )

            Button(
                onClick = { handleSubmit() }, // Se llama a la nueva función
                enabled = !loading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (loading) CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (loading) "Ingresando..." else "Ingresar")
            }

            TextButton(onClick = onGoRegister) { Text("Crear cuenta") }
        }
    }
}