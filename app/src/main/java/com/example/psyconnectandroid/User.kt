package com.example.psyconnectandroid

data class User(
    val fullName: String = "",
    val email: String = "",
    val userType: UserType = UserType.PATIENT,
    val psychologistCardCountry: String? = null,
    val psychologistCardNumber: String? = null
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "fullName" to fullName,
            "email" to email,
            "userType" to userType.name,
            "psychologistCardCountry" to psychologistCardCountry,
            "psychologistCardNumber" to psychologistCardNumber
        )
    }
}