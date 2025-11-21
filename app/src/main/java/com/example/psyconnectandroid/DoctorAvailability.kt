package com.example.psyconnectandroid

import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude

/**
 * Modelo de disponibilidade do doutor
 * Document ID: {doctorId}_{timestamp}
 */
data class DoctorAvailability(
    val doctorId: String = "",
    val date: Timestamp? = null,              // Data do slot
    val startTime: Timestamp? = null,           // Horário de início
    val endTime: Timestamp? = null,             // Horário de fim
    val isAvailable: Boolean = true,            // Se está disponível
    val isBooked: Boolean = false,              // Se está reservado
    val appointmentId: String? = null,           // ID do appointment (se reservado)
    val patientId: String? = null,              // ID do paciente (se reservado)
    val patientName: String? = null,            // Nome do paciente (se reservado)
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    @get:Exclude
    var id: String = ""
    
    companion object {
        fun fromMap(map: Map<String, Any>, documentId: String): DoctorAvailability {
            return DoctorAvailability(
                doctorId = map["doctorId"] as? String ?: "",
                date = map["date"] as? Timestamp,
                startTime = map["startTime"] as? Timestamp,
                endTime = map["endTime"] as? Timestamp,
                isAvailable = (map["isAvailable"] as? Boolean) ?: true,
                isBooked = (map["isBooked"] as? Boolean) ?: false,
                appointmentId = map["appointmentId"] as? String,
                patientId = map["patientId"] as? String,
                patientName = map["patientName"] as? String,
                createdAt = map["createdAt"] as? Timestamp,
                updatedAt = map["updatedAt"] as? Timestamp
            ).apply {
                this.id = documentId
            }
        }
        
        fun toMap(availability: DoctorAvailability): Map<String, Any> {
            val map = mutableMapOf<String, Any>()
            map["doctorId"] = availability.doctorId
            availability.date?.let { map["date"] = it }
            availability.startTime?.let { map["startTime"] = it }
            availability.endTime?.let { map["endTime"] = it }
            map["isAvailable"] = availability.isAvailable
            map["isBooked"] = availability.isBooked
            availability.appointmentId?.let { map["appointmentId"] = it }
            availability.patientId?.let { map["patientId"] = it }
            availability.patientName?.let { map["patientName"] = it }
            availability.createdAt?.let { map["createdAt"] = it }
            availability.updatedAt?.let { map["updatedAt"] = it }
            return map
        }
    }
}


