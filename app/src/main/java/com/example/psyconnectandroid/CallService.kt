package com.example.psyconnectandroid

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import java.util.UUID

/**
 * Serviço para gerenciar chamadas no Firestore
 */
object CallService {
    private const val TAG = "CallService"
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    private var incomingCallsListener: ListenerRegistration? = null
    var onIncomingCall: ((Call) -> Unit)? = null
    
    /**
     * Inicia uma chamada
     */
    fun initiateCall(
        patientId: String,
        patientName: String,
        onSuccess: (String) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            onError(Exception("Usuário não autenticado"))
            return
        }
        
        val callId = UUID.randomUUID().toString()
        val callerId = currentUser.uid
        
        val call = Call(
            callId = callId,
            callerId = callerId,
            calleeId = patientId,
            patientName = patientName,
            status = CallStatus.INITIATED,
            timestamp = com.google.firebase.Timestamp.now(),
            participants = listOf(callerId, patientId)
        )
        
        firestore.collection("calls")
            .document(callId)
            .set(Call.toMap(call))
            .addOnSuccessListener {
                Log.d(TAG, "Chamada iniciada: $callId")
                onSuccess(callId)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Erro ao iniciar chamada", e)
                onError(e)
            }
    }
    
    /**
     * Aceita uma chamada
     */
    fun answerCall(
        callId: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        firestore.collection("calls")
            .document(callId)
            .update(
                mapOf(
                    "status" to CallStatus.ANSWERED.name.lowercase(),
                    "answeredAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                )
            )
            .addOnSuccessListener {
                Log.d(TAG, "Chamada aceita: $callId")
                onSuccess()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Erro ao aceitar chamada", e)
                onError(e)
            }
    }
    
    /**
     * Rejeita uma chamada
     */
    fun rejectCall(
        callId: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        firestore.collection("calls")
            .document(callId)
            .update(
                mapOf(
                    "status" to CallStatus.REJECTED.name.lowercase(),
                    "rejectedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                )
            )
            .addOnSuccessListener {
                Log.d(TAG, "Chamada rejeitada: $callId")
                onSuccess()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Erro ao rejeitar chamada", e)
                onError(e)
            }
    }
    
    /**
     * Finaliza uma chamada
     */
    fun endCall(
        callId: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        firestore.collection("calls")
            .document(callId)
            .update(
                mapOf(
                    "status" to CallStatus.ENDED.name.lowercase(),
                    "endedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                )
            )
            .addOnSuccessListener {
                Log.d(TAG, "Chamada finalizada: $callId")
                onSuccess()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Erro ao finalizar chamada", e)
                onError(e)
            }
    }
    
    /**
     * Marca chamada como perdida
     */
    fun markAsMissed(
        callId: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        firestore.collection("calls")
            .document(callId)
            .update(
                mapOf(
                    "status" to CallStatus.MISSED.name.lowercase()
                )
            )
            .addOnSuccessListener {
                Log.d(TAG, "Chamada marcada como perdida: $callId")
                onSuccess()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Erro ao marcar chamada como perdida", e)
                onError(e)
            }
    }
    
    /**
     * Escuta chamadas recebidas
     */
    fun listenToIncomingCalls() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Log.w(TAG, "Usuário não autenticado, não é possível escutar chamadas")
            return
        }
        
        val userId = currentUser.uid
        
        // Remover listener anterior se existir
        incomingCallsListener?.remove()
        
        incomingCallsListener = firestore.collection("calls")
            .whereEqualTo("calleeId", userId)
            .whereEqualTo("status", CallStatus.INITIATED.name.lowercase())
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Erro ao escutar chamadas recebidas", error)
                    return@addSnapshotListener
                }
                
                snapshot?.documents?.forEach { document ->
                    try {
                        val data = document.data
                        if (data != null) {
                            val call = Call.fromMap(data, document.id)
                            Log.d(TAG, "Nova chamada recebida: ${call.callId}")
                            onIncomingCall?.invoke(call)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Erro ao processar chamada recebida", e)
                    }
                }
            }
        
        Log.d(TAG, "Escutando chamadas recebidas para usuário: $userId")
    }
    
    /**
     * Para de escutar chamadas recebidas
     */
    fun stopListeningToIncomingCalls() {
        incomingCallsListener?.remove()
        incomingCallsListener = null
        Log.d(TAG, "Parou de escutar chamadas recebidas")
    }
    
    /**
     * Busca uma chamada por ID
     */
    fun getCall(
        callId: String,
        onSuccess: (Call?) -> Unit,
        onError: (Exception) -> Unit
    ) {
        firestore.collection("calls")
            .document(callId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val call = Call.fromMap(document.data!!, document.id)
                    onSuccess(call)
                } else {
                    onSuccess(null)
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Erro ao buscar chamada", e)
                onError(e)
            }
    }
}

