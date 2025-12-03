# Educanet — Plataforma Móvil Educativa  
Aplicación móvil desarrollada en **Android Studio** utilizando **Kotlin + Jetpack Compose**, diseñada para mejorar la experiencia educativa entre estudiantes, profesores y apoderados.  
Este proyecto corresponde al *Encargo de la Evaluación Parcial 2* de la asignatura **Desarrollo de Aplicaciones Móviles (DSY1105)**.

---

##  Objetivo del Proyecto
Educanet busca centralizar funciones académicas esenciales dentro de una sola aplicación fácil de usar, rápida y moderna.  
La app permite crear clases, ver progreso, administrar cursos y acceder a información educativa desde un entorno amigable para estudiantes y docentes.

---

#  Funcionalidades Implementadas

###  Gestión Académica
- Crear clases con datos personalizados.
- Visualización de asignaturas del usuario.
- Detalle de una clase seleccionada.
- Sistema básico de progreso (vista de avance por asignatura).

###  Autenticación
- Inicio de sesión con validaciones.
- Registro de nuevos usuarios.
- Persistencia de sesión con DataStore.
- Cierre de sesión seguro.

###  Interfaz & Navegación
- Navegación fluida mediante **NavHost**.
- Estructura visual modular con Jetpack Compose.
- Pantalla de Splash animada.
- Diseño responsivo y moderno.

###  Formularios con validación
- Validaciones lógicas de email, contraseña y campos obligatorios.
- Lógica separada del UI para cumplir con MVVM.
- (Pendiente agregar retroalimentación visual de errores).

###  Persistencia Local
- Implementación de **DataStore** para almacenar:
  - Preferencias del usuario
  - Estado de sesión
  - Configuraciones básicas

###  Recursos Nativos Utilizados
- **Cámara** para cargar imágenes (StorageImage).
- **Notificaciones push (Firebase Messaging)** integradas en el proyecto.
- (Pendiente conectar notificaciones a la UI o agregar segundo recurso nativo adicional).

---

#  Arquitectura del Proyecto

El proyecto utiliza el siguiente patron
com.example.educanet
│
├── data
├── domain
├── item
├── navigation
├── repo
├── screen
├── ui.ui
├── util
└── viewmodel



