package com.example.psyconnectandroid

import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Serviço para gerar tokens de autenticação do Agora
 */
object TokenService {
    private const val TAG = "TokenService"
    private const val BASE_URL = "https://agora-token-server-o5im.onrender.com"
    private const val TIMEOUT_SECONDS = 10L
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()
    
    private val gson = Gson()
    
    data class TokenRequest(
        val channelName: String,
        val uid: String,
        val role: String = "publisher",
        val expireTime: Int = 3600
    )
    
    data class TokenResponse(
        val token: String,
        val appId: String,
        val channelName: String,
        val uid: String,
        val expireTime: Long
    )
    
    /**
     * Verifica se o servidor está disponível
     */
    suspend fun checkServerHealth(): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$BASE_URL/health")
                .get()
                .build()
            
            val response = client.newCall(request).execute()
            val isHealthy = response.isSuccessful
            response.close()
            
            Log.d(TAG, "Server health check: $isHealthy")
            isHealthy
        } catch (e: Exception) {
            Log.e(TAG, "Error checking server health", e)
            false
        }
    }
    
    /**
     * Gera token RTC dinamicamente via servidor
     */
    suspend fun generateRTCToken(
        channelName: String,
        uid: String,
        role: String = "publisher",
        expireTime: Int = 3600
    ): String? = withContext(Dispatchers.IO) {
        try {
            val requestBody = TokenRequest(
                channelName = channelName,
                uid = uid,
                role = role,
                expireTime = expireTime
            )
            
            val json = gson.toJson(requestBody)
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = json.toRequestBody(mediaType)
            
            val request = Request.Builder()
                .url("$BASE_URL/api/tokens/rtc")
                .post(body)
                .addHeader("Content-Type", "application/json")
                .build()
            
            val response = client.newCall(request).execute()
            
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                val tokenResponse = gson.fromJson(responseBody, TokenResponse::class.java)
                Log.d(TAG, "Token generated successfully for channel: $channelName")
                response.close()
                tokenResponse.token
            } else {
                Log.e(TAG, "Failed to generate token: ${response.code} ${response.message}")
                response.close()
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error generating RTC token", e)
            null
        }
    }
}

