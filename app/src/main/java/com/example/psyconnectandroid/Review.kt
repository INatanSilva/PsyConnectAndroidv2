package com.example.psyconnectandroid

data class Review(
    val reviewerName: String = "",
    val rating: Double = 0.0,
    val comment: String = "",
    val reviewerPhotoUrl: String = ""
) {
    companion object {
        fun fromMap(map: Map<String, Any>): Review {
            return Review(
                reviewerName = map["reviewerName"] as? String ?: "Anônimo",
                rating = (map["rating"] as? Number)?.toDouble() ?: 0.0,
                comment = map["comment"] as? String ?: "",
                reviewerPhotoUrl = map["reviewerPhotoUrl"] as? String ?: ""
            )
        }
    }
}