package com.example.educanet.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class Student(val uid: String, val name: String, val email: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentProgressScreen(
    onBack: () -> Unit,
    onOpenProgress: (String) -> Unit,
) {
    val db = remember { FirebaseFirestore.getInstance() }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var students by remember { mutableStateOf<List<Student>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        loading = true
        try {
            val snapshot = db.collection("users")
                .whereEqualTo("role", "estudiante")
                .orderBy("name", Query.Direction.ASCENDING)
                .get()
                .await()

            students = snapshot.documents.mapNotNull { doc ->
                Student(
                    uid = doc.id,
                    name = doc.getString("name") ?: "Nombre no disponible",
                    email = doc.getString("email") ?: "Email no disponible"
                )
            }
        } catch (e: Exception) {
            scope.launch { snackbar.showSnackbar("Error al cargar alumnos: ${e.message}") }
        } finally {
            loading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Progreso de Alumnos") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Atrás") } }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        if (loading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(students, key = { it.uid }) { student ->
                    ListItem(
                        headlineContent = { Text(student.name) },
                        supportingContent = { Text(student.email) },
                        modifier = Modifier.clickable { onOpenProgress(student.uid) }
                    )
                }
            }
        }
    }
}