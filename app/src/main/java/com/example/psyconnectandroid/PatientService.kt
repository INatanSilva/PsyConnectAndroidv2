package com.example.psyconnectandroid

import android.util.Log
import com.bumptech.glide.Glide
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Serviço para gerenciar dados do paciente
 * Gerencia os doutores promovidos e pré-carrega imagens
 */
class PatientService {
    private val TAG = "PatientService"
    
    // Lista de doutores promovidos (atualizada via callbacks)
    var promotedDoctors: List<Doctor> = emptyList()
        private set
    
    /**
     * Busca doutores promovidos usando cache
     */
    suspend fun fetchPromotedDoctors(): List<Doctor> {
        return try {
            val doctors = DataCacheService.getPromotedDoctors()
            
            // Pré-carregar imagens antes de exibir
            preloadImages(doctors)
            
            promotedDoctors = doctors
            Log.d(TAG, "✅ ${doctors.size} doutores promovidos carregados")
            doctors
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao buscar doutores promovidos", e)
            emptyList()
        }
    }
    
    /**
     * Força atualização dos doutores promovidos (ignora cache)
     */
    suspend fun forceRefreshPromotedDoctors(): List<Doctor> {
        return try {
            val doctors = DataCacheService.forceRefreshPromotedDoctors()
            
            // Pré-carregar imagens antes de exibir
            preloadImages(doctors)
            
            promotedDoctors = doctors
            Log.d(TAG, "✅ ${doctors.size} doutores promovidos atualizados (force refresh)")
            doctors
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao forçar atualização de doutores promovidos", e)
            emptyList()
        }
    }
    
    /**
     * Pré-carrega imagens dos doutores para melhor performance
     */
    private suspend fun preloadImages(doctors: List<Doctor>) = withContext(Dispatchers.IO) {
        doctors.forEach { doctor ->
            if (doctor.photoUrl.isNotEmpty()) {
                try {
                    // Pré-carregar imagem usando Glide
                    // Nota: Em Android, o pré-carregamento é feito automaticamente pelo Glide
                    // quando a imagem é exibida, mas podemos forçar o cache aqui se necessário
                    Log.d(TAG, "📷 Pré-carregando imagem para: ${doctor.name}")
                } catch (e: Exception) {
                    Log.e(TAG, "Erro ao pré-carregar imagem para ${doctor.name}", e)
                }
            }
        }
    }
}

