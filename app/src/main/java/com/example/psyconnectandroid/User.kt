package com.example.psyconnectandroid

import com.google.firebase.firestore.Exclude

data class User(
    val fullName: String = "",
    val email: String = "",
    val userType: UserType = UserType.PATIENT
) {
    @get:Exclude
    var id: String = ""
    
    fun toMap(): Map<String, Any> {
        return mapOf(
            "fullName" to fullName,
            "email" to email,
            "userType" to userType.name
        )
    }
    
    companion object {
        fun fromMap(map: Map<String, Any>, id: String): User {
            return User(
                fullName = map["fullName"] as? String ?: "",
                email = map["email"] as? String ?: "",
                userType = try {
                    UserType.valueOf(map["userType"] as? String ?: "PATIENT")
                } catch (e: Exception) {
                    UserType.PATIENT
                }
            ).apply {
                this.id = id
            }
        }
    }
}

