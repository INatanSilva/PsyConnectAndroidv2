package com.example.psyconnectandroid

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Date

/**
 * Serviço para gerenciar dados do doutor
 * Cache de 30 segundos
 */
class DoctorService {
    private val TAG = "DoctorService"
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    private var cacheTimestamp: Long = 0
    private val cacheTimeout = 30_000L // 30 segundos
    private var cachedPendingRequests: List<ConsultationRequest> = emptyList()
    private var cachedAcceptedRequests: List<ConsultationRequest> = emptyList()
    
    /**
     * Carrega consultas pendentes e aceitas
     */
    suspend fun loadData(): Pair<List<ConsultationRequest>, List<ConsultationRequest>> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        
        // Verificar cache
        if (now - cacheTimestamp < cacheTimeout && cachedPendingRequests.isNotEmpty()) {
            Log.d(TAG, "📦 Retornando dados do cache")
            return@withContext Pair(cachedPendingRequests, cachedAcceptedRequests)
        }
        
        try {
            val doctorId = auth.currentUser?.uid
            if (doctorId == null) {
                Log.e(TAG, "❌ Usuário não autenticado")
                return@withContext Pair(emptyList(), emptyList())
            }
            
            Log.d(TAG, "🔍 Carregando consultas para doutor: $doctorId")
            
            val appointmentsSnapshot = firestore.collection("appointments")
                .whereEqualTo("doctorId", doctorId)
                .get()
                .await()
            
            val allRequests = appointmentsSnapshot.documents.mapNotNull { document ->
                try {
                    ConsultationRequest.fromMap(document.data ?: return@mapNotNull null, document.id)
                } catch (e: Exception) {
                    Log.e(TAG, "Erro ao processar consulta ${document.id}", e)
                    null
                }
            }
            
            val pending = allRequests.filter { it.isPending }
            val accepted = allRequests.filter { it.isAccepted }
            
            // Atualizar cache
            cachedPendingRequests = pending
            cachedAcceptedRequests = accepted
            cacheTimestamp = now
            
            Log.d(TAG, "✅ Carregadas ${pending.size} pendentes e ${accepted.size} aceitas")
            
            Pair(pending, accepted)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao carregar dados", e)
            Pair(emptyList(), emptyList())
        }
    }
    
    /**
     * Aceita uma consulta
     */
    suspend fun acceptRequest(requestId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val doctorId = auth.currentUser?.uid
            if (doctorId == null) {
                Log.e(TAG, "❌ Usuário não autenticado")
                return@withContext false
            }
            
            val appointmentRef = firestore.collection("appointments").document(requestId)
            val appointment = appointmentRef.get().await()
            
            if (!appointment.exists()) {
                Log.e(TAG, "❌ Consulta não encontrada: $requestId")
                return@withContext false
            }
            
            val data = appointment.data ?: return@withContext false
            val scheduledStartTime = data["scheduledStartTime"] as? Timestamp
            val scheduledEndTime = data["scheduledEndTime"] as? Timestamp
            
            // Atualizar status do appointment
            appointmentRef.update(
                mapOf(
                    "status" to "Agendada",
                    "updatedAt" to Timestamp.now()
                )
            ).await()
            
            // Bloquear horário em doctorAvailability
            if (scheduledStartTime != null && scheduledEndTime != null) {
                val availabilityQuery = firestore.collection("doctorAvailability")
                    .whereEqualTo("doctorId", doctorId)
                    .whereEqualTo("startTime", scheduledStartTime)
                    .get()
                    .await()
                
                for (doc in availabilityQuery.documents) {
                    doc.reference.update(
                        mapOf(
                            "isBooked" to true,
                            "isAvailable" to false,
                            "appointmentId" to requestId,
                            "patientId" to (data["patientId"] as? String),
                            "patientName" to (data["patientName"] as? String),
                            "updatedAt" to Timestamp.now()
                        )
                    ).await()
                }
            }
            
            // Invalidar cache
            invalidateCache()
            
            Log.d(TAG, "✅ Consulta aceita: $requestId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao aceitar consulta", e)
            false
        }
    }
    
    /**
     * Recusa uma consulta
     */
    suspend fun rejectRequest(requestId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val appointmentRef = firestore.collection("appointments").document(requestId)
            val appointment = appointmentRef.get().await()
            
            if (!appointment.exists()) {
                Log.e(TAG, "❌ Consulta não encontrada: $requestId")
                return@withContext false
            }
            
            val data = appointment.data ?: return@withContext false
            val scheduledStartTime = data["scheduledStartTime"] as? Timestamp
            
            // Atualizar status do appointment
            appointmentRef.update(
                mapOf(
                    "status" to "Cancelada",
                    "updatedAt" to Timestamp.now()
                )
            ).await()
            
            // Liberar horário em doctorAvailability
            if (scheduledStartTime != null) {
                val doctorId = auth.currentUser?.uid ?: return@withContext false
                val availabilityQuery = firestore.collection("doctorAvailability")
                    .whereEqualTo("doctorId", doctorId)
                    .whereEqualTo("startTime", scheduledStartTime)
                    .get()
                    .await()
                
                for (doc in availabilityQuery.documents) {
                    doc.reference.update(
                        mapOf(
                            "isBooked" to false,
                            "isAvailable" to true,
                            "appointmentId" to null,
                            "patientId" to null,
                            "patientName" to null,
                            "updatedAt" to Timestamp.now()
                        )
                    ).await()
                }
            }
            
            // Invalidar cache
            invalidateCache()
            
            Log.d(TAG, "✅ Consulta recusada: $requestId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao recusar consulta", e)
            false
        }
    }
    
    /**
     * Configura listener em tempo real para consultas
     */
    fun setupRealtimeListener(
        onPendingChanged: (List<ConsultationRequest>) -> Unit,
        onAcceptedChanged: (List<ConsultationRequest>) -> Unit
    ) {
        val doctorId = auth.currentUser?.uid
        if (doctorId == null) {
            Log.e(TAG, "❌ Usuário não autenticado")
            return
        }
        
        firestore.collection("appointments")
            .whereEqualTo("doctorId", doctorId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "❌ Erro no listener", error)
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    val allRequests = snapshot.documents.mapNotNull { document ->
                        try {
                            ConsultationRequest.fromMap(document.data ?: return@mapNotNull null, document.id)
                        } catch (e: Exception) {
                            null
                        }
                    }
                    
                    val pending = allRequests.filter { it.isPending }
                    val accepted = allRequests.filter { it.isAccepted }
                    
                    onPendingChanged(pending)
                    onAcceptedChanged(accepted)
                }
            }
    }
    
    /**
     * Invalida o cache
     */
    fun invalidateCache() {
        cacheTimestamp = 0
        cachedPendingRequests = emptyList()
        cachedAcceptedRequests = emptyList()
    }
}


