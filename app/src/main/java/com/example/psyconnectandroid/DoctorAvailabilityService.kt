package com.example.psyconnectandroid

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Date

/**
 * Serviço para gerenciar disponibilidade do doutor
 * Cache de 30 segundos
 */
class DoctorAvailabilityService {
    private val TAG = "DoctorAvailabilityService"
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    private var cacheTimestamp: Long = 0
    private val cacheTimeout = 30_000L // 30 segundos
    private var cachedSlots: List<DoctorAvailability> = emptyList()
    
    /**
     * Busca disponibilidade do doutor
     */
    suspend fun fetchDoctorAvailability(doctorId: String): List<DoctorAvailability> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        
        // Verificar cache
        if (now - cacheTimestamp < cacheTimeout && cachedSlots.isNotEmpty()) {
            Log.d(TAG, "📦 Retornando slots do cache")
            return@withContext cachedSlots
        }
        
        try {
            Log.d(TAG, "🔍 Buscando disponibilidade para doutor: $doctorId")
            
            val snapshot = firestore.collection("doctorAvailability")
                .whereEqualTo("doctorId", doctorId)
                .get()
                .await()
            
            val slots = snapshot.documents.mapNotNull { document ->
                try {
                    DoctorAvailability.fromMap(document.data ?: return@mapNotNull null, document.id)
                } catch (e: Exception) {
                    Log.e(TAG, "Erro ao processar slot ${document.id}", e)
                    null
                }
            }
            
            // Atualizar cache
            cachedSlots = slots
            cacheTimestamp = now
            
            Log.d(TAG, "✅ Encontrados ${slots.size} slots")
            slots
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao buscar disponibilidade", e)
            emptyList()
        }
    }
    
    /**
     * Obtém slots disponíveis (para pacientes)
     */
    suspend fun getAvailableTimeSlots(doctorId: String, date: Date): List<DoctorAvailability> = withContext(Dispatchers.IO) {
        try {
            val calendar = Calendar.getInstance().apply {
                time = date
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val dayStart = Timestamp(calendar.time)
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            val dayEnd = Timestamp(calendar.time)
            
            val snapshot = firestore.collection("doctorAvailability")
                .whereEqualTo("doctorId", doctorId)
                .whereEqualTo("isAvailable", true)
                .whereEqualTo("isBooked", false)
                .get()
                .await()
            
            val now = Timestamp.now()
            val slots = snapshot.documents.mapNotNull { document ->
                try {
                    val slot = DoctorAvailability.fromMap(document.data ?: return@mapNotNull null, document.id)
                    // Filtrar apenas slots futuros
                    val slotTime = slot.startTime ?: slot.date
                    if (slotTime != null && slotTime.compareTo(now) > 0) {
                        slot
                    } else {
                        null
                    }
                } catch (e: Exception) {
                    null
                }
            }.filter { slot ->
                val slotDate = slot.date?.toDate() ?: slot.startTime?.toDate()
                slotDate != null && slotDate >= calendar.apply { time = date }.time && 
                slotDate < calendar.apply { add(Calendar.DAY_OF_YEAR, 1) }.time
            }
            
            slots.sortedBy { it.startTime?.toDate() ?: it.date?.toDate() }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao buscar slots disponíveis", e)
            emptyList()
        }
    }
    
    /**
     * Obtém agenda do dia
     */
    suspend fun getDoctorSchedule(doctorId: String, date: Date): List<DoctorAvailability> = withContext(Dispatchers.IO) {
        try {
            val calendar = Calendar.getInstance().apply {
                time = date
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val dayStart = Timestamp(calendar.time)
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            val dayEnd = Timestamp(calendar.time)
            
            val snapshot = firestore.collection("doctorAvailability")
                .whereEqualTo("doctorId", doctorId)
                .get()
                .await()
            
            val slots = snapshot.documents.mapNotNull { document ->
                try {
                    DoctorAvailability.fromMap(document.data ?: return@mapNotNull null, document.id)
                } catch (e: Exception) {
                    null
                }
            }.filter { slot ->
                val slotDate = slot.date?.toDate() ?: slot.startTime?.toDate()
                slotDate != null && slotDate >= calendar.apply { time = date }.time && 
                slotDate < calendar.apply { add(Calendar.DAY_OF_YEAR, 1) }.time
            }
            
            slots.sortedBy { it.startTime?.toDate() ?: it.date?.toDate() }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao buscar agenda", e)
            emptyList()
        }
    }
    
    /**
     * Cria múltiplos slots de disponibilidade
     */
    suspend fun createAvailabilitySlots(
        doctorId: String,
        date: Date,
        timeSlots: List<Pair<Date, Date>>
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val now = Timestamp.now()
            val dateTimestamp = Timestamp(date)
            
            timeSlots.forEach { (startTime, endTime) ->
                val slotId = "${doctorId}_${startTime.time}"
                val slot = DoctorAvailability(
                    doctorId = doctorId,
                    date = dateTimestamp,
                    startTime = Timestamp(startTime),
                    endTime = Timestamp(endTime),
                    isAvailable = true,
                    isBooked = false,
                    createdAt = now,
                    updatedAt = now
                )
                
                firestore.collection("doctorAvailability")
                    .document(slotId)
                    .set(DoctorAvailability.toMap(slot))
                    .await()
            }
            
            // Invalidar cache
            invalidateCache()
            
            Log.d(TAG, "✅ Criados ${timeSlots.size} slots")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao criar slots", e)
            false
        }
    }
    
    /**
     * Atualiza disponibilidade de um slot
     */
    suspend fun updateAvailability(
        slotId: String,
        isAvailable: Boolean
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            firestore.collection("doctorAvailability")
                .document(slotId)
                .update(
                    mapOf(
                        "isAvailable" to isAvailable,
                        "updatedAt" to Timestamp.now()
                    )
                )
                .await()
            
            // Invalidar cache
            invalidateCache()
            
            Log.d(TAG, "✅ Slot atualizado: $slotId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao atualizar slot", e)
            false
        }
    }
    
    /**
     * Reserva um slot
     */
    suspend fun bookTimeSlot(
        slotId: String,
        appointmentId: String,
        patientId: String,
        patientName: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            firestore.collection("doctorAvailability")
                .document(slotId)
                .update(
                    mapOf(
                        "isBooked" to true,
                        "isAvailable" to false,
                        "appointmentId" to appointmentId,
                        "patientId" to patientId,
                        "patientName" to patientName,
                        "updatedAt" to Timestamp.now()
                    )
                )
                .await()
            
            // Invalidar cache
            invalidateCache()
            
            Log.d(TAG, "✅ Slot reservado: $slotId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao reservar slot", e)
            false
        }
    }
    
    /**
     * Cancela um agendamento
     */
    suspend fun cancelAppointment(slotId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            firestore.collection("doctorAvailability")
                .document(slotId)
                .update(
                    mapOf(
                        "isBooked" to false,
                        "isAvailable" to true,
                        "appointmentId" to null,
                        "patientId" to null,
                        "patientName" to null,
                        "updatedAt" to Timestamp.now()
                    )
                )
                .await()
            
            // Invalidar cache
            invalidateCache()
            
            Log.d(TAG, "✅ Agendamento cancelado: $slotId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao cancelar agendamento", e)
            false
        }
    }
    
    /**
     * Obtém datas com horários disponíveis
     */
    suspend fun getAvailableDates(doctorId: String): List<Date> = withContext(Dispatchers.IO) {
        try {
            val now = Timestamp.now()
            val snapshot = firestore.collection("doctorAvailability")
                .whereEqualTo("doctorId", doctorId)
                .whereEqualTo("isAvailable", true)
                .whereEqualTo("isBooked", false)
                .get()
                .await()
            
            val dates = snapshot.documents.mapNotNull { document ->
                try {
                    val slot = DoctorAvailability.fromMap(document.data ?: return@mapNotNull null, document.id)
                    val slotTime = slot.startTime ?: slot.date
                    if (slotTime != null && slotTime.compareTo(now) > 0) {
                        slotTime.toDate()
                    } else {
                        null
                    }
                } catch (e: Exception) {
                    null
                }
            }.map { date ->
                val calendar = Calendar.getInstance().apply { time = date }
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                calendar.time
            }.distinct().sorted()
            
            dates
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao buscar datas disponíveis", e)
            emptyList()
        }
    }
    
    /**
     * Invalida o cache
     */
    fun invalidateCache() {
        cacheTimestamp = 0
        cachedSlots = emptyList()
    }
}


