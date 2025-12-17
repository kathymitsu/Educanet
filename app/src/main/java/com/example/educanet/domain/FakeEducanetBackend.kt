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
    val price: Double,
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

class FakeEducanetBackend {

    val users = mutableListOf<User>()
    val resources = mutableListOf<Resource>()
    val classes = mutableListOf<ClassItem>()
    val grades = mutableListOf<Grade>()
    val cart = mutableListOf<ClassItem>()

    // ---------------------------------
    // Registro y Login
    // ---------------------------------
    fun registerUser(user: User): Boolean {
        if (users.any { it.email == user.email }) return false
        users += user
        return true
    }

    fun login(email: String, password: String): User? {
        return users.find { it.email == email }
    }

    fun buildWelcomeMessage(user: User?): String? {
        return user?.let { "Hola, ${it.name}" }
    }

    // ---------------------------------
    // Calificaciones y Progreso
    // ---------------------------------
    fun getGradesForStudent(email: String): List<Grade> =
        grades.filter { it.studentEmail == email }

    fun calculateAverageForStudent(email: String): Int? {
        val list = getGradesForStudent(email)
        if (list.isEmpty()) return null
        return list.sumOf { it.value } / list.size
    }

    fun isApproved(average: Int?): Boolean = average != null && average >= 70

    // ---------------------------------
    // Carrito y Pagos (MTC_170)
    // ---------------------------------
    fun isPaymentModuleAvailable(): Boolean = true

    fun addToCart(classItem: ClassItem, user: User): Boolean {
        if (user.role != Role.ESTUDIANTE) return false
        if (cart.any { it.id == classItem.id }) return false
        if (classes.find { it.id == classItem.id }?.assignedStudents?.contains(user.email) == true) return false
        cart.add(classItem)
        return true
    }

    fun getCartTotal(): Double = cart.sumOf { it.price }

    fun checkout(user: User): Boolean {
        if (user.role != Role.ESTUDIANTE || cart.isEmpty()) return false
        cart.forEach { classInCart ->
            val index = classes.indexOfFirst { it.id == classInCart.id }
            if (index != -1) {
                val originalClass = classes[index]
                val updatedClass = originalClass.copy(assignedStudents = originalClass.assignedStudents + user.email)
                classes[index] = updatedClass
            }
        }
        cart.clear()
        return true
    }
    
    // ---------------------------------
    // Funcionalidades de Administrador
    // ---------------------------------
    fun getStudents(): List<User> = users.filter { it.role == Role.ESTUDIANTE }

    fun adminCanSeeStudentProgress(adminEmail: String): Boolean {
        return users.any { it.email == adminEmail && it.role == Role.ADMIN }
    }

    fun adminCanUploadResourcesToClass(): Boolean {
        return classes.any { it.resources.isNotEmpty() }
    }

    fun deleteUser(userEmail: String, adminEmail: String): Boolean {
        val admin = users.find { it.email == adminEmail }
        if (admin?.role != Role.ADMIN) return false
        return users.removeIf { it.email == userEmail }
    }

    // ---------------------------------
    // Otras funcionalidades (disponibilidad)
    // ---------------------------------
    fun getResourcesOrderedDesc(): List<Resource> = resources.sortedByDescending { it.createdAt }
    fun isPasswordRecoveryAvailable(): Boolean = false
    fun canEditProfileNameOrEmail(): Boolean = false
    fun professorCanUploadResource(): Boolean = true
    fun openVideoInExternalBrowser(url: String): Boolean = url.startsWith("http")
    fun hasOfflineSupport(): Boolean = false
    fun hasVirtualClassroom(): Boolean = false
    fun notificationsWork(): Boolean = true
    fun messagingWorks(): Boolean = true
    fun isHomeworkModuleAvailable(): Boolean = false
    fun isContentProviderAvailable(): Boolean = false
    fun isCalendarIntegrationAvailable(): Boolean = false
    fun isNotificationConfigAvailable(): Boolean = false
    fun isAdsModuleAvailable(): Boolean = false
    fun isLogoutAvailable(): Boolean = true
    fun parentCanSeeLinkedStudentProgress(parentEmail: String): Boolean {
        val parent = users.find { it.email == parentEmail && it.role == Role.APODERADO }
        return parent?.linkedStudentEmail != null
    }
    fun classesHaveImages(): Boolean = classes.any { it.imageUrl != null }
    fun professorCanCreateClass(): Boolean = true
    fun adminCanCreateClassWithAssignments(): Boolean = classes.any { it.professorEmail != null && it.assignedStudents.isNotEmpty() }
}
