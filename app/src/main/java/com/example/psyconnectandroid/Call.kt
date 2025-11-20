package com.example.psyconnectandroid

import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude

/**
 * Modelo de chamada de vídeo
 * Estrutura do Firebase: calls collection
 */
data class Call(
    @get:Exclude var id: String = "",
    val callId: String = "",
    val callerId: String = "",
    val calleeId: String = "",
    val patientName: String = "",
    val status: CallStatus = CallStatus.INITIATED,
    val timestamp: Timestamp = Timestamp.now(),
    val answeredAt: Timestamp? = null,
    val endedAt: Timestamp? = null,
    val rejectedAt: Timestamp? = null,
    val participants: List<String> = emptyList()
) {
    companion object {
        fun fromMap(map: Map<String, Any>, documentId: String): Call {
            val statusStr = map["status"] as? String ?: CallStatus.INITIATED.name
            val status = try {
                CallStatus.valueOf(statusStr.uppercase())
            } catch (e: Exception) {
                CallStatus.INITIATED
            }
            
            val participantsList = map["participants"] as? List<*> ?: emptyList<Any>()
            val participants = participantsList.mapNotNull { it as? String }
            
            return Call(
                id = documentId,
                callId = map["callId"] as? String ?: documentId,
                callerId = map["callerId"] as? String ?: "",
                calleeId = map["calleeId"] as? String ?: "",
                patientName = map["patientName"] as? String ?: "",
                status = status,
                timestamp = map["timestamp"] as? Timestamp ?: Timestamp.now(),
                answeredAt = map["answeredAt"] as? Timestamp,
                endedAt = map["endedAt"] as? Timestamp,
                rejectedAt = map["rejectedAt"] as? Timestamp,
                participants = participants
            )
        }
        
        fun toMap(call: Call): Map<String, Any> {
            val map = mutableMapOf<String, Any>(
                "callId" to call.callId,
                "callerId" to call.callerId,
                "calleeId" to call.calleeId,
                "patientName" to call.patientName,
                "status" to call.status.name.lowercase(),
                "timestamp" to call.timestamp,
                "participants" to call.participants
            )
            
            call.answeredAt?.let { map["answeredAt"] = it }
            call.endedAt?.let { map["endedAt"] = it }
            call.rejectedAt?.let { map["rejectedAt"] = it }
            
            return map
        }
    }
}

enum class CallStatus {
    INITIATED,  // Chamada iniciada, aguardando resposta
    ANSWERED,   // Chamada atendida, em andamento
    ENDED,      // Chamada finalizada
    REJECTED,   // Chamada rejeitada
    MISSED      // Chamada perdida
}

