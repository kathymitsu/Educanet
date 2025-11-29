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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

data class ReviewItem(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val rating: Int = 5,
    val comment: String = "",
    val createdAt: Timestamp? = null
)

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

    var assignedNames by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    var grades by remember { mutableStateOf(listOf<GradeItem>()) }
    var showAddEdit by remember { mutableStateOf(false) }
    var editing: GradeItem? by remember { mutableStateOf(null) }

    var reviews by remember { mutableStateOf(listOf<ReviewItem>()) }
    var myRatingText by remember { mutableStateOf("5") }
    var myComment by remember { mutableStateOf("") }
    var reviewError by remember { mutableStateOf<String?>(null) }

    // --------- Cargar rol y clase ----------
    LaunchedEffect(uid, classId) {
        loading = true

        db.collection("users").document(uid).get()
            .addOnSuccessListener { role = it.getString("role") }
            .addOnFailureListener { e ->
                scope.launch { snack.showSnackbar("Error al obtener rol: ${e.message}") }
            }

        db.collection("classes").document(classId).get()
            .addOnSuccessListener { d -> classItem = d.toObject(ClassItem::class.java) }
            .addOnFailureListener { e ->
                scope.launch { snack.showSnackbar("Error al cargar clase: ${e.message}") }
            }
            .addOnCompleteListener { loading = false }
    }

    // --------- Cargar nombres de alumnos ----------
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

    // --------- Escuchar notas ----------
    LaunchedEffect(classId) {
        db.collection("classes").document(classId)
            .collection("grades")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    scope.launch { snack.showSnackbar("Error al cargar notas: ${err.message}") }
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

    // --------- Escuchar reseñas ----------
    LaunchedEffect(classId) {
        db.collection("classes").document(classId)
            .collection("reviews")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    scope.launch { snack.showSnackbar("Error al cargar reseñas: ${err.message}") }
                    return@addSnapshotListener
                }
                reviews = snap?.documents?.map { d ->
                    ReviewItem(
                        id = d.id,
                        userId = d.getString("userId") ?: "",
                        userName = d.getString("userName") ?: "",
                        rating = (d.getLong("rating") ?: 5L).toInt(),
                        comment = d.getString("comment") ?: "",
                        createdAt = d.getTimestamp("createdAt")
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

    // --------- Progreso académico ----------
    fun recomputeProgressFor(studentId: String) {
        val gs = grades.filter { it.studentId == studentId }
        val avg = if (gs.isNotEmpty()) gs.map { it.score }.average() else 0.0
        val status = if (avg >= 70.0) "aprobado" else "en curso"
        val docId = "${classId}_$studentId"

        val data = mapOf(
            "classId" to classId,
            "classTitle" to (classItem?.title ?: "Clase sin título"),
            "userId" to studentId,
            "score" to avg,
            "status" to status,
            "updatedAt" to Timestamp.now()
        )

        db.collection("progress")
            .document(docId)
            .set(data, SetOptions.merge())
    }

    // --------- Guardar nota ----------
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
                    scope.launch { snack.showSnackbar("Nota registrada correctamente") }
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
                scope.launch { snack.showSnackbar("Nota actualizada correctamente") }
            }.addOnFailureListener { e ->
                scope.launch { snack.showSnackbar("Error al actualizar: ${e.message}") }
            }
        }
        editing = null
        showAddEdit = false
    }

    // --------- Eliminar nota ----------
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

    // --------- Agregar clase al carrito ----------
    fun addToCart() {
        if (uid.isBlank()) {
            scope.launch { snack.showSnackbar("Debes iniciar sesión para usar el carrito.") }
            return
        }
        val c = classItem ?: return
        val userCartRef = db.collection("users").document(uid).collection("cart")
        val now = Timestamp.now()

        val price = c.price

        val data = mapOf(
            "classId" to classId,
            "classTitle" to c.title,
            "price" to price,
            "imageUrl" to c.imageUrl,
            "createdAt" to now
        )

        userCartRef.document(classId)
            .set(data)
            .addOnSuccessListener {
                scope.launch { snack.showSnackbar("Clase agregada al carrito.") }
            }
            .addOnFailureListener { e ->
                scope.launch { snack.showSnackbar("Error al agregar al carrito: ${e.message}") }
            }
    }

    // --------- Guardar reseña ----------
    fun saveReview() {
        reviewError = null
        val rating = myRatingText.toIntOrNull()
        if (rating == null || rating !in 1..5) {
            reviewError = "La calificación debe ser un número entre 1 y 5."
            return
        }
        if (myComment.isBlank()) {
            reviewError = "Escribe un comentario."
            return
        }

        val usersRef = db.collection("users").document(uid)
        val now = Timestamp.now()

        usersRef.get()
            .addOnSuccessListener { u ->
                val name = u.getString("name") ?: u.getString("email") ?: "Usuario"
                val ref = db.collection("classes").document(classId)
                    .collection("reviews")

                val data = mapOf(
                    "userId" to uid,
                    "userName" to name,
                    "rating" to rating,
                    "comment" to myComment.trim(),
                    "createdAt" to now
                )

                ref.add(data)
                    .addOnSuccessListener {
                        myComment = ""
                        myRatingText = "5"
                        reviewError = null
                        scope.launch { snack.showSnackbar("Reseña enviada.") }
                    }
                    .addOnFailureListener { e ->
                        reviewError = "Error al enviar reseña: ${e.message}"
                    }
            }
            .addOnFailureListener { e ->
                reviewError = "Error al obtener datos de usuario: ${e.message}" }
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
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(pad),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .padding(pad)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Cupos (stock)
            val availableSeats = classItem?.availableSeats?.toInt()
            if (availableSeats != null) {
                Text("Cupos disponibles: $availableSeats")
            } else {
                Text("Cupos disponibles: ilimitados")
            }

            if (role == "estudiante" || role == "apoderado") {
                Button(onClick = { addToCart() }) {
                    Text("Agregar al carrito")
                }
            }

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
                        label = { Text(if (avg >= 70) "Aprobado" else "En curso") }
                    )
                }
            }

            Text("Calificaciones", style = MaterialTheme.typography.titleMedium)

            if (visibleGrades.isEmpty()) {
                Text(if (isStudent) "Aún no tienes calificaciones." else "Sin calificaciones registradas.")
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.heightIn(max = 220.dp)
                ) {
                    items(visibleGrades, key = { it.id }) { g ->
                        ElevatedCard(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp)) {
                                Text(
                                    text = if (isStudent) {
                                        "Tu nota: ${g.score}"
                                    } else {
                                        "${g.studentName} — ${g.score}"
                                    },
                                    style = MaterialTheme.typography.titleMedium
                                )
                                if (g.comment.isNotBlank()) {
                                    Spacer(Modifier.height(4.dp))
                                    Text(g.comment, style = MaterialTheme.typography.bodyMedium)
                                }
                                Spacer(Modifier.height(6.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        "Actualizado: " +
                                                (g.updatedAt?.toDate()?.toString() ?: "-"),
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

            Divider()

            Text("Reseñas de la clase", style = MaterialTheme.typography.titleMedium)

            if (reviews.isEmpty()) {
                Text("Aún no hay reseñas.")
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.heightIn(max = 220.dp)
                ) {
                    items(reviews, key = { it.id }) { r ->
                        ElevatedCard(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp)) {
                                Text("${r.userName} — ${r.rating}★")
                                if (r.comment.isNotBlank()) {
                                    Spacer(Modifier.height(4.dp))
                                    Text(r.comment)
                                }
                            }
                        }
                    }
                }
            }

            if (role == "estudiante" || role == "apoderado") {
                Spacer(Modifier.height(8.dp))
                Text("Escribe tu reseña", style = MaterialTheme.typography.titleSmall)
                OutlinedTextField(
                    value = myRatingText,
                    onValueChange = { myRatingText = it },
                    label = { Text("Calificación (1 a 5)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = myComment,
                    onValueChange = { myComment = it },
                    label = { Text("Comentario") },
                    modifier = Modifier.fillMaxWidth()
                )
                if (reviewError != null) {
                    Text(reviewError!!, color = MaterialTheme.colorScheme.error)
                }
                Button(
                    onClick = { saveReview() },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Enviar reseña")
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
                    error = "Selecciona un alumno y una nota válida."
                    return@TextButton
                }
                onSubmit(studentId, sc, comment.trim())
            }) {
                Text(if (editing == null) "Guardar" else "Actualizar")
            }
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
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
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
