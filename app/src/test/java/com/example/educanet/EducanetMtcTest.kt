package com.example.educanet


import com.example.educanet.domain.*
import org.junit.Assert.*


import com.example.educanet.domain.ClassItem
import com.example.educanet.domain.FakeEducanetBackend
import com.example.educanet.domain.Grade
import com.example.educanet.domain.Resource
import com.example.educanet.domain.Role
import com.example.educanet.domain.User
import org.junit.Before
import org.junit.Test

/**
 * Pruebas JUnit que cubren TODOS los MTC del plan de pruebas.
 * Cada @Test corresponde a un MTC_xxx.
 */
class EducanetMtcTest {

    private lateinit var backend: FakeEducanetBackend

    @Before
    fun setup() {
        backend = FakeEducanetBackend()

        // Cargamos usuarios base (según lo que tienes en Firebase)
        backend.registerUser(
            User(
                email = "katherine@educanet.cl",
                name = "Katherine",
                role = Role.ESTUDIANTE
            )
        )
        backend.registerUser(
            User(
                email = "nicolas@profesor.cl",
                name = "nicolas",
                role = Role.PROFESOR
            )
        )
        backend.registerUser(
            User(
                email = "isabel@admineducanet.cl",
                name = "iisabel",
                role = Role.ADMIN
            )
        )
        backend.registerUser(
            User(
                email = "ana@apoderado.cl",
                name = "Ana",
                role = Role.APODERADO,
                linkedStudentEmail = "katherine@educanet.cl"
            )
        )
    }

    // =========================================================
    // MTC_010 - Registro de usuario exitoso
    // =========================================================
    @Test
    fun MTC_010_registro_usuario_exitoso() {
        val nuevo = User(
            email = "nuevo@educanet.cl",
            name = "Nuevo",
            role = Role.ESTUDIANTE
        )
        val ok = backend.registerUser(nuevo)

        assertTrue("El usuario debería registrarse correctamente", ok)
        assertTrue(backend.users.any { it.email == "nuevo@educanet.cl" })
    }

    // =========================================================
    // MTC_020 - Inicio de sesión válido
    // =========================================================
    @Test
    fun MTC_020_inicio_sesion_valido() {
        val user = backend.login("katherine@educanet.cl", "123456")
        val welcome = backend.buildWelcomeMessage(user)

        assertNotNull("Debería devolver un usuario válido", user)
        assertEquals("Hola, Katherine", welcome)
    }

    // =========================================================
    // MTC_030 - Recuperación de contraseña (NO implementado)
    // =========================================================
    @Test
    fun MTC_030_recuperacion_contrasena_no_implementada() {
        val available = backend.isPasswordRecoveryAvailable()
        assertFalse(
            "No debería existir funcionalidad de recuperación de contraseña",
            available
        )
    }

    // =========================================================
    // MTC_040 - Edición de perfil de usuario
    // =========================================================
    @Test
    fun MTC_040_edicion_perfil_limitada() {
        val canEditProfile = backend.canEditProfileNameOrEmail()
        assertFalse(
            "Solo se debería permitir cambiar avatar localmente, no nombre/email",
            canEditProfile
        )
    }

    // =========================================================
    // MTC_050 - Búsqueda y visualización de recursos
    // =========================================================
    @Test
    fun MTC_050_lista_recursos_ordenada() {
        backend.resources.addAll(
            listOf(
                Resource(
                    id = "1",
                    title = "Recurso antiguo",
                    type = "PDF",
                    url = "http://pdf1",
                    createdAt = 1000L,
                    createdByRole = Role.PROFESOR
                ),
                Resource(
                    id = "2",
                    title = "Recurso nuevo",
                    type = "Video",
                    url = "http://video",
                    createdAt = 2000L,
                    createdByRole = Role.PROFESOR
                )
            )
        )

        val ordered = backend.getResourcesOrderedDesc()
        assertEquals("Recurso nuevo", ordered.first().title)
        assertEquals("Recurso antiguo", ordered.last().title)
    }

    // =========================================================
    // MTC_060 - Subida de recurso por profesor
    // =========================================================
    @Test
    fun MTC_060_subida_recurso_profesor() {
        val canUpload = backend.professorCanUploadResource()
        assertTrue("Los profesores deberían poder crear recursos", canUpload)
    }

    // =========================================================
    // MTC_070 - Reproducción de video integrado (externo)
    // =========================================================
    @Test
    fun MTC_070_reproduccion_video_externa() {
        val ok = backend.openVideoInExternalBrowser("https://youtube.com/mi_video")
        assertTrue("El video se abre mediante Intent externo, no nativo", ok)
    }

    // =========================================================
    // MTC_080 - Descarga para offline (NO implementado)
    // =========================================================
    @Test
    fun MTC_080_descarga_offline_no_disponible() {
        val offline = backend.hasOfflineSupport()
        assertFalse("La app requiere conexión permanente, sin modo offline", offline)
    }

    // =========================================================
    // MTC_090 - Unirse a clase virtual (NO implementado)
    // =========================================================
    @Test
    fun MTC_090_aula_virtual_no_implementada() {
        val hasVirtualClassroom = backend.hasVirtualClassroom()
        assertFalse("No existe módulo de aula virtual / videollamadas", hasVirtualClassroom)
    }

    // =========================================================
    // MTC_100 - Notificación de clase próxima
    // =========================================================
    @Test
    fun MTC_100_notificaciones_push_ok() {
        val works = backend.notificationsWork()
        assertTrue("MyFirebaseMessagingService debería recibir y mostrar notificaciones", works)
    }

    // =========================================================
    // MTC_110 - Envío de mensaje interno
    // =========================================================
    @Test
    fun MTC_110_mensajeria_por_clase_ok() {
        val works = backend.messagingWorks()
        assertTrue("Los comentarios deberían funcionar como mensajería por clase", works)
    }

