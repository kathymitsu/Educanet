
package com.example.educanet.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Grade
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.educanet.item.ClassItem
import com.example.educanet.item.GradeItem
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassDetailScreen(
    classId: String,
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
) {
    val auth = remember { FirebaseAuth.getInstance() }
    val db = remember { FirebaseFirestore.getInstance() }
    val uid = auth.currentUser?.uid.orEmpty()

    val scope = rememberCoroutineScope()
    val snack = remember { SnackbarHostState() }

    var classItem by remember { mutableStateOf<ClassItem?>(null) }
    var role by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }

    // uid -> nombre del alumno
    var assignedNames by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    var grades by remember { mutableStateOf(listOf<GradeItem>()) }
    var showAddEdit by remember { mutableStateOf(false) }
    var editing: GradeItem? by remember { mutableStateOf(null) }

    // Profesores (para que el admin asigne uno)
    var professors by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var selectedProfessorId: String? by remember { mutableStateOf("") }

    // ---------- Cargar rol y clase ----------
    LaunchedEffect(uid, classId) {
        loading = true

        db.collection("users").document(uid).get()
            .addOnSuccessListener { role = it.getString("role") }
            .addOnFailureListener { e ->
                scope.launch { snack.showSnackbar("Error rol: ${e.message}") }
            }

        db.collection("classes").document(classId).addSnapshotListener { d, _ ->
            val ci = d?.toObject(ClassItem::class.java)
            classItem = ci
            selectedProfessorId = ci?.professorId
            loading = false
        }
    }

    // ---------- Cargar nombres de alumnos ----------
    LaunchedEffect(classItem) {
        val ids = classItem?.assignedStudents ?: emptyList()
        if (ids.isEmpty()) {
            assignedNames = emptyMap()
            return@LaunchedEffect
        }

        val map = mutableMapOf<String, String>()

        ids.forEach { studentUid ->
            db.collection("users").document(studentUid).get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        val name = doc.getString("name")
                            ?: doc.getString("email")
                            ?: studentUid
                        map[studentUid] = name
                        assignedNames = map.toMap()
                    } else {
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
                                map[studentUid] = studentUid
                                assignedNames = map.toMap()
                            }
                    }
                }
                .addOnFailureListener {
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
                grades = snap?.documents?.mapNotNull { it.toObject(GradeItem::class.java) } ?: emptyList()
            }
    }

    // ---------- Lista de profesores (solo admin) ----------
    LaunchedEffect(role) {
        if (role != "admin") return@LaunchedEffect

        try {
            val snap = db.collection("users")
                .whereEqualTo("role", "profesor")
                .get()
                .await()

            professors = snap.documents.map { d ->
                val pid = d.id
                val name = d.getString("name")
                    ?: d.getString("email")
                    ?: pid
                pid to name
            }.sortedBy { it.second.lowercase() }

        } catch (e: Exception) {
            scope.launch { snack.showSnackbar("Error cargando profesores: ${e.message}") }
        }
    }

    // ---------- Reglas de permisos ----------
    val isAdmin = role == "admin"
    val isProfessor = role == "profesor"
    val isStudent = role == "estudiante"

    val canEditClass = isAdmin || (isProfessor && classItem?.professorId == uid)
    val canManageGrades = isProfessor

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
            val data = GradeItem(
                studentId = studentId,
                studentName = studentName,
                score = score,
                comment = comment,
                createdAt = now,
                updatedAt = now,
                updatedBy = uid
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

    // Guardar profesor asignado (solo admin)
    fun saveProfessorAssignment() {
        val profId = selectedProfessorId ?: return
        if (profId.isBlank()) return

        db.collection("classes").document(classId)
            .update("professorId", profId)
            .addOnSuccessListener {
                classItem = classItem?.copy(professorId = profId)
                scope.launch { snack.showSnackbar("Profesor asignado correctamente.") }
            }
            .addOnFailureListener { e ->
                scope.launch { snack.showSnackbar("Error al asignar profesor: ${e.message}") }
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
                },
                actions = {
                    if (canEditClass) {
                        IconButton(onClick = { onEdit(classId) }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Editar")
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snack) },
        floatingActionButton = {
            // Solo PROFESOR ve el FAB para agregar notas
            if (canManageGrades) {
                FloatingActionButton(onClick = { editing = null; showAddEdit = true }) {
                    Icon(Icons.Filled.Grade, contentDescription = "Agregar nota")
                }
            }
        }
    ) { pad ->
        if (loading) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(pad),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .padding(pad)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // -------- Bloque de asignación de profesor (ADMIN) --------
            if (isAdmin) {
                Text(
                    text = "Profesor asignado",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                val currentName: String = professors.firstOrNull {
                    it.first == selectedProfessorId
                }?.second
                    ?: if (selectedProfessorId.isNullOrBlank()) "Sin profesor" else selectedProfessorId!!

                var expanded by remember { mutableStateOf(false) }

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = currentName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Profesor") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        professors.forEach { (id, name) ->
                            DropdownMenuItem(
                                text = { Text(name) },
                                onClick = {
                                    selectedProfessorId = id
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                Button(
                    onClick = { saveProfessorAssignment() },
                    enabled = !selectedProfessorId.isNullOrBlank()
                ) {
                    Text("Guardar profesor")
                }

                Divider()
            }
            // ----------------------------------------------------------

            if (isStudent) {
                val mine = visibleGrades
                val avg = if (mine.isNotEmpty()) mine.map { it.score }.average() else 0.0
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Tu promedio: ${"%.1f".format(avg)}",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.width(10.dp))
                    AssistChip(
                        onClick = {},
                        label = { Text(if (avg >= 70) "Aprobado" else "En progreso") }
                    )
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
                                    text = if (isStudent)
                                        "Tu nota: ${g.score}"
                                    else
                                        "${g.studentName} — ${g.score}",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                if (g.comment.isNotBlank()) {
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        g.comment,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                Spacer(Modifier.height(6.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        "Actualizado: " + (g.updatedAt?.toDate()?.toString() ?: "-"),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Spacer(Modifier.weight(1f))
                                    // Solo profesor puede editar / borrar
                                    if (canManageGrades) {
                                        IconButton(onClick = {
                                            editing = g
                                            showAddEdit = true
                                        }) {
                                            Icon(Icons.Filled.Edit, contentDescription = "Editar")
                                        }
                                        IconButton(onClick = { deleteGrade(g) }) {
                                            Icon(
                                                Icons.Filled.Delete,
                                                contentDescription = "Eliminar"
                                            )
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
    val pairs = remember(assignedNames) {
        assignedNames.entries.sortedBy { it.value.lowercase() }
    }

    var studentId by remember {
        mutableStateOf(editing?.studentId ?: pairs.firstOrNull()?.key.orEmpty())
    }
    var scoreText by remember { mutableStateOf(editing?.score?.toString() ?: "") }
    var comment by remember { mutableStateOf(editing?.comment ?: "") }
    var error by remember { mutableStateOf<String?>(null) }

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
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = assignedNames[studentId] ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Alumno") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded)
                        },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        pairs.forEach { e ->
                            DropdownMenuItem(
                                text = { Text(e.value) },
                                onClick = {
                                    studentId = e.key
                                    expanded = false
                                }
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

                if (error != null) {
                    Text(error!!, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    )
}
