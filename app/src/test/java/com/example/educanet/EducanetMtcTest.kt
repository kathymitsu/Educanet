package com.example.educanet

import com.example.educanet.domain.ClassItem
import com.example.educanet.domain.FakeEducanetBackend
import com.example.educanet.domain.Grade
import com.example.educanet.domain.Resource
import com.example.educanet.domain.Role
import com.example.educanet.domain.User
import com.example.educanet.util.AuthValidators
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class EducanetMtcTest {

    private lateinit var backend: FakeEducanetBackend
    private lateinit var student: User
    private lateinit var professor: User
    private lateinit var admin: User

    @Before
    fun setup() {
        backend = FakeEducanetBackend()
        student = User("katherine@educanet.cl", "Katherine", Role.ESTUDIANTE)
        professor = User("nicolas@profesor.cl", "Nicolas", Role.PROFESOR)
        admin = User("isabel@admineducanet.cl", "Isabel", Role.ADMIN)
        backend.registerUser(student)
        backend.registerUser(professor)
        backend.registerUser(admin)
    }

    // region User and Auth Tests

    @Test
    fun `MTC_010 - registro de usuario exitoso`() {
        val newUser = User("nuevo@educanet.cl", "Nuevo", Role.ESTUDIANTE)
        val result = backend.registerUser(newUser)
        assertTrue("El usuario debería registrarse correctamente", result)
        assertTrue(backend.users.any { it.email == "nuevo@educanet.cl" })
    }

    @Test
    fun `MTC_020 - inicio de sesion valido`() {
        val user = backend.login(student.email, "123456")
        val welcome = backend.buildWelcomeMessage(user)
        assertNotNull("Debería devolver un usuario válido", user)
        assertEquals("Hola, Katherine", welcome)
    }

    @Test
    fun `MTC_490 - validacion de correo de profesor`() {
        val validProfessorEmail = "profesor@profesor.cl"
        val invalidProfessorEmail = "profesor@gmail.com"

        assertNull(AuthValidators.validateEmail(validProfessorEmail))
        assertEquals(
            "El dominio del correo no es válido.",
            AuthValidators.validateEmail(invalidProfessorEmail)?.message
        )
    }

    // endregion

    // region Feature Availability Tests

    @Test fun `MTC_030 - recuperacion de contrasena no implementada`() {
        assertFalse(backend.isPasswordRecoveryAvailable())
    }

    @Test fun `MTC_040 - edicion de perfil limitada`() {
        assertFalse(backend.canEditProfileNameOrEmail())
    }

    @Test fun `MTC_080 - descarga para offline no disponible`() {
        assertFalse(backend.hasOfflineSupport())
    }

    @Test fun `MTC_090 - aula virtual no implementada`() {
        assertFalse(backend.hasVirtualClassroom())
    }

    // endregion

    // region Resource and Class Tests

    @Test
    fun `MTC_050 - lista de recursos ordenada`() {
        backend.resources.addAll(listOf(
            Resource("1", "Antiguo", "PDF", "url1", 1000L, Role.PROFESOR),
            Resource("2", "Nuevo", "Video", "url2", 2000L, Role.PROFESOR)
        ))
        val ordered = backend.getResourcesOrderedDesc()
        assertEquals("Nuevo", ordered.first().title)
    }

    @Test
    fun `MTC_060 y MTC_400 - subida de recurso por profesor`() {
        assertTrue("Los profesores deberían poder crear recursos", backend.professorCanUploadResource())
    }

    @Test
    fun `MTC_420 - creacion de clases por profesor`() {
        assertTrue("El profesor debería poder crear clases", backend.professorCanCreateClass())
    }

    // endregion

    // region Grades and Progress Tests

    @Test
    fun `MTC_120 - visualizacion de calificaciones`() {
        backend.grades.addAll(listOf(
            Grade(student.email, 80),
            Grade(student.email, 60)
        ))
        val avg = backend.calculateAverageForStudent(student.email)
        assertEquals(70, avg)
        assertTrue("Con 70 debería estar aprobado", backend.isApproved(avg))
    }

    // endregion

    // region Cart and Checkout Tests

    @Test
    fun `MTC_170 - procesamiento de pago con carrito`() {
        val classToBuy = ClassItem("c1", "Kotlin Básico", "...", 100.0, null, null, professor.email)
        backend.classes.add(classToBuy)

        // 1. Student adds a class to the cart
        val added = backend.addToCart(classToBuy, student)
        assertTrue(added)
        assertEquals(1, backend.cart.size)
        assertEquals(100.0, backend.getCartTotal(), 0.0)

        // 2. Student checks out
        val checkedOut = backend.checkout(student)
        assertTrue(checkedOut)

        // 3. Cart is now empty and student is enrolled
        assertTrue(backend.cart.isEmpty())
        val updatedClass = backend.classes.find { it.id == classToBuy.id }
        assertTrue(updatedClass?.assignedStudents?.contains(student.email) == true)
    }

    // endregion

    // region Admin-specific Tests

    @Test
    fun `MTC_450 - admin ve lista de estudiantes`() {
        val students = backend.getStudents()
        assertEquals(1, students.size)
        assertEquals(student.email, students.first().email)
    }

    @Test
    fun `MTC_460 - admin ve progreso de estudiantes`() {
        assertTrue(
            "Admin debería poder ver el progreso de cualquier estudiante",
            backend.adminCanSeeStudentProgress(admin.email)
        )
    }

    @Test
    fun `MTC_470 - admin elimina un usuario`() {
        val initialUserCount = backend.users.size
        val deleted = backend.deleteUser(student.email, admin.email)
        assertTrue(deleted)
        assertEquals(initialUserCount - 1, backend.users.size)
        assertNull(backend.users.find { it.email == student.email })
    }

    @Test
    fun `MTC_470 - non-admin cannot delete a user`() {
        val initialUserCount = backend.users.size
        val deleted = backend.deleteUser(student.email, professor.email) // Professor tries to delete
        assertFalse(deleted)
        assertEquals(initialUserCount, backend.users.size)
    }

    @Test
    fun `MTC_480 - admin puede subir recursos a una clase`() {
        val newClass = ClassItem(
            id = "c1", title = "Clase con Recursos", description = "...", price = 0.0,
            videoLink = null, imageUrl = null, professorEmail = professor.email,
            resources = listOf(mapOf("name" to "recurso.pdf", "url" to "http://example.com/recurso.pdf"))
        )
        backend.classes.add(newClass)

        assertTrue(backend.adminCanUploadResourcesToClass())
    }

    // endregion
}
