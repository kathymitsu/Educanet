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
fun LoginScreen(
    onSuccess: () -> Unit,
    onGoRegister: () -> Unit,
    snackbarHostState: SnackbarHostState
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val auth = remember { FirebaseAuth.getInstance() }
    val db = remember { FirebaseFirestore.getInstance() }

    var email by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }

    var emailErr by remember { mutableStateOf<String?>(null) }
    var passErr by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }

    fun validate(): Boolean {
        emailErr = AuthValidators.validateEmail(email)?.message
        passErr = AuthValidators.validatePassword(pass)?.message
        return emailErr == null && passErr == null
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
                },
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
