package com.example.educanet.domain

fun isValidEmail(email: String): Boolean {
    if (email.isBlank() || !email.contains("@")) return false
    val parts = email.split("@")
    if (parts.size != 2) return false
    val domain = parts[1].lowercase()
    return when (domain) {
        "admineducanet.cl", "profesoreducanet.cl", "apoderadoeducanet.cl", "educanet.cl" -> true
        else -> false
    }
}

fun getRoleFromEmail(email: String): Role? {
    if (email.isBlank() || !email.contains("@")) return null
    val parts = email.split("@")
    if (parts.size != 2) return null
    return when (parts[1].lowercase()) {
        "admineducanet.cl" -> Role.ADMIN
        "profesoreducanet.cl" -> Role.PROFESOR
        "apoderadoeducanet.cl" -> Role.APODERADO
        "educanet.cl" -> Role.ESTUDIANTE
        else -> null
    }
}
