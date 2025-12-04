package com.example.educanet

import com.example.educanet.domain.ClassItem
import com.example.educanet.domain.FakeEducanetBackend
import com.example.educanet.domain.Role
import com.example.educanet.domain.User
import com.example.educanet.util.AuthValidators
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    private val backend = FakeEducanetBackend()

    @Test
    fun `test admin can see student list`() {
        backend.users.add(User("admin@test.com", "Admin", Role.ADMIN))
        backend.users.add(User("student1@test.com", "Student 1", Role.ESTUDIANTE))
        backend.users.add(User("student2@test.com", "Student 2", Role.ESTUDIANTE))
        backend.users.add(User("profesor@test.com", "Profesor", Role.PROFESOR))

        val students = backend.getStudents()
        assertEquals(2, students.size)
        assertTrue(students.all { it.role == Role.ESTUDIANTE })
    }

    @Test
    fun `test admin can see student progress`() {
        val admin = User("admin@test.com", "Admin", Role.ADMIN)
        backend.users.add(admin)
        val canSeeProgress = backend.adminCanSeeStudentProgress(admin.email)
        assertTrue("Admin should be able to see student progress", canSeeProgress)
    }

    @Test
    fun `test admin can upload resources to class`() {
        val newClass = ClassItem(
            id = "c1",
            title = "Clase con Recursos",
            description = "Descripción de la clase",
            videoLink = null,
            imageUrl = null,
            professorEmail = "profesor@test.com",
            assignedStudents = listOf("student1@test.com"),
            resources = listOf(mapOf("name" to "recurso.pdf", "url" to "http://example.com/recurso.pdf"))
        )
        backend.classes.add(newClass)

        val canUpload = backend.adminCanUploadResourcesToClass()
        assertTrue("Admin should be able to upload resources to a class", canUpload)
    }

    @Test
    fun `test professor login with invalid domain fails`() {
        val professorEmail = "profesor@wrongdomain.com"
        val error = AuthValidators.validateEmail(professorEmail)
        assertEquals("El dominio del correo no es válido.", error?.message)
    }
}
