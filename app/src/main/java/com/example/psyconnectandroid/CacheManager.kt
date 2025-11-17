package com.example.psyconnectandroid

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.concurrent.ConcurrentHashMap

/**
 * Gerenciador de cache centralizado para a aplicação
 * Armazena dados em memória e SharedPreferences para persistência
 */
object CacheManager {
    
    // Cache em memória (rápido)
    @PublishedApi
    internal val memoryCache = ConcurrentHashMap<String, CacheEntry>()
    
    // SharedPreferences para persistência
    @PublishedApi
    internal lateinit var prefs: SharedPreferences
    @PublishedApi
    internal val gson = Gson()
    
    // Tempo de expiração padrão (30 minutos)
    private const val DEFAULT_EXPIRATION_MS = 30 * 60 * 1000L
    
    // Keys para diferentes tipos de cache
    private const val PREF_NAME = "PsyConnectCache"
    const val CACHE_DOCTORS = "cache_doctors"
    const val CACHE_DOCTOR_PROFILES = "cache_doctor_profiles"
    const val CACHE_PATIENT_PROFILES = "cache_patient_profiles"
    const val CACHE_APPOINTMENTS = "cache_appointments"
    const val CACHE_REVIEWS = "cache_reviews"
    const val CACHE_AVAILABILITY = "cache_availability"
    
    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }
    
    /**
     * Salva um objeto no cache
     */
    fun <T> put(key: String, value: T, expirationMs: Long = DEFAULT_EXPIRATION_MS) {
        val entry = CacheEntry(
            data = gson.toJson(value),
            timestamp = System.currentTimeMillis(),
            expirationMs = expirationMs
        )
        
        // Salvar em memória
        memoryCache[key] = entry
        
        // Salvar em SharedPreferences para persistência
        prefs.edit().putString(key, gson.toJson(entry)).apply()
    }
    
    /**
     * Recupera um objeto do cache
     */
    inline fun <reified T> get(key: String): T? {
        // Tentar buscar da memória primeiro
        var entry = memoryCache[key]
        
        // Se não estiver em memória, buscar do SharedPreferences
        if (entry == null) {
            val json = prefs.getString(key, null)
            if (json != null) {
                entry = gson.fromJson(json, CacheEntry::class.java)
                memoryCache[key] = entry
            }
        }
        
        // Verificar se o cache ainda é válido
        if (entry != null && !entry.isExpired()) {
            return try {
                gson.fromJson(entry.data, object : TypeToken<T>() {}.type)
            } catch (e: Exception) {
                android.util.Log.e("CacheManager", "Error deserializing cache for key: $key", e)
                null
            }
        }
        
        // Cache expirado ou não existe
        if (entry != null) {
            remove(key)
        }
        
        return null
    }
    
    /**
     * Remove um item do cache
     */
    fun remove(key: String) {
        memoryCache.remove(key)
        prefs.edit().remove(key).apply()
    }
    
    /**
     * Limpa todo o cache
     */
    fun clear() {
        memoryCache.clear()
        prefs.edit().clear().apply()
    }
    
    /**
     * Remove itens expirados do cache
     */
    fun clearExpired() {
        val keysToRemove = mutableListOf<String>()
        
        memoryCache.forEach { (key, entry) ->
            if (entry.isExpired()) {
                keysToRemove.add(key)
            }
        }
        
        keysToRemove.forEach { remove(it) }
    }
    
    /**
     * Verifica se existe um cache válido para a key
     */
    fun has(key: String): Boolean {
        val entry = memoryCache[key] ?: run {
            val json = prefs.getString(key, null)
            if (json != null) {
                gson.fromJson(json, CacheEntry::class.java)
            } else {
                null
            }
        }
        
        return entry != null && !entry.isExpired()
    }
    
    /**
     * Classe interna para armazenar dados do cache
     */
    @PublishedApi
    internal data class CacheEntry(
        val data: String,
        val timestamp: Long,
        val expirationMs: Long
    ) {
        fun isExpired(): Boolean {
            return System.currentTimeMillis() - timestamp > expirationMs
        }
    }
}

/**
 * Cache específico para fotos de perfil
 */
object PhotoCache {
    private val cache = ConcurrentHashMap<String, String>()
    
    fun put(userId: String, photoUrl: String) {
        if (photoUrl.isNotEmpty()) {
            cache[userId] = photoUrl
        }
    }
    
    fun get(userId: String): String? {
        return cache[userId]
    }
    
    fun remove(userId: String) {
        cache.remove(userId)
    }
    
    fun clear() {
        cache.clear()
    }
}

/**
 * Cache específico para perfis de doutores
 */
object DoctorCache {
    private val cache = ConcurrentHashMap<String, Doctor>()
    
    fun put(doctorId: String, doctor: Doctor) {
        cache[doctorId] = doctor
    }
    
    fun get(doctorId: String): Doctor? {
        return cache[doctorId]
    }
    
    fun getAll(): List<Doctor> {
        return cache.values.toList()
    }
    
    fun remove(doctorId: String) {
        cache.remove(doctorId)
    }
    
    fun clear() {
        cache.clear()
    }
    
    fun size(): Int {
        return cache.size
    }
}

/**
 * Cache específico para perfis de pacientes
 */
object PatientCache {
    private val cache = ConcurrentHashMap<String, Map<String, Any>>()
    
    fun put(patientId: String, data: Map<String, Any>) {
        cache[patientId] = data
    }
    
    fun get(patientId: String): Map<String, Any>? {
        return cache[patientId]
    }
    
    fun remove(patientId: String) {
        cache.remove(patientId)
    }
    
    fun clear() {
        cache.clear()
    }
}

