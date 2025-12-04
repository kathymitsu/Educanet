package com.example.educanet.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentProgressScreen(
    studentId: String,
    onBack: () -> Unit
) {
    val db = remember { FirebaseFirestore.getInstance() }
    var studentName by remember { mutableStateOf("") }
    var progressList by remember { mutableStateOf<List<Pair<String, Double>>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(studentId) {
        loading = true
        // Get student name
        db.collection("users").document(studentId).get()
            .addOnSuccessListener { doc ->
                studentName = doc.getString("name") ?: doc.getString("email") ?: "Unknown"
            }
            .addOnFailureListener { e ->
                error = "Error fetching student details: ${e.message}"
            }

        // Get student progress
        // This is a placeholder. We need to know the actual data structure for progress.
        // Assuming a "progress" collection with documents per student,
        // and each document has a map of classId to progress percentage.
        db.collection("progress").document(studentId).get()
            .addOnSuccessListener { doc ->
                val progressData = doc.data
                if (progressData != null) {
                    // Assuming progress is stored as a map of class names to a numeric value (e.g., percentage)
                    val progressMap = progressData.mapValues { it.value as? Double ?: 0.0 }
                    progressList = progressMap.toList()
                }
                loading = false
            }
            .addOnFailureListener { e ->
                error = "Error fetching progress: ${e.message}"
                loading = false
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Progreso de $studentName") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (loading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (error != null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(text = error!!, color = MaterialTheme.colorScheme.error)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
            ) {
                if (progressList.isEmpty()) {
                    item {
                        Text("No se encontró progreso para este estudiante.")
                    }
                } else {
                    items(progressList) { (className, progress) ->
                        Column(modifier = Modifier.padding(vertical = 8.dp)) {
                            Text(text = className, style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { (progress / 100).toFloat() },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(text = "${progress.toInt()}% completado", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}