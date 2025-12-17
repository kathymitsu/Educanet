package com.example.educanet.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EvaluationEditScreen(
    classId: String?,
    evaluationId: String?,
    onBack: () -> Unit,
) {
    val db = remember { FirebaseFirestore.getInstance() }

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var questions by remember { mutableStateOf(listOf<String>()) }

    LaunchedEffect(evaluationId) {
        if (evaluationId != null) {
            db.collection("classes").document(classId!!).collection("evaluations").document(evaluationId).get()
                .addOnSuccessListener { doc ->
                    title = doc.getString("title") ?: ""
                    description = doc.getString("description") ?: ""
                    questions = doc.get("questions") as? List<String> ?: listOf()
                }
        }
    }

    fun saveEvaluation() {
        if (classId == null) return

        val evaluationData = hashMapOf(
            "title" to title,
            "description" to description,
            "questions" to questions,
            "createdAt" to Timestamp.now()
        )

        val collection = db.collection("classes").document(classId).collection("evaluations")

        if (evaluationId == null) {
            collection.add(evaluationData).addOnSuccessListener { onBack() }
        } else {
            collection.document(evaluationId).set(evaluationData).addOnSuccessListener { onBack() }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (evaluationId == null) "Nueva Evaluación" else "Editar Evaluación") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Título") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Descripción") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Preguntas", style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
                IconButton(onClick = { questions = questions + "" }) {
                    Icon(Icons.Default.Add, contentDescription = "Añadir pregunta")
                }
            }

            LazyColumn(modifier = Modifier.weight(1f)) {
                itemsIndexed(questions) { index, question ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = question,
                            onValueChange = { newText ->
                                questions = questions.toMutableList().also { it[index] = newText }
                            },
                            label = { Text("Pregunta ${index + 1}") },
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = {
                            questions = questions.toMutableList().also { it.removeAt(index) }
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Eliminar pregunta")
                        }
                    }
                }
            }

            Button(
                onClick = { saveEvaluation() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Guardar")
            }
        }
    }
}
