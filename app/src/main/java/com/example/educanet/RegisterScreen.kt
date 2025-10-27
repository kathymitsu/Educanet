package com.example.educanet

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onGoLogin: () -> Unit,
    onSuccess: () -> Unit
) {
    val auth = remember { FirebaseAuth.getInstance() }
    val db = remember { FirebaseFirestore.getInstance() }

    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var pass by rememberSaveable { mutableStateOf("") }
    var pass2 by rememberSaveable { mutableStateOf("") }

    // 👇 rol con dropdown (estudiante o profesor)
    var role by rememberSaveable { mutableStateOf("estudiante") }
    var roleMenu by remember { mutableStateOf(false) }

    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun createProfile(uid: String) {
        val userDoc = mapOf(
            "uid" to uid,
            "name" to name.trim(),
            "email" to email.trim(),
            "role" to role, //
            "createdAt" to FieldValue.serverTimestamp()
        )
        db.collection("users").document(uid).set(userDoc)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> error = "No se pudo guardar el perfil: ${e.message}" }
            .addOnCompleteListener { loading = false }
    }

    fun doRegister() {
        error = null
        if (name.isBlank() || email.isBlank() || pass.length < 6 || pass != pass2) {
            error = "Revisa nombre, email y contraseñas (mín. 6 y iguales)"
            return
        }
        loading = true
        auth.createUserWithEmailAndPassword(email.trim(), pass)
            .addOnSuccessListener { res ->
                val uid = res.user?.uid ?: return@addOnSuccessListener
                createProfile(uid) //
            }
            .addOnFailureListener { e ->
                loading = false
                error = e.message
            }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Crear cuenta", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = name, onValueChange = { name = it },
            label = { Text("Nombre") }, singleLine = true, modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = email, onValueChange = { email = it },
            label = { Text("Correo") }, singleLine = true, modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = pass, onValueChange = { pass = it },
            label = { Text("Contraseña") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = pass2, onValueChange = { pass2 = it },
            label = { Text("Repite contraseña") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        // ▼ Selector de rol (Material 3 Exposed Dropdown)
        Spacer(Modifier.height(12.dp))
        ExposedDropdownMenuBox(
            expanded = roleMenu,
            onExpandedChange = { roleMenu = !roleMenu }
        ) {
            OutlinedTextField(
                value = if (role == "profesor") "Profesor" else "Estudiante",
                onValueChange = {},
                readOnly = true,
                label = { Text("Rol") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = roleMenu) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = roleMenu,
                onDismissRequest = { roleMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Estudiante") },
                    onClick = { role = "estudiante"; roleMenu = false }
                )
                DropdownMenuItem(
                    text = { Text("Profesor") },
                    onClick = { role = "profesor"; roleMenu = false }
                )
            }
        }
        // ▲ Fin selector rol

        if (error != null) {
            Spacer(Modifier.height(8.dp))
            Text(error!!, color = MaterialTheme.colorScheme.error)
        }

        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { doRegister() },
            enabled = !loading,
            modifier = Modifier.fillMaxWidth()
        ) { Text(if (loading) "Creando..." else "Registrarme") }

        TextButton(onClick = onGoLogin, modifier = Modifier.fillMaxWidth()) {
            Text("Ya tengo cuenta")
        }
    }
}
