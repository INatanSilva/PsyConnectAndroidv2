package com.example.psyconnectandroid

import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude

data class Appointment(
    @get:Exclude var id: String = "",
    val doctorId: String = "",
    val patientId: String = "",
    val doctorName: String = "",
    val patientName: String = "",
    val startTime: Timestamp? = null,
    val endTime: Timestamp? = null,
    val status: String = "" // e.g., "confirmed", "completed", "cancelled"
) {
    companion object {
        fun fromMap(map: Map<String, Any>, documentId: String): Appointment {
            // Try to get startTime/endTime, fallback to scheduledStartTime/scheduledEndTime
            val startTime = map["startTime"] as? Timestamp
                ?: map["scheduledStartTime"] as? Timestamp
            val endTime = map["endTime"] as? Timestamp
                ?: map["scheduledEndTime"] as? Timestamp
            
            // Normalize status to lowercase for comparison
            val statusValue = map["status"] as? String ?: "Pendente"
            val normalizedStatus = when (statusValue.lowercase()) {
                "agendada", "scheduled" -> "Agendada"
                "confirmed", "confirmada" -> "Confirmada"
                "completed", "completada", "concluída" -> "Completada"
                "cancelled", "cancelada" -> "Cancelada"
                else -> statusValue
            }
            
            return Appointment(
                id = documentId,
                doctorId = map["doctorId"] as? String ?: "",
                patientId = map["patientId"] as? String ?: "",
                doctorName = map["doctorName"] as? String ?: "Dr(a). Desconhecido(a)",
                patientName = map["patientName"] as? String ?: "Paciente",
                startTime = startTime,
                endTime = endTime,
                status = normalizedStatus
            )
        }
    }
}