package com.example.psyconnectandroid

import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude

/**
 * Modelo de solicitação de consulta
 * Usado na tela de pacientes do doutor
 */
data class ConsultationRequest(
    val id: String = "",
    val patientId: String = "",
    val patientName: String = "",
    val createdAt: Timestamp? = null,
    val status: String = "",                    // "Pago", "Aguardando Confirmação", "Agendada", "Cancelada"
    val type: String = "appointment",           // Tipo: "appointment"
    val date: Timestamp? = null,                // Data da consulta
    val time: String? = null,                   // Horário da consulta
    val scheduledStartTime: Timestamp? = null,  // Horário agendado de início
    val scheduledEndTime: Timestamp? = null,     // Horário agendado de fim
    val paymentStatus: String? = null,          // Status do pagamento
    val amountEurCents: Int = 0                 // Valor em centavos
) {
    @get:Exclude
    val isPending: Boolean
        get() = status == "Pago" || status == "Aguardando Confirmação"
    
    @get:Exclude
    val isAccepted: Boolean
        get() = status == "Agendada"
    
    @get:Exclude
    val isCancelled: Boolean
        get() = status == "Cancelada"
    
    companion object {
        fun fromAppointment(appointment: Appointment): ConsultationRequest {
            return ConsultationRequest(
                id = appointment.id,
                patientId = appointment.patientId,
                patientName = appointment.patientName,
                createdAt = appointment.startTime, // Usar startTime como createdAt se disponível
                status = appointment.status,
                type = "appointment",
                date = appointment.startTime,
                scheduledStartTime = appointment.startTime,
                scheduledEndTime = appointment.endTime
            )
        }
        
        fun fromMap(map: Map<String, Any>, documentId: String): ConsultationRequest {
            val payment = map["payment"] as? Map<*, *>
            val amount = (payment?.get("amountEurCents") as? Number)?.toInt()
                ?: (map["priceEurCents"] as? Number)?.toInt()
                ?: 0
            val paymentStatus = payment?.get("status") as? String
            
            return ConsultationRequest(
                id = documentId,
                patientId = map["patientId"] as? String ?: "",
                patientName = map["patientName"] as? String ?: "Paciente",
                createdAt = map["createdAt"] as? Timestamp,
                status = map["status"] as? String ?: "Pago",
                type = map["type"] as? String ?: "appointment",
                date = map["date"] as? Timestamp ?: map["scheduledStartTime"] as? Timestamp,
                time = map["time"] as? String,
                scheduledStartTime = map["scheduledStartTime"] as? Timestamp,
                scheduledEndTime = map["scheduledEndTime"] as? Timestamp,
                paymentStatus = paymentStatus,
                amountEurCents = amount
            )
        }
    }
}


