package com.example.educanet.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Grade
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.launch
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.example.educanet.item.ClassItem
import com.example.educanet.item.GradeItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassDetailScreen(
    classId: String,
    onBack: () -> Unit
) {
    val auth = remember { FirebaseAuth.getInstance() }
    val db = remember { FirebaseFirestore.getInstance() }
    val uid = auth.currentUser?.uid.orEmpty()

    val scope = rememberCoroutineScope()
    val snack = remember { SnackbarHostState() }

    var classItem by remember { mutableStateOf<ClassItem?>(null) }
    var role by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }

    // uid -> nombre (o UID si no hay nombre)
    var assignedNames by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    var grades by remember { mutableStateOf(listOf<GradeItem>()) }
    var showAddEdit by remember { mutableStateOf(false) }
    var editing: GradeItem? by remember { mutableStateOf(null) }

    // ---------- Cargar rol y clase ----------
    LaunchedEffect(uid, classId) {
        loading = true

        db.collection("users").document(uid).get()
            .addOnSuccessListener { role = it.getString("role") }
            .addOnFailureListener { e ->
                scope.launch { snack.showSnackbar("Error rol: ${e.message}") }
            }

        db.collection("classes").document(classId).get()
            .addOnSuccessListener { d -> classItem = d.toObject(ClassItem::class.java) }
            .addOnFailureListener { e ->
                scope.launch { snack.showSnackbar("Error clase: ${e.message}") }
            }
            .addOnCompleteListener { loading = false }
    }

    // ---------- Cargar nombres de alumnos (por-doc, con fallback por campo "uid") ----------
    LaunchedEffect(classItem) {
        val ids = classItem?.assignedStudents ?: emptyList()
        if (ids.isEmpty()) { assignedNames = emptyMap(); return@LaunchedEffect }

        val map = mutableMapOf<String, String>()

        // Para cada UID:
        ids.forEach { studentUid ->
            // 1) users/{docId == uid} ?
            db.collection("users").document(studentUid).get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        val name = doc.getString("name")
                            ?: doc.getString("email")
                            ?: studentUid
                        map[studentUid] = name
                        assignedNames = map.toMap()
                    } else {
                        // 2) Fallback: buscar por campo uid
                        db.collection("users")
                            .whereEqualTo("uid", studentUid)
                            .limit(1)
                            .get()
                            .addOnSuccessListener { qs ->
                                val d = qs.documents.firstOrNull()
                                val name = d?.getString("name")
                                    ?: d?.getString("email")
                                    ?: studentUid
                                map[studentUid] = name
                                assignedNames = map.toMap()
                            }
                            .addOnFailureListener {
                                // 3) Último recurso: mostrar el UID
                                map[studentUid] = studentUid
                                assignedNames = map.toMap()
                            }
                    }
                }
                .addOnFailureListener {
                    // Problema leyendo el doc directo: usa UID en crudo
                    map[studentUid] = studentUid
                    assignedNames = map.toMap()
                }
        }
    }

    // ---------- Escuchar calificaciones ----------
    LaunchedEffect(classId) {
        db.collection("classes").document(classId)
            .collection("grades")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    scope.launch { snack.showSnackbar("Error notas: ${err.message}") }
                    return@addSnapshotListener
                }
                grades = snap?.documents?.map { d ->
                    GradeItem(
                        id = d.id,
                        studentId = d.getString("studentId") ?: "",
                        studentName = d.getString("studentName") ?: "",
                        score = (d.getDouble("score") ?: 0.0),
                        comment = d.getString("comment") ?: "",
                        createdAt = d.getTimestamp("createdAt"),
                        updatedAt = d.getTimestamp("updatedAt"),
                        updatedBy = d.getString("updatedBy") ?: ""
                    )
                } ?: emptyList()
            }
    }

    val isAdmin = role == "admin"
    val isProfessorOwner = classItem?.professorId == uid
    val canManage = isAdmin || isProfessorOwner
    val isStudent = role == "estudiante"

    val visibleGrades = remember(grades, isStudent, uid) {
        if (isStudent) grades.filter { it.studentId == uid } else grades
    }

    fun recomputeProgressFor(studentId: String) {
        val gs = grades.filter { it.studentId == studentId }
        val avg = if (gs.isNotEmpty()) gs.map { it.score }.average() else 0.0
        val passed = avg >= 70.0
        val docId = "${classId}_$studentId"
        val data = mapOf(
            "classId" to classId,
            "studentId" to studentId,
            "average" to avg,
            "passed" to passed,
            "updatedAt" to Timestamp.now()
        )
        db.collection("progress").document(docId).set(data, SetOptions.merge())
    }

    fun saveGrade(studentId: String, studentName: String, score: Double, comment: String) {
        val ref = db.collection("classes").document(classId).collection("grades")
        val now = Timestamp.now()

        if (editing == null) {
            val data = mapOf(
                "studentId" to studentId,
                "studentName" to studentName,
                "score" to score,
                "comment" to comment,
                "createdAt" to now,
                "updatedAt" to now,
                "updatedBy" to uid
            )
            ref.add(data)
                .addOnSuccessListener {
                    recomputeProgressFor(studentId)
                    scope.launch { snack.showSnackbar("Nota registrada") }
                }
                .addOnFailureListener { e ->
                    scope.launch { snack.showSnackbar("Error al guardar: ${e.message}") }
                }
        } else {
            val old = editing!!
            ref.document(old.id).update(
                mapOf(
                    "studentId" to studentId,
                    "studentName" to studentName,
                    "score" to score,
                    "comment" to comment,
                    "updatedAt" to now,
                    "updatedBy" to uid
                )
            ).addOnSuccessListener {
                recomputeProgressFor(studentId)
                scope.launch { snack.showSnackbar("Nota actualizada") }
            }.addOnFailureListener { e ->
                scope.launch { snack.showSnackbar("Error al actualizar: ${e.message}") }
            }
        }
        editing = null
        showAddEdit = false
    }

    fun deleteGrade(item: GradeItem) {
        db.collection("classes").document(classId)
            .collection("grades").document(item.id)
            .delete()
            .addOnSuccessListener {
                recomputeProgressFor(item.studentId)
                scope.launch { snack.showSnackbar("Nota eliminada") }
            }
            .addOnFailureListener { e ->
                scope.launch { snack.showSnackbar("Error al eliminar: ${e.message}") }
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(classItem?.title ?: "Clase") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snack) },
        floatingActionButton = {
            if (canManage) {
                FloatingActionButton(onClick = { editing = null; showAddEdit = true }) {
                    Icon(Icons.Filled.Grade, contentDescription = "Agregar nota")
                }
            }
        }
    ) { pad ->
        if (loading) {
            Box(Modifier.fillMaxSize().padding(pad), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier.padding(pad).padding(16.dp).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (isStudent) {
                val mine = visibleGrades
                val avg = if (mine.isNotEmpty()) mine.map { it.score }.average() else 0.0
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Tu promedio: ${"%.1f".format(avg)}", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.width(10.dp))
                    AssistChip(onClick = {}, label = { Text(if (avg >= 70) "Aprobado" else "En progreso") })
                }
            }

            Text("Calificaciones", style = MaterialTheme.typography.titleMedium)

            if (visibleGrades.isEmpty()) {
                Text(if (isStudent) "Aún no tienes calificaciones." else "Sin calificaciones.")
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(visibleGrades, key = { it.id }) { g ->
                        ElevatedCard(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp)) {
                                Text(
                                    text = if (isStudent) "Tu nota: ${g.score}" else "${g.studentName} — ${g.score}",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                if (g.comment.isNotBlank()) {
                                    Spacer(Modifier.height(4.dp))
                                    Text(g.comment, style = MaterialTheme.typography.bodyMedium)
                                }
                                Spacer(Modifier.height(6.dp))
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        "Actualizado: " + (g.updatedAt?.toDate()?.toString() ?: "-"),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Spacer(Modifier.weight(1f))
                                    if (canManage) {
                                        IconButton(onClick = { editing = g; showAddEdit = true }) {
                                            Icon(Icons.Filled.Edit, contentDescription = "Editar")
                                        }
                                        IconButton(onClick = { deleteGrade(g) }) {
                                            Icon(Icons.Filled.Delete, contentDescription = "Eliminar")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddEdit) {
        AddEditGradeDialog(
            assignedNames = assignedNames,
            editing = editing,
            onDismiss = { showAddEdit = false; editing = null },
            onSubmit = { studentId, score, comment ->
                val name = assignedNames[studentId] ?: studentId
                saveGrade(studentId, name, score, comment)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddEditGradeDialog(
    assignedNames: Map<String, String>,
    editing: GradeItem?,
    onDismiss: () -> Unit,
    onSubmit: (studentId: String, score: Double, comment: String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val pairs = remember(assignedNames) { assignedNames.entries.sortedBy { it.value.lowercase() } }

    var studentId by remember { mutableStateOf(editing?.studentId ?: pairs.firstOrNull()?.key.orEmpty()) }
    var scoreText by remember { mutableStateOf(editing?.score?.toString() ?: "") }
    var comment by remember { mutableStateOf(editing?.comment ?: "") }
    var error by remember { mutableStateOf<String?>(null) }

    // auto-selección cuando llegan alumnos
    LaunchedEffect(pairs) {
        if (studentId.isBlank() && pairs.isNotEmpty()) {
            studentId = pairs.first().key
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                error = null
                val sc = scoreText.toDoubleOrNull()
                if (studentId.isBlank() || sc == null) {
                    error = "Selecciona alumno y una nota válida."
                    return@TextButton
                }
                onSubmit(studentId, sc, comment.trim())
            }) { Text(if (editing == null) "Guardar" else "Actualizar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
        title = { Text(if (editing == null) "Registrar nota" else "Editar nota") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                    OutlinedTextField(
                        value = assignedNames[studentId] ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Alumno") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        pairs.forEach { e ->
                            DropdownMenuItem(
                                text = { Text(e.value) },
                                onClick = { studentId = e.key; expanded = false }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = scoreText,
                    onValueChange = { scoreText = it },
                    label = { Text("Nota (número)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("Comentario (opcional)") },
                    modifier = Modifier.fillMaxWidth()
                )

                if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error)
            }
        }
    )
}
