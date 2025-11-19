package com.example.psyconnectandroid

import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude

/**
 * Modelo de mensagem compatível com iOS
 * Campos: id, senderId, text, createdAt
 */
data class Message(
    @get:Exclude var id: String = "",
    val senderId: String = "",
    val text: String = "",
    val createdAt: Timestamp = Timestamp.now()
) {
    companion object {
        fun fromMap(map: Map<String, Any>, documentId: String): Message {
            return Message(
                id = documentId,
                senderId = map["senderId"] as? String ?: "",
                text = map["text"] as? String ?: "",
                createdAt = map["createdAt"] as? Timestamp ?: Timestamp.now()
            )
        }
        
        fun toMap(message: Message): Map<String, Any> {
            return mapOf(
                "senderId" to message.senderId,
                "text" to message.text,
                "createdAt" to message.createdAt
            )
        }
    }
}