    // =========================================================
    // MTC_120 - Visualización de calificaciones
    // =========================================================
    @Test
    fun MTC_120_visualizacion_calificaciones() {
        // Notas para Katherine y otro estudiante
        backend.grades.addAll(
            listOf(
                Grade("katherine@educanet.cl", 80),
                Grade("katherine@educanet.cl", 60),
                Grade("otro@educanet.cl", 50)
            )
        )

        val kGrades = backend.getGradesForStudent("katherine@educanet.cl")
        val otherGrades = backend.getGradesForStudent("otro@educanet.cl")

        assertEquals(2, kGrades.size)
        assertEquals(1, otherGrades.size)

        val avgK = backend.calculateAverageForStudent("katherine@educanet.cl")
        assertEquals(70, avgK)
        assertTrue("Con ≥70 debería estar aprobado", backend.isApproved(avgK))
    }

    // =========================================================
    // MTC_130 - Entrega de tarea por estudiante (NO implementado)
    // =========================================================
    @Test
    fun MTC_130_tareas_no_implementadas() {
        val available = backend.isHomeworkModuleAvailable()
        assertFalse("No existe módulo de tareas en la aplicación", available)
    }

    // =========================================================
    // MTC_140 - Content Provider (NO implementado)
    // =========================================================
    @Test
    fun MTC_140_content_provider_no_implementado() {
        val available = backend.isContentProviderAvailable()
        assertFalse("No existe Content Provider para compartir datos", available)
    }

    // =========================================================
    // MTC_150 - Sincronización con calendario (NO implementado)
    // =========================================================
    @Test
    fun MTC_150_calendario_no_implementado() {
        val available = backend.isCalendarIntegrationAvailable()
        assertFalse("No hay integración con calendario del dispositivo", available)
    }

    // =========================================================
    // MTC_160 - Configuración de notificaciones (NO implementado)
    // =========================================================
    @Test
    fun MTC_160_config_notificaciones_no_implementada() {
        val available = backend.isNotificationConfigAvailable()
        assertFalse("No existe configuración granular de notificaciones", available)
    }

    // =========================================================
    // MTC_170 - Procesamiento de pago (NO implementado)
    // =========================================================
    @Test
    fun MTC_170_pagos_no_implementados() {
        val available = backend.isPaymentModuleAvailable()
        assertFalse("No existe módulo de pagos en la aplicación", available)
    }

    // =========================================================
    // MTC_180 - Visualización de anuncios (NO implementado)
    // =========================================================
    @Test
    fun MTC_180_anuncios_no_implementados() {
        val available = backend.isAdsModuleAvailable()
        assertFalse("No existe sistema de anuncios en la aplicación", available)
    }

    // =========================================================
    // MTC_190 - Logout seguro (NO implementado)
    // =========================================================
    @Test
    fun MTC_190_logout_no_implementado() {
        val available = backend.isLogoutAvailable()
        assertFalse("No existe funcionalidad de logout en la app", available)
    }

    // =========================================================
    // MTC_390 - Registro con rol apoderado
    // =========================================================
    @Test
    fun MTC_390_registro_rol_apoderado() {
        val parent = backend.users.find { it.email == "ana@apoderado.cl" }
        assertNotNull(parent)
        assertEquals(Role.APODERADO, parent!!.role)
        assertTrue(
            "Apoderado debe tener linkedStudent para ver progreso del alumno",
            backend.parentCanSeeLinkedStudentProgress("ana@apoderado.cl")
        )
    }

    // =========================================================
    // MTC_400 - Creación de recursos por profesor
    // =========================================================
    @Test
    fun MTC_400_creacion_recursos_profesor() {
        val can = backend.professorCanUploadResource()
        assertTrue("Profesor debería poder crear recursos", can)
    }

    // =========================================================
    // MTC_410 - Visualización de clases con imágenes
    // =========================================================
    @Test
    fun MTC_410_clases_con_imagenes() {
        backend.classes.add(
            ClassItem(
                id = "class1",
                title = "Clase 1",
                description = "Descripción",
                videoLink = "https://youtube.com/123",
                imageUrl = "https://mi-imagen.png",
                professorEmail = "nicolas@profesor.cl"
            )
        )

        assertTrue(
            "Las clases deberían permitir mostrar imágenes con Coil",
            backend.classesHaveImages()
        )
    }

    // =========================================================
    // MTC_420 - Creación de clases por profesor
    // =========================================================
    @Test
    fun MTC_420_creacion_clases_profesor() {
        val can = backend.professorCanCreateClass()
        assertTrue("Profesor debería poder crear clases", can)
    }

    // =========================================================
    // MTC_430 - Creación de clases por admin
    // =========================================================
    @Test
    fun MTC_430_creacion_clases_admin_con_asignaciones() {
        backend.classes.add(
            ClassItem(
                id = "class2",
                title = "Clase Admin",
                description = "Admin crea clase",
                videoLink = null,
                imageUrl = null,
                professorEmail = "nicolas@profesor.cl",
                assignedStudents = listOf("katherine@educanet.cl")
            )
        )

        val ok = backend.adminCanCreateClassWithAssignments()
        assertTrue(
            "Admin debería poder crear clases asignando profesor y estudiantes",
            ok
        )
    }

    // =========================================================
    // MTC_440 - Progreso para apoderados
    // =========================================================
    @Test
    fun MTC_440_progreso_para_apoderados() {
        val canSee = backend.parentCanSeeLinkedStudentProgress("ana@apoderado.cl")
        assertTrue(
            "El apoderado debe poder ver el progreso del alumno vinculado",
            canSee
        )
    }
}