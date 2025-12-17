package com.example.educanet.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.example.educanet.item.EvaluationItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EvaluationListScreen(
    classId: String,
    onBack: () -> Unit,
    onOpenEvaluation: (String) -> Unit,
    onNewEvaluation: () -> Unit,
    onEditEvaluation: (String) -> Unit,
) {
    val db = remember { FirebaseFirestore.getInstance() }
    val auth = remember { FirebaseAuth.getInstance() }
    val uid = auth.currentUser?.uid.orEmpty()

    var evaluations by remember { mutableStateOf<List<EvaluationItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var role by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        db.collection("users").document(uid).get().addOnSuccessListener {
            role = it.getString("role")
        }

        db.collection("classes").document(classId).collection("evaluations")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    loading = false
                    return@addSnapshotListener
                }
                evaluations = snapshot?.toObjects(EvaluationItem::class.java) ?: emptyList()
                loading = false
            }
    }

    val canEdit = role == "admin" || role == "profesor"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Evaluaciones") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás")
                    }
                }
            )
        },
        floatingActionButton = {
            if (canEdit) {
                FloatingActionButton(onClick = onNewEvaluation) {
                    Icon(Icons.Default.Add, contentDescription = "Nueva Evaluación")
                }
            }
        }
    ) { paddingValues ->
        if (loading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(evaluations) { evaluation ->
                    EvaluationListItem(evaluation, canEdit, onOpenEvaluation, onEditEvaluation)
                }
            }
        }
    }
}

@Composable
private fun EvaluationListItem(
    evaluation: EvaluationItem,
    canEdit: Boolean,
    onOpenEvaluation: (String) -> Unit,
    onEditEvaluation: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenEvaluation(evaluation.id) },
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = evaluation.title, style = MaterialTheme.typography.titleMedium)
                Text(text = evaluation.description, style = MaterialTheme.typography.bodyMedium)
            }
            if (canEdit) {
                Spacer(modifier = Modifier.width(16.dp))
                IconButton(onClick = { onEditEvaluation(evaluation.id) }) {
                    Icon(Icons.Default.Edit, contentDescription = "Editar")
                }
            }
        }
    }
}
