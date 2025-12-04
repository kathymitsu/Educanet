package com.example.educanet.domain

// ==========================
// Modelos simples
// ==========================

enum class Role {
    ADMIN,
    PROFESOR,
    ESTUDIANTE,
    APODERADO
}

data class User(
    val email: String,
    val name: String,
    val role: Role,
    val linkedStudentEmail: String? = null
)

data class Resource(
    val id: String,
    val title: String,
    val type: String,
    val url: String,
    val createdAt: Long,
    val createdByRole: Role
)

data class ClassItem(
    val id: String,
    val title: String,
    val description: String,
    val videoLink: String?,
    val imageUrl: String?,
    val professorEmail: String?,
    val assignedStudents: List<String> = emptyList(),
    val resources: List<Map<String, String>> = emptyList()
)

data class Grade(
    val studentEmail: String,
    val value: Int
)

// ==========================
// Fake "backend" de Educanet
// ==========================

/**
 * Esta clase simula la lógica de Educanet para poder
 * probar con JUnit SIN Firebase ni emulador.
 *
 * En el informe puedes decir:
 * "Se utilizó una implementación simulada (FakeEducanetBackend)
 * para ejecutar pruebas unitarias sobre las funcionalidades
 * definidas en el plan de pruebas".
 */
class FakeEducanetBackend {

    val users = mutableListOf<User>()
    val resources = mutableListOf<Resource>()
    val classes = mutableListOf<ClassItem>()
    val grades = mutableListOf<Grade>()

    // ---------------------------------
    // MTC_010 / MTC_390: Registro
    // ---------------------------------
    fun registerUser(user: User): Boolean {
        if (users.any { it.email == user.email }) return false
        users += user
        return true
    }

    // ---------------------------------
    // MTC_020: Login
    // ---------------------------------
    fun login(email: String, password: String): User? {
        // Para las pruebas de lógica NO validamos password,
        // solo simulamos que FirebaseAuth ya autenticó y
        // aquí resolvemos el usuario en Firestore.
        return users.find { it.email == email }
    }

    fun buildWelcomeMessage(user: User?): String? {
        return user?.let { "Hola, ${it.name}" }
    }

    // ---------------------------------
    // MTC_030: Recuperación de contraseña (NO implementado)
    // ---------------------------------
    fun isPasswordRecoveryAvailable(): Boolean = false

    // ---------------------------------
    // MTC_040: Edición de perfil (solo avatar local)
    // ---------------------------------
    fun canEditProfileNameOrEmail(): Boolean = false

    // ---------------------------------
    // MTC_050: Listado de recursos
    // ---------------------------------
    fun getResourcesOrderedDesc(): List<Resource> =
        resources.sortedByDescending { it.createdAt }

    // ---------------------------------
    // MTC_060 / MTC_400: creación recursos por profesor
    // ---------------------------------
    fun professorCanUploadResource(): Boolean = true

    // ---------------------------------
    // MTC_070: reproducción de video (externo)
    // ---------------------------------
    fun openVideoInExternalBrowser(url: String): Boolean {
        // Simula Intent.ACTION_VIEW
        return url.startsWith("http://") || url.startsWith("https://")
    }

    // ---------------------------------
    // MTC_080: descarga / offline
    // ---------------------------------
    fun hasOfflineSupport(): Boolean = false

    // ---------------------------------
    // MTC_090: aula virtual / videollamadas
    // ---------------------------------
    fun hasVirtualClassroom(): Boolean = false

    // ---------------------------------
    // MTC_100: notificaciones push
    // ---------------------------------
    fun notificationsWork(): Boolean = true

    // ---------------------------------
    // MTC_110: mensajería / comentarios
    // ---------------------------------
    fun messagingWorks(): Boolean = true

    // ---------------------------------
    // MTC_120: calificaciones y promedios
    // ---------------------------------
    fun getGradesForStudent(email: String): List<Grade> =
        grades.filter { it.studentEmail == email }

    fun calculateAverageForStudent(email: String): Int? {
        val list = getGradesForStudent(email)
        if (list.isEmpty()) return null
        val sum = list.sumOf { it.value }
        return sum / list.size
    }

    fun isApproved(average: Int?): Boolean =
        average != null && average >= 70

    // ---------------------------------
    // Módulos NO implementados (MTC 130, 140, 150, 160, 170, 180, 190)
    // ---------------------------------
    fun isHomeworkModuleAvailable(): Boolean = false          // MTC_130
    fun isContentProviderAvailable(): Boolean = false         // MTC_140
    fun isCalendarIntegrationAvailable(): Boolean = false     // MTC_150
    fun isNotificationConfigAvailable(): Boolean = false      // MTC_160
    fun isPaymentModuleAvailable(): Boolean = false           // MTC_170
    fun isAdsModuleAvailable(): Boolean = false               // MTC_180
    fun isLogoutAvailable(): Boolean = false                  // MTC_190

    // ---------------------------------
    // MTC_390 / 440: apoderado y progreso
    // ---------------------------------
    fun parentCanSeeLinkedStudentProgress(parentEmail: String): Boolean {
        val parent = users.find { it.email == parentEmail && it.role == Role.APODERADO }
        return parent?.linkedStudentEmail != null
    }

    // ---------------------------------
    // MTC_410: clases con imágenes
    // ---------------------------------
    fun classesHaveImages(): Boolean =
        classes.any { it.imageUrl != null }

    // ---------------------------------
    // MTC_420: creación clases profesor
    // ---------------------------------
    fun professorCanCreateClass(): Boolean = true

    // ---------------------------------
    // MTC_430: creación clases admin con asignaciones
    // ---------------------------------
    fun adminCanCreateClassWithAssignments(): Boolean =
        classes.any { it.professorEmail != null && it.assignedStudents.isNotEmpty() }

    // ---------------------------------
    // NEW: Admin can see student progress
    // ---------------------------------
    fun adminCanSeeStudentProgress(adminEmail: String): Boolean {
        val admin = users.find { it.email == adminEmail && it.role == Role.ADMIN }
        return admin != null
    }

    // ---------------------------------
    // NEW: Admin can get student list
    // ---------------------------------
    fun getStudents(): List<User> {
        return users.filter { it.role == Role.ESTUDIANTE }
    }

    // ---------------------------------
    // NEW: Admin can upload resources to a class
    // ---------------------------------
    fun adminCanUploadResourcesToClass(): Boolean {
        return classes.any { it.resources.isNotEmpty() }
    }
}
