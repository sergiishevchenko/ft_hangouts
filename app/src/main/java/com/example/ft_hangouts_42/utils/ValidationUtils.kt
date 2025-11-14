package com.example.ft_hangouts_42.utils

object ValidationUtils {
    fun isValidPhone(phone: String): Boolean {
        val digitsOnly = phone.replace(Regex("[^0-9]"), "")
        return digitsOnly.length >= 7 && digitsOnly.length <= 15
    }

    fun isValidEmail(email: String): Boolean {
        if (email.isBlank()) return true
        val emailRegex = Regex(
            "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
        )
        return emailRegex.matches(email)
    }

    fun formatPhone(phone: String): String {
        return phone.replace(Regex("[^0-9]"), "")
    }
}

