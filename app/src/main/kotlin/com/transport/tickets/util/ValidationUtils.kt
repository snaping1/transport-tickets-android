package com.transport.tickets.util

object ValidationUtils {
    fun isValidEmail(email: String): Boolean =
        email.length >= 3 && email.contains('@') && email.substringAfter('@').contains('.')

    fun isValidPassword(password: String): Boolean = password.length >= 6

    fun passwordsMatch(p1: String, p2: String): Boolean = p1 == p2

    fun isValidDocumentNumber(number: String): Boolean =
        number.isNotBlank() && number.length >= 4

    fun emailError(email: String): String? =
        if (!isValidEmail(email)) "Введите корректный e-mail" else null

    fun passwordError(password: String): String? =
        if (!isValidPassword(password)) "Пароль должен содержать не менее 6 символов" else null
}
