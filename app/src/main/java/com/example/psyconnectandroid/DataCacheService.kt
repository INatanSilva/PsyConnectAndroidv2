package com.example.psyconnectandroid

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.Date

/**
 * Serviço de cache para doutores promovidos
 * Implementa cache de ~5 minutos para reduzir requisições ao Firestore
 */
object DataCacheService {
    private const val TAG = "DataCacheService"
    private const val CACHE_DURATION_MS = 5 * 60 * 1000L // 5 minutos
    
    private val firestore = FirebaseFirestore.getInstance()
    
    // Cache de doutores promovidos
    private var cachedPromotedDoctors: List<Doctor> = emptyList()
    private var lastPromotedDoctorsFetch: Date? = null
    
    /**
     * Verifica se deve atualizar o cache (última busca foi há mais de 5 minutos)
     */
    private fun shouldRefreshPromotedDoctors(): Boolean {
        val lastFetch = lastPromotedDoctorsFetch ?: return true
        val now = Date()
        val timeSinceLastFetch = now.time - lastFetch.time
        return timeSinceLastFetch > CACHE_DURATION_MS
    }
    
    /**
     * Busca doutores promovidos do Firestore
     * Tenta primeiro na coleção "doctors", depois fallback para "doutores"
     */
    suspend fun fetchPromotedDoctorsFromDB(): List<Doctor> {
        val now = Timestamp.now()
        val doctors = mutableListOf<Doctor>()
        
        try {
            // 1. Buscar na coleção "doctors"
            Log.d(TAG, "🔍 Buscando doutores promovidos na coleção 'doctors'...")
            val snapshotDoctors = firestore.collection("doctors")
                .whereEqualTo("isPromoted", true)
                .whereGreaterThan("promotionExpiresAt", now)
                .get()
                .await()
            
            Log.d(TAG, "✅ Encontrados ${snapshotDoctors.size()} doutores na coleção 'doctors'")
            
            for (document in snapshotDoctors.documents) {
                try {
                    val data = document.data
                    if (data != null) {
                        val doctor = Doctor.fromMap(data, document.id)
                        if (doctor.isPromotionValid()) {
                            doctors.add(doctor)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Erro ao processar doutor ${document.id}", e)
                }
            }
            
            // 2. Fallback: buscar na coleção "doutores" se não encontrou nada
            if (doctors.isEmpty()) {
                Log.d(TAG, "⚠️ Nenhum doutor encontrado em 'doctors', tentando 'doutores'...")
                val snapshotDoutores = firestore.collection("doutores")
                    .whereEqualTo("isPromoted", true)
                    .whereGreaterThan("promotionExpiresAt", now)
                    .get()
                    .await()
                
                Log.d(TAG, "✅ Encontrados ${snapshotDoutores.size()} doutores na coleção 'doutores'")
                
                for (document in snapshotDoutores.documents) {
                    try {
                        val data = document.data
                        if (data != null) {
                            val doctor = Doctor.fromMap(data, document.id)
                            if (doctor.isPromotionValid()) {
                                doctors.add(doctor)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Erro ao processar doutor ${document.id}", e)
                    }
                }
            }
            
            // 3. Ordenar por data de expiração (mais próximos de expirar primeiro)
            doctors.sortBy { doctor ->
                doctor.promotionExpiresAt?.toDate()?.time ?: Long.MAX_VALUE
            }
            
            Log.d(TAG, "✅ Total de ${doctors.size} doutores promovidos válidos")
            return doctors
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao buscar doutores promovidos do Firestore", e)
            throw e
        }
    }
    
    /**
     * Retorna doutores promovidos do cache ou busca do Firestore se necessário
     */
    suspend fun getPromotedDoctors(): List<Doctor> {
        // Verifica se deve atualizar (cache de ~5 minutos)
        if (!shouldRefreshPromotedDoctors() && cachedPromotedDoctors.isNotEmpty()) {
            Log.d(TAG, "📦 Retornando doutores promovidos do cache (${cachedPromotedDoctors.size} doutores)")
            return cachedPromotedDoctors
        }
        
        // Buscar no banco de dados
        val doctors = fetchPromotedDoctorsFromDB()
        
        // Atualizar cache
        cachedPromotedDoctors = doctors
        lastPromotedDoctorsFetch = Date()
        
        Log.d(TAG, "✅ Cache atualizado com ${doctors.size} doutores promovidos")
        return doctors
    }
    
    /**
     * Força atualização do cache (ignora tempo de cache)
     */
    suspend fun forceRefreshPromotedDoctors(): List<Doctor> {
        Log.d(TAG, "🔄 Forçando atualização do cache de doutores promovidos...")
        val doctors = fetchPromotedDoctorsFromDB()
        
        // Atualizar cache
        cachedPromotedDoctors = doctors
        lastPromotedDoctorsFetch = Date()
        
        return doctors
    }
    
    /**
     * Limpa o cache
     */
    fun clearCache() {
        cachedPromotedDoctors = emptyList()
        lastPromotedDoctorsFetch = null
        Log.d(TAG, "🗑️ Cache limpo")
    }
}

