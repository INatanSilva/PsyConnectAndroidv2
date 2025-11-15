package com.example.psyconnectandroid

import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude

data class Doctor(
    val name: String = "",
    val email: String = "",
    val photoUrl: String = "",
    val priceEurCents: Int = 0,
    val isAvailable: Boolean = false,
    val isPromoted: Boolean = false,
    val promotionExpiresAt: Timestamp? = null,
    val specialization: String = "",
    val rating: Double = 0.0
) {
    @get:Exclude
    var id: String = ""
    
    fun getPriceFormatted(): String {
        val euros = priceEurCents / 100.0
        return String.format("%.2f €", euros)
    }
    
    /**
     * Check if the promotion is still valid (not expired)
     */
    fun isPromotionValid(): Boolean {
        if (!isPromoted || promotionExpiresAt == null) {
            return false
        }
        val now = Timestamp.now()
        return promotionExpiresAt.compareTo(now) > 0
    }
    
    companion object {
        fun fromMap(map: Map<String, Any>, id: String): Doctor {
            // Try different possible field names for photo (checking profileImageURL first as it's in Firestore)
            val photoUrl = map["profileImageURL"] as? String
                ?: map["profileImageUrl"] as? String
                ?: map["photoUrl"] as? String 
                ?: map["photo"] as? String 
                ?: map["imageUrl"] as? String 
                ?: map["image"] as? String 
                ?: map["profileImage"] as? String
                ?: ""
            
            // Debug log to check if photo URL is found
            if (photoUrl.isNotEmpty()) {
                android.util.Log.d("Doctor", "Photo URL found for doctor $id: ${photoUrl.take(50)}...")
            } else {
                android.util.Log.d("Doctor", "No photo URL found for doctor $id. Available fields: ${map.keys}")
            }
            
            // Check for isOnline (from Firestore) or isAvailable
            val isAvailable = map["isOnline"] as? Boolean 
                ?: map["isAvailable"] as? Boolean 
                ?: false
            
            // Check for isPromoted (new field name) or isFeatured (legacy)
            val isPromoted = map["isPromoted"] as? Boolean 
                ?: map["isFeatured"] as? Boolean 
                ?: false
            
            // Get promotion expiration date
            val promotionExpiresAt = map["promotionExpiresAt"] as? Timestamp
            
            return Doctor(
                name = map["name"] as? String ?: map["fullName"] as? String ?: "",
                email = map["email"] as? String ?: "",
                photoUrl = photoUrl,
                priceEurCents = (map["priceEurCents"] as? Number)?.toInt() ?: 0,
                isAvailable = isAvailable,
                isPromoted = isPromoted,
                promotionExpiresAt = promotionExpiresAt,
                specialization = map["specialization"] as? String ?: "",
                rating = (map["rating"] as? Number)?.toDouble() ?: 0.0
            ).apply {
                this.id = id
            }
        }
    }
}

