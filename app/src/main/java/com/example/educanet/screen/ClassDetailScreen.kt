package com.example.educanet.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Grade
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.educanet.item.ClassItem
import com.example.educanet.item.GradeItem
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassDetailScreen(
    classId: String,
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    onOpenProgress: (String) -> Unit, // <--- para ver el progreso de un alumno
) {
    val auth = remember { FirebaseAuth.getInstance() }
    val db = remember { FirebaseFirestore.getInstance() }
    val uid = auth.currentUser?.uid.orEmpty()

    val scope = rememberCoroutineScope()
    val snack = remember { SnackbarHostState() }

    var classItem by remember { mutableStateOf<ClassItem?>(null) }
    var role by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }

    var isInCart by remember { mutableStateOf(false) }

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

        if (uid.isNotBlank()) {
            db.collection("users").document(uid).get()
                .addOnSuccessListener { role = it.getString("role") }
                .addOnFailureListener { e ->
                    scope.launch { snack.showSnackbar("Error rol: ${e.message}") }
                }

            db.collection("users").document(uid).collection("cart").document(classId)
                .addSnapshotListener { snap, _ ->
                    isInCart = snap?.exists() == true
                }
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
                    }
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
    val isEnrolled = classItem?.assignedStudents?.contains(uid) == true

    val canEditClass = isAdmin || (isProfessor && classItem?.professorId == uid)
    val canManageGrades = isAdmin || (isProfessor && classItem?.professorId == uid)

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

    // ++++++++++ FUNCIÓN AÑADIDA PARA CORREGIR EL ERROR ++++++++++
    fun deleteGrade(grade: GradeItem) {
        if (grade.id.isBlank()) {
            scope.launch { snack.showSnackbar("Error: ID de calificación inválido.") }
            return
        }

        scope.launch {
            try {
                // Eliminar el documento de la calificación
                db.collection("classes").document(classId)
                    .collection("grades").document(grade.id)
                    .delete()
                    .await()

                // Recalcular el progreso del estudiante después de borrar
                recomputeProgressFor(grade.studentId)

                snack.showSnackbar("Calificación borrada exitosamente.")
            } catch (e: Exception) {
                snack.showSnackbar("Error al borrar la calificación: ${e.message}")
            }
        }
    }
    // +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

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

            // VISTA PARA ALUMNO NO INSCRITO
            if (isStudent && !isEnrolled) {
                Text(classItem?.description ?: "", style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(16.dp))

                val format = NumberFormat.getCurrencyInstance(Locale("es", "MX"))
                Text(
                    text = format.format(classItem?.price ?: 0.0),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(16.dp))

                if (isInCart) {
                    Button(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) {
                        Text("Ya está en el carrito")
                    }
                } else {
                    Button(
                        onClick = {
                            scope.launch {
                                try {
                                    val classRef = db.collection("classes").document(classId)
                                    val cartRef = db.collection("users").document(uid).collection("cart").document(classId)

                                    db.runTransaction { transaction ->
                                        val snapshot = transaction.get(classRef)
                                        val currentSeats = snapshot.getLong("availableSeats") ?: 0
                                        if (currentSeats > 0) {
                                            transaction.update(classRef, "availableSeats", FieldValue.increment(-1))

                                            val cartItem = hashMapOf(
                                                "classId" to classId,
                                                "classTitle" to classItem?.title,
                                                "description" to classItem?.description,
                                                "price" to classItem?.price,
                                                "imageUrl" to classItem?.imageUrl,
                                                "availableSeats" to (currentSeats - 1),
                                                "createdAt" to Timestamp.now()
                                            )
                                            transaction.set(cartRef, cartItem)
                                        } else {
                                            throw FirebaseFirestoreException("No more seats available", FirebaseFirestoreException.Code.ABORTED)
                                        }
                                    }.await()
                                    scope.launch { snack.showSnackbar("Añadido al carrito") }
                                } catch (e: Exception) {
                                    scope.launch { snack.showSnackbar("Error al añadir al carrito: ${e.message}") }
                                }
                            }
                        },
                        enabled = (classItem?.availableSeats ?: 0) > 0,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Añadir al carrito")
                    }
                }

            } else {
                // VISTA PARA ADMIN, PROFESOR O ALUMNO INSCRITO
                if (isAdmin) {
                    Text("Profesor asignado", style = MaterialTheme.typography.titleMedium)
                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                        OutlinedTextField(
                            value = professors.firstOrNull { it.first == selectedProfessorId }?.second ?: "Sin profesor",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Profesor") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
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
                    Button(onClick = {
                        scope.launch {
                            try {
                                db.collection("classes").document(classId)
                                    .update("professorId", selectedProfessorId ?: "")
                                    .await()
                                snack.showSnackbar("Profesor guardado correctamente.")
                            } catch (e: Exception) {
                                snack.showSnackbar("Error al guardar: ${e.message}")
                            }
                        }
                    }) {
                        Text("Guardar profesor")
                    }
                    Divider()
                }

                if (isStudent) {
                    val mine = visibleGrades
                    val avg = if (mine.isNotEmpty()) mine.map { it.score }.average() else 0.0
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Tu promedio: ${String.format("%.1f", avg)}", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.width(10.dp))
                        AssistChip(onClick = {}, label = { Text(if (avg >= 70) "Aprobado" else "En progreso") })
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("Calificaciones", style = MaterialTheme.typography.titleMedium)

                    if (visibleGrades.isEmpty()) {
                        Text("Aún no tienes calificaciones.")
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(visibleGrades, key = { it.id }) { g ->
                                ElevatedCard(Modifier.fillMaxWidth()) {
                                    Column(Modifier.padding(12.dp)) {
                                        Text(
                                            text = "Nota: ${g.score}",
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        if (g.comment.isNotBlank()) {
                                            Spacer(Modifier.height(4.dp))
                                            Text(g.comment, style = MaterialTheme.typography.bodyMedium)
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // VISTA PARA PROFESOR / ADMIN: LISTA DE ALUMNOS
                    Text("Alumnos inscritos", style = MaterialTheme.typography.titleMedium)
                    val students = classItem?.assignedStudents ?: emptyList()

                    if (students.isEmpty()) {
                        Text("No hay alumnos inscritos en este curso.")
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(students) { studentId ->
                                val studentName = assignedNames[studentId] ?: "Alumno (ID: ${studentId.take(6)}...)"
                                ElevatedCard(Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(studentName, style = MaterialTheme.typography.bodyLarge)
                                        OutlinedButton(onClick = { onOpenProgress(studentId) }) {
                                            Text("Ver/Editar Progreso")
                                            Spacer(Modifier.width(4.dp))
                                            Icon(Icons.Default.TrendingUp, contentDescription = "Progreso")
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
}