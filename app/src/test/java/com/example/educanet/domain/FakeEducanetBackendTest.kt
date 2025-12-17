package com.example.educanet.domain

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito

class FakeEducanetBackendTest {

    private lateinit var backend: FakeEducanetBackend
    private lateinit var backendSpy: FakeEducanetBackend

    @Before
    fun setUp() {
        // Inicializamos un backend limpio y un spy para CADA prueba.
        // Esto garantiza que los tests no compartan estado.
        backend = FakeEducanetBackend()
        backendSpy = Mockito.spy(backend)
    }

    // region User Registration & Login Tests

    @Test
    fun `MTC_010 registerUser succeeds for new user`() {
        val newUser = User("test@example.com", "Test User", Role.ESTUDIANTE)
        val result = backend.registerUser(newUser)
        assertTrue(result)
        assertEquals(1, backend.users.size)
        assertEquals(newUser, backend.users.first())
    }

    @Test
    fun `MTC_010 registerUser fails for existing user`() {
        val existingUser = User("test@example.com", "Test User", Role.ESTUDIANTE)
        backend.registerUser(existingUser) // Estado inicial para esta prueba

        val newUser = User("test@example.com", "Another User", Role.PROFESOR)
        val result = backend.registerUser(newUser)

        assertFalse(result)
        assertEquals(1, backend.users.size) // No debería añadir el duplicado
    }

    @Test
    fun `MTC_020 login succeeds for existing user`() {
        val user = User("test@example.com", "Test User", Role.ESTUDIANTE)
        backend.registerUser(user) // Estado inicial para esta prueba

        val loggedInUser = backend.login("test@example.com", "password")
        assertNotNull(loggedInUser)
        assertEquals(user, loggedInUser)
    }

    // endregion

    // region Grades and Progress Tests

    @Test
    fun `MTC_120 calculateAverageForStudent calls getGradesForStudent`() {
        val studentEmail = "student@test.com"
        backendSpy.grades.add(Grade(studentEmail, 100))

        backendSpy.calculateAverageForStudent(studentEmail)

        // Verificamos que el método que calcula el promedio llame al que obtiene las notas
        Mockito.verify(backendSpy).getGradesForStudent(studentEmail)
    }

    @Test
    fun `MTC_120 isApproved returns true for average greater or equal to 70`() {
        assertTrue(backend.isApproved(70))
        assertTrue(backend.isApproved(100))
    }

    @Test
    fun `MTC_120 isApproved returns false for average less than 70`() {
        assertFalse(backend.isApproved(69))
    }

    // endregion

    // region Cart and Checkout Tests (MTC_170)

    @Test
    fun `MTC_170 addToCart succeeds for student`() {
        // Arrange: Preparamos el estado específico para esta prueba
        val studentUser = User("student@test.com", "Student", Role.ESTUDIANTE)
        val class1 = ClassItem("c1", "Kotlin Básico", "...", 100.0, null, null, "prof@test.com")
        backend.registerUser(studentUser)
        backend.classes.add(class1)

        // Act
        val result = backend.addToCart(class1, studentUser)

        // Assert
        assertTrue(result)
        assertEquals(1, backend.cart.size)
        assertEquals(100.0, backend.getCartTotal(), 0.0)
    }

    @Test
    fun `MTC_170 addToCart fails for non-student`() {
        val professorUser = User("prof@test.com", "Professor", Role.PROFESOR)
        val class1 = ClassItem("c1", "Kotlin Básico", "...", 100.0, null, null, "prof@test.com")
        backend.registerUser(professorUser)
        backend.classes.add(class1)

        val result = backend.addToCart(class1, professorUser)

        assertFalse(result)
        assertTrue(backend.cart.isEmpty())
    }

    @Test
    fun `MTC_170 addToCart avoids duplicates`() {
        val studentUser = User("student@test.com", "Student", Role.ESTUDIANTE)
        val class1 = ClassItem("c1", "Kotlin Básico", "...", 100.0, null, null, "prof@test.com")
        backend.registerUser(studentUser)
        backend.classes.add(class1)

        backend.addToCart(class1, studentUser) // primera vez
        val result = backend.addToCart(class1, studentUser) // segunda vez

        assertFalse(result)
        assertEquals(1, backend.cart.size)
    }

    @Test
    fun `MTC_170 getCartTotal calculates correctly`() {
        val studentUser = User("student@test.com", "Student", Role.ESTUDIANTE)
        val class1 = ClassItem("c1", "Kotlin", "...", 100.0, null, null, "p")
        val class2 = ClassItem("c2", "Android", "...", 150.0, null, null, "p")
        backend.registerUser(studentUser)
        backend.classes.addAll(listOf(class1, class2))

        backend.addToCart(class1, studentUser)
        backend.addToCart(class2, studentUser)

        assertEquals(250.0, backend.getCartTotal(), 0.0)
    }

    @Test
    fun `MTC_170 checkout enrolls student in class and clears cart`() {
        val studentUser = User("student@test.com", "Student", Role.ESTUDIANTE)
        val class1 = ClassItem("c1", "Kotlin Básico", "...", 100.0, null, null, "prof@test.com")
        backend.registerUser(studentUser)
        backend.classes.add(class1)
        backend.addToCart(class1, studentUser)

        val checkoutResult = backend.checkout(studentUser)

        assertTrue(checkoutResult)
        assertTrue(backend.cart.isEmpty())
        val updatedClass = backend.classes.find { it.id == class1.id }
        assertNotNull(updatedClass)
        assertTrue(updatedClass!!.assignedStudents.contains(studentUser.email))
    }

    @Test
    fun `MTC_170 checkout fails for empty cart`() {
        val studentUser = User("student@test.com", "Student", Role.ESTUDIANTE)
        backend.registerUser(studentUser)

        val checkoutResult = backend.checkout(studentUser)

        assertFalse(checkoutResult)
    }

    // endregion

    // region Role-specific Logic Tests

    @Test
    fun `MTC_390 parentCanSeeLinkedStudentProgress returns true for linked parent`() {
        val parent = User("parent@test.com", "Parent", Role.APODERADO, "student@test.com")
        backend.registerUser(parent)

        assertTrue(backend.parentCanSeeLinkedStudentProgress("parent@test.com"))
    }

    @Test
    fun `getStudents returns only student users`() {
        // Arrange
        backend.registerUser(User("student1@test.com", "S1", Role.ESTUDIANTE))
        backend.registerUser(User("prof@test.com", "P1", Role.PROFESOR))
        backend.registerUser(User("admin@test.com", "A1", Role.ADMIN))
        backend.registerUser(User("student2@test.com", "S2", Role.ESTUDIANTE))

        // Act
        val students = backend.getStudents()

        // Assert
        assertEquals(2, students.size)
        assertTrue(students.all { it.role == Role.ESTUDIANTE })
    }

    // endregion
}
