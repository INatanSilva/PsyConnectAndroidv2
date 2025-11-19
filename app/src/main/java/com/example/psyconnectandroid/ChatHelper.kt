package com.example.psyconnectandroid

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

object ChatHelper {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    /**
     * Gera chatId ordenado alfabeticamente (compatível com iOS)
     * Exemplo: se patientId="uPAmIPl..." e doctorId="hhGlsn...", 
     * retorna "hhGlsn..._uPAmIPl..." (ordenado)
     */
    fun generateChatId(patientId: String, doctorId: String): String {
        return if (doctorId < patientId) {
            "${doctorId}_${patientId}"
        } else {
            "${patientId}_${doctorId}"
        }
    }
    
    /**
     * Cria ou obtém um chatRoom entre um paciente e um doutor
     */
    fun createOrGetChatRoom(
        patientId: String,
        doctorId: String,
        patientName: String,
        doctorName: String,
        patientPhotoUrl: String = "",
        doctorPhotoUrl: String = "",
        appointmentId: String? = null,
        onSuccess: (String) -> Unit, // Retorna o chatRoomId
        onFailure: (Exception) -> Unit
    ) {
        // Gerar chatId ordenado (compatível com iOS)
        val chatRoomId = generateChatId(patientId, doctorId)
        
        // Verificar se o chat já existe usando o ID ordenado
        firestore.collection("chats")
            .document(chatRoomId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    // Chat já existe
                    onSuccess(chatRoomId)
                } else {
                    // Criar novo chat
                    
                    val chatRoom = ChatRoom(
                        id = chatRoomId,
                        patientId = patientId,
                        doctorId = doctorId,
                        patientName = patientName,
                        doctorName = doctorName,
                        patientPhotoUrl = patientPhotoUrl,
                        doctorPhotoUrl = doctorPhotoUrl,
                        lastMessage = "",
                        lastMessageTimestamp = null,
                        unreadCount = 0,
                        appointmentId = appointmentId
                    )
                    
                    firestore.collection("chats")
                        .document(chatRoomId)
                        .set(ChatRoom.toMap(chatRoom))
                        .addOnSuccessListener {
                            onSuccess(chatRoomId)
                        }
                        .addOnFailureListener { e ->
                            onFailure(e)
                        }
                }
            }
            .addOnFailureListener { e ->
                android.util.Log.e("ChatHelper", "Error checking/creating chat", e)
                onFailure(e)
            }
    }
    
    /**
     * Inicia uma conversa com um doutor a partir do perfil do doutor
     */
    fun startChatWithDoctor(
        doctorId: String,
        doctorName: String,
        doctorPhotoUrl: String = "",
        activity: android.app.Activity,
        onChatRoomCreated: (String) -> Unit
    ) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            android.widget.Toast.makeText(activity, "Você precisa estar logado", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        
        val patientId = currentUser.uid
        
        // Buscar nome e foto do paciente
        firestore.collection("pacientes").document(patientId).get()
            .addOnSuccessListener { patientDoc ->
                val patientName = patientDoc.getString("name") 
                    ?: patientDoc.getString("fullName") 
                    ?: "Paciente"
                val patientPhotoUrl = patientDoc.getString("profileImageURL") 
                    ?: patientDoc.getString("photoUrl") 
                    ?: ""
                
                createOrGetChatRoom(
                    patientId = patientId,
                    doctorId = doctorId,
                    patientName = patientName,
                    doctorName = doctorName,
                    patientPhotoUrl = patientPhotoUrl,
                    doctorPhotoUrl = doctorPhotoUrl,
                    onSuccess = { chatRoomId ->
                        onChatRoomCreated(chatRoomId)
                    },
                    onFailure = { e ->
                        android.util.Log.e("ChatHelper", "Error creating chat room", e)
                        android.widget.Toast.makeText(activity, "Erro ao iniciar conversa", android.widget.Toast.LENGTH_SHORT).show()
                    }
                )
            }
            .addOnFailureListener { e ->
                // Se não conseguir buscar dados do paciente, usar valores padrão
                createOrGetChatRoom(
                    patientId = patientId,
                    doctorId = doctorId,
                    patientName = "Paciente",
                    doctorName = doctorName,
                    patientPhotoUrl = "",
                    doctorPhotoUrl = doctorPhotoUrl,
                    onSuccess = { chatRoomId ->
                        onChatRoomCreated(chatRoomId)
                    },
                    onFailure = { ex ->
                        android.util.Log.e("ChatHelper", "Error creating chat room", ex)
                        android.widget.Toast.makeText(activity, "Erro ao iniciar conversa", android.widget.Toast.LENGTH_SHORT).show()
                    }
                )
            }
    }
    
    /**
     * Inicia um chat a partir de um appointment
     */
    fun startChatFromAppointment(
        appointment: Appointment,
        activity: android.app.Activity,
        onChatRoomCreated: (String) -> Unit
    ) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            android.widget.Toast.makeText(activity, "Você precisa estar logado", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        
        val currentUserId = currentUser.uid
        
        // Buscar dados do paciente
        firestore.collection("pacientes").document(appointment.patientId).get()
            .addOnSuccessListener { patientDoc ->
                val patientName = patientDoc.getString("name") 
                    ?: patientDoc.getString("fullName") 
                    ?: appointment.patientName
                val patientPhotoUrl = patientDoc.getString("profileImageURL") 
                    ?: patientDoc.getString("photoUrl") 
                    ?: ""
                
                // Buscar dados do doutor
                firestore.collection("doutores").document(appointment.doctorId).get()
                    .addOnSuccessListener { doctorDoc ->
                        val doctorName = doctorDoc.getString("name") 
                            ?: doctorDoc.getString("fullName") 
                            ?: appointment.doctorName
                        val doctorPhotoUrl = doctorDoc.getString("profileImageURL") 
                            ?: doctorDoc.getString("photoUrl") 
                            ?: ""
                        
                        createOrGetChatRoom(
                            patientId = appointment.patientId,
                            doctorId = appointment.doctorId,
                            patientName = patientName,
                            doctorName = doctorName,
                            patientPhotoUrl = patientPhotoUrl,
                            doctorPhotoUrl = doctorPhotoUrl,
                            appointmentId = appointment.id,
                            onSuccess = { chatRoomId ->
                                onChatRoomCreated(chatRoomId)
                            },
                            onFailure = { e ->
                                android.util.Log.e("ChatHelper", "Error creating chat from appointment", e)
                                android.widget.Toast.makeText(activity, "Erro ao iniciar conversa", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                    .addOnFailureListener { e ->
                        // Se não conseguir buscar dados do doutor, usar valores do appointment
                        createOrGetChatRoom(
                            patientId = appointment.patientId,
                            doctorId = appointment.doctorId,
                            patientName = patientName,
                            doctorName = appointment.doctorName,
                            patientPhotoUrl = patientPhotoUrl,
                            doctorPhotoUrl = "",
                            appointmentId = appointment.id,
                            onSuccess = { chatRoomId ->
                                onChatRoomCreated(chatRoomId)
                            },
                            onFailure = { ex ->
                                android.util.Log.e("ChatHelper", "Error creating chat from appointment", ex)
                                android.widget.Toast.makeText(activity, "Erro ao iniciar conversa", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
            }
            .addOnFailureListener { e ->
                // Se não conseguir buscar dados do paciente, usar valores do appointment
                firestore.collection("doutores").document(appointment.doctorId).get()
                    .addOnSuccessListener { doctorDoc ->
                        val doctorName = doctorDoc.getString("name") 
                            ?: doctorDoc.getString("fullName") 
                            ?: appointment.doctorName
                        val doctorPhotoUrl = doctorDoc.getString("profileImageURL") 
                            ?: doctorDoc.getString("photoUrl") 
                            ?: ""
                        
                        createOrGetChatRoom(
                            patientId = appointment.patientId,
                            doctorId = appointment.doctorId,
                            patientName = appointment.patientName,
                            doctorName = doctorName,
                            patientPhotoUrl = "",
                            doctorPhotoUrl = doctorPhotoUrl,
                            appointmentId = appointment.id,
                            onSuccess = { chatRoomId ->
                                onChatRoomCreated(chatRoomId)
                            },
                            onFailure = { ex ->
                                android.util.Log.e("ChatHelper", "Error creating chat from appointment", ex)
                                android.widget.Toast.makeText(activity, "Erro ao iniciar conversa", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                    .addOnFailureListener { ex ->
                        // Usar valores básicos do appointment
                        createOrGetChatRoom(
                            patientId = appointment.patientId,
                            doctorId = appointment.doctorId,
                            patientName = appointment.patientName,
                            doctorName = appointment.doctorName,
                            patientPhotoUrl = "",
                            doctorPhotoUrl = "",
                            appointmentId = appointment.id,
                            onSuccess = { chatRoomId ->
                                onChatRoomCreated(chatRoomId)
                            },
                            onFailure = { error ->
                                android.util.Log.e("ChatHelper", "Error creating chat from appointment", error)
                                android.widget.Toast.makeText(activity, "Erro ao iniciar conversa", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
            }
    }
}


