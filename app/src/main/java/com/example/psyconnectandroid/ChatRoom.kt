package com.example.psyconnectandroid

import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude

data class ChatRoom(
    @get:Exclude var id: String = "",
    val patientId: String = "",
    val doctorId: String = "",
    val patientName: String = "",
    val doctorName: String = "",
    val patientPhotoUrl: String = "",
    val doctorPhotoUrl: String = "",
    val lastMessage: String = "",
    val lastMessageTimestamp: Timestamp? = null,
    val unreadCount: Int = 0,
    val appointmentId: String? = null  // Link to related appointment if any
) {
    companion object {
        fun fromMap(map: Map<String, Any>, documentId: String): ChatRoom {
            // Tentar ler participants como objeto primeiro (formato iOS)
            val participants = map["participants"] as? Map<String, Any>
            
            // Se participants existir, usar os valores de dentro
            val patientId = if (participants != null) {
                participants["patientId"] as? String ?: ""
            } else {
                map["patientId"] as? String ?: ""
            }
            
            val doctorId = if (participants != null) {
                participants["doctorId"] as? String ?: ""
            } else {
                map["doctorId"] as? String ?: ""
            }
            
            // Tentar ambos os formatos de timestamp
            val timestamp = map["lastMessageAt"] as? Timestamp 
                ?: map["lastMessageTimestamp"] as? Timestamp
            
            return ChatRoom(
                id = documentId,
                patientId = patientId,
                doctorId = doctorId,
                patientName = map["patientName"] as? String ?: "",
                doctorName = map["doctorName"] as? String ?: "",
                patientPhotoUrl = map["patientPhotoUrl"] as? String ?: "",
                doctorPhotoUrl = map["doctorPhotoUrl"] as? String ?: "",
                lastMessage = map["lastMessage"] as? String ?: "",
                lastMessageTimestamp = timestamp,
                unreadCount = (map["unreadCount"] as? Number)?.toInt() ?: 0,
                appointmentId = map["appointmentId"] as? String
            )
        }
        
        fun toMap(chatRoom: ChatRoom): Map<String, Any> {
            val timestamp = chatRoom.lastMessageTimestamp ?: Timestamp.now()
            
            val map = mutableMapOf(
                // Manter compatibilidade com formato iOS usando participants
                "participants" to mapOf(
                    "doctorId" to chatRoom.doctorId,
                    "patientId" to chatRoom.patientId
                ),
                // Também incluir no nível raiz para compatibilidade
                "patientId" to chatRoom.patientId,
                "doctorId" to chatRoom.doctorId,
                "patientName" to chatRoom.patientName,
                "doctorName" to chatRoom.doctorName,
                "patientPhotoUrl" to chatRoom.patientPhotoUrl,
                "doctorPhotoUrl" to chatRoom.doctorPhotoUrl,
                "lastMessage" to chatRoom.lastMessage,
                "lastMessageAt" to timestamp,  // Formato iOS
                "lastMessageTimestamp" to timestamp,  // Formato Android
                "unreadCount" to chatRoom.unreadCount,
                "createdAt" to timestamp
            )
            
            // Add appointmentId only if it's not null
            chatRoom.appointmentId?.let { map["appointmentId"] = it }
            
            return map
        }
    }
}


