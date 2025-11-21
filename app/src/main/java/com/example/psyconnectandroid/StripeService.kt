package com.example.psyconnectandroid

import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * Serviço para integração com backend do Stripe
 */
class StripeService {
    private val TAG = "StripeService"
    private val baseUrl = "https://stripe-backend-psyconnect.onrender.com"
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    private val gson = Gson()
    
    /**
     * Resposta de saldo da conta
     */
    data class BalanceResponse(
        val available: List<BalanceAmount>,
        val pending: List<BalanceAmount>
    )
    
    /**
     * Valor de saldo
     */
    data class BalanceAmount(
        val amount: Int,
        val currency: String
    )
    
    /**
     * Requisição de saldo
     */
    data class BalanceRequest(
        val providerAccountId: String
    )
    
    /**
     * Requisição de Payment Intent
     */
    data class PaymentIntentRequest(
        val amount: Int,
        val currency: String,
        val stripeAccountId: String,
        val appointmentId: String,
        val description: String
    )
    
    /**
     * Resposta de Payment Intent
     */
    data class PaymentIntentResponse(
        val clientSecret: String,
        val paymentIntentId: String,
        val publishableKey: String
    )
    
    /**
     * Result wrapper para operações assíncronas
     */
    sealed class Result<out T> {
        data class Success<T>(val data: T) : Result<T>()
        data class Failure(val error: Throwable) : Result<Nothing>()
        
        inline fun <R> onSuccess(action: (T) -> R): Result<R> {
            return when (this) {
                is Success -> try {
                    Success(action(data))
                } catch (e: Exception) {
                    Failure(e)
                }
                is Failure -> this
            }
        }
        
        inline fun onFailure(action: (Throwable) -> Unit) {
            if (this is Failure) {
                action(error)
            }
        }
    }
    
    /**
     * Verifica se o servidor está online
     */
    suspend fun pingServer(): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$baseUrl/health")
                .get()
                .build()
            
            val response = client.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao fazer ping no servidor", e)
            false
        }
    }
    
    /**
     * Obtém o saldo da conta Stripe
     * Retry com backoff exponencial (1s, 2s, 3s)
     */
    suspend fun getAccountBalance(providerAccountId: String): BalanceResponse? = withContext(Dispatchers.IO) {
        val delays = listOf(1000L, 2000L, 3000L)
        var lastException: Exception? = null
        
        // Health check antes da chamada
        if (!pingServer()) {
            Log.w(TAG, "⚠️ Servidor não está respondendo")
        }
        
        for ((index, delay) in delays.withIndex()) {
            try {
                val requestBody = gson.toJson(BalanceRequest(providerAccountId))
                    .toRequestBody("application/json".toMediaType())
                
                val request = Request.Builder()
                    .url("$baseUrl/api/account-balance")
                    .post(requestBody)
                    .addHeader("Content-Type", "application/json")
                    .build()
                
                val response = client.newCall(request).execute()
                
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    if (responseBody != null) {
                        val balance = gson.fromJson(responseBody, BalanceResponse::class.java)
                        Log.d(TAG, "✅ Saldo carregado: disponível=${balance.available.sumOf { it.amount }}, pendente=${balance.pending.sumOf { it.amount }}")
                        return@withContext balance
                    }
                } else {
                    Log.e(TAG, "❌ Erro na resposta: ${response.code} - ${response.message}")
                }
            } catch (e: Exception) {
                lastException = e
                Log.e(TAG, "❌ Tentativa ${index + 1} falhou", e)
                
                if (index < delays.size - 1) {
                    kotlinx.coroutines.delay(delay)
                }
            }
        }
        
        Log.e(TAG, "❌ Todas as tentativas falharam", lastException)
        null
    }
    
    /**
     * Cria um Payment Intent no Stripe
     */
    suspend fun createPaymentIntent(
        amount: Int,
        currency: String,
        stripeAccountId: String,
        appointmentId: String,
        description: String
    ): Result<PaymentIntentResponse> = withContext(Dispatchers.IO) {
        try {
            val requestBody = gson.toJson(
                PaymentIntentRequest(
                    amount = amount,
                    currency = currency,
                    stripeAccountId = stripeAccountId,
                    appointmentId = appointmentId,
                    description = description
                )
            ).toRequestBody("application/json".toMediaType())
            
            val request = Request.Builder()
                .url("$baseUrl/api/create-payment-intent")
                .post(requestBody)
                .addHeader("Content-Type", "application/json")
                .build()
            
            val response = client.newCall(request).execute()
            
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                if (responseBody != null) {
                    val paymentIntent = gson.fromJson(responseBody, PaymentIntentResponse::class.java)
                    Log.d(TAG, "✅ Payment Intent criado: ${paymentIntent.paymentIntentId}")
                    Result.Success(paymentIntent)
                } else {
                    Result.Failure(Exception("Resposta vazia do servidor"))
                }
            } else {
                val errorMessage = response.body?.string() ?: "Erro desconhecido"
                Log.e(TAG, "❌ Erro na resposta: ${response.code} - $errorMessage")
                Result.Failure(Exception("Erro ${response.code}: $errorMessage"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao criar Payment Intent", e)
            Result.Failure(e)
        }
    }
    
    companion object {
        @Volatile
        private var INSTANCE: StripeService? = null
        
        fun getInstance(): StripeService {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: StripeService().also { INSTANCE = it }
            }
        }
    }
}
