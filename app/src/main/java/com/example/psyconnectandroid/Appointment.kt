package com.example.psyconnectandroid

import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude
import java.util.Date

data class Appointment(
    @get:Exclude var id: String = "",
    val doctorId: String = "",
    val patientId: String = "",
    val doctorName: String = "",
    val patientName: String = "",
    val startTime: Timestamp? = null,
    val endTime: Timestamp? = null,
    val status: String = "", // e.g., "confirmed", "completed", "cancelled"
    val date: Timestamp? = null, // Data da consulta
    val time: String = "", // Horário formatado (ex: "05:09 - 06:09")
    val type: String = "consultation_request", // Tipo de documento
    val payment: Map<String, Any>? = null, // Objeto de pagamento
    val createdAt: Timestamp? = null, // Data de criação
    val updatedAt: Timestamp? = null, // Data de atualização
    @get:Exclude val doctorPhotoUrl: String = "" // Photo URL do doutor (não vem direto do Firestore, precisa buscar)
) {
    @get:Exclude
    val isPendingConfirmation: Boolean
        get() = status == "Pago" || status == "Aguardando Confirmação"
    
    @get:Exclude
    val isScheduled: Boolean
        get() = status == "Agendada" || status == "Scheduled"
    
    @get:Exclude
    val isCompleted: Boolean
        get() = status == "Concluída" || status == "Completed"
    
    @get:Exclude
    val isCancelled: Boolean
        get() = status == "Cancelada" || status == "Cancelled"
    
    companion object {
        fun fromMap(map: Map<String, Any>, documentId: String): Appointment {
            // Try to get startTime/endTime, fallback to scheduledStartTime/scheduledEndTime
            val startTime = map["startTime"] as? Timestamp
                ?: map["scheduledStartTime"] as? Timestamp
            val endTime = map["endTime"] as? Timestamp
                ?: map["scheduledEndTime"] as? Timestamp
            
            // Get date field
            val date = map["date"] as? Timestamp
            
            // Get time field (formatted string)
            val time = map["time"] as? String ?: ""
            
            // Get type
            val type = map["type"] as? String ?: "consultation_request"
            
            // Get payment object
            val payment = map["payment"] as? Map<String, Any>
            
            // Get timestamps
            val createdAt = map["createdAt"] as? Timestamp
            val updatedAt = map["updatedAt"] as? Timestamp
            
            // Normalize status to lowercase for comparison
            val statusValue = map["status"] as? String ?: "Pendente"
            val normalizedStatus = when (statusValue.lowercase()) {
                "agendada", "scheduled" -> "Agendada"
                "confirmed", "confirmada" -> "Confirmada"
                "completed", "completada", "concluída" -> "Concluída"
                "cancelled", "cancelada" -> "Cancelada"
                "pago", "paid" -> "Pago"
                "aguardando confirmação", "awaiting confirmation" -> "Aguardando Confirmação"
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
                status = normalizedStatus,
                date = date,
                time = time,
                type = type,
                payment = payment,
                createdAt = createdAt,
                updatedAt = updatedAt
            )
        }
    }
    
    /**
     * Formata a data para exibição (ex: "20 Nov 2025")
     */
    fun getFormattedDate(): String {
        val dateToFormat = date?.toDate() ?: startTime?.toDate() ?: Date()
        val formatter = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale("pt", "PT"))
        return formatter.format(dateToFormat)
    }
    
    /**
     * Retorna o horário formatado ou "A definir" se não houver
     */
    fun getFormattedTime(): String {
        return if (time.isNotEmpty()) {
            time
        } else if (startTime != null && endTime != null) {
            val start = startTime.toDate()
            val end = endTime.toDate()
            val timeFormat = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
            "${timeFormat.format(start)} - ${timeFormat.format(end)}"
        } else {
            "A definir"
        }
    }
}

/**
 * Enum para status de consulta
 */
enum class AppointmentStatus(val value: String) {
    PENDING("Aguardando Confirmação"),
    PAID("Pago"),
    SCHEDULED("Agendada"),
    COMPLETED("Concluída"),
    CANCELLED("Cancelada")
}
