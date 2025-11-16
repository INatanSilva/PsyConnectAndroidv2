package com.example.psyconnectandroid

import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude

data class AvailabilitySlot(
    val doctorId: String = "",
    val startTime: Timestamp? = null,
    val endTime: Timestamp? = null,
    val date: Timestamp? = null,
    val isBooked: Boolean = false,
    val isAvailable: Boolean = true,
    val patientId: String? = null,
    val patientName: String? = null,
    val appointmentId: String? = null,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    @get:Exclude
    var id: String = ""

    companion object {
        fun fromMap(map: Map<String, Any>, id: String): AvailabilitySlot {
            return AvailabilitySlot(
                doctorId = map["doctorId"] as? String ?: "",
                startTime = map["startTime"] as? Timestamp,
                endTime = map["endTime"] as? Timestamp,
                date = map["date"] as? Timestamp,
                isBooked = map["isBooked"] as? Boolean ?: false,
                isAvailable = map["isAvailable"] as? Boolean ?: true,
                patientId = map["patientId"] as? String,
                patientName = map["patientName"] as? String,
                appointmentId = map["appointmentId"] as? String,
                createdAt = map["createdAt"] as? Timestamp,
                updatedAt = map["updatedAt"] as? Timestamp
            ).apply {
                this.id = id
            }
        }
    }
}