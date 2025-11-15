package com.example.psyconnectandroid

import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude

data class AvailabilitySlot(
    val startTime: Timestamp? = null,
    val endTime: Timestamp? = null,
    val isBooked: Boolean = false
) {
    @get:Exclude
    var id: String = ""

    companion object {
        fun fromMap(map: Map<String, Any>, id: String): AvailabilitySlot {
            return AvailabilitySlot(
                startTime = map["startTime"] as? Timestamp,
                endTime = map["endTime"] as? Timestamp,
                isBooked = map["isBooked"] as? Boolean ?: false
            ).apply {
                this.id = id
            }
        }
    }
}