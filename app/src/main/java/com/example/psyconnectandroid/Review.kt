package com.example.psyconnectandroid

import com.google.firebase.Timestamp

data class Review(
    val reviewerName: String = "",
    val rating: Double = 0.0,
    val comment: String = "",
    val reviewerPhotoUrl: String = "",
    val createdAt: Timestamp? = null // Data de criação da avaliação
) {
    companion object {
        fun fromMap(map: Map<String, Any>): Review {
            return Review(
                reviewerName = map["reviewerName"] as? String ?: "Anônimo",
                rating = (map["rating"] as? Number)?.toDouble() ?: 0.0,
                comment = map["comment"] as? String ?: "",
                reviewerPhotoUrl = map["reviewerPhotoUrl"] as? String ?: "",
                createdAt = map["createdAt"] as? Timestamp
                    ?: map["created_at"] as? Timestamp
                    ?: map["date"] as? Timestamp
                    ?: null
            )
        }
    }
}