package com.example.educanet.util

data class FieldError(val message: String)

object AuthValidators {

    fun validateEmail(email: String): FieldError? {
        if (email.isBlank()) return FieldError("El correo es obligatorio.")
        val ok = android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
        return if (!ok) FieldError("Formato de correo inválido.") else null
    }

    fun validatePassword(pass: String): FieldError? {
        if (pass.isBlank()) return FieldError("La contraseña es obligatoria.")
        if (pass.length < 6) return FieldError("Mínimo 6 caracteres.")
        return null
    }

    fun validateName(name: String): FieldError? {
        if (name.isBlank()) return FieldError("El nombre es obligatorio.")
        if (name.length < 2) return FieldError("Nombre demasiado corto.")
        return null
    }
}
