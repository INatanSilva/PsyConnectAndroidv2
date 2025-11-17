package com.example.psyconnectandroid

import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * Service to interact with Stripe payment gateway API
 * Base URL: https://stripe-backend-psyconnect.onrender.com
 * 
 * This implementation follows the same structure as the iOS app
 */
object StripeService {
    
    private const val BASE_URL = "https://stripe-backend-psyconnect.onrender.com"
    private const val TAG = "StripeService"
    
    /**
     * Creates a Payment Intent for an appointment (same structure as iOS)
     * @param amount Amount in cents (e.g., 2000 for 20.00 EUR)
     * @param currency Currency code (e.g., "eur")
     * @param stripeAccountId Stripe Connect Account ID of the doctor (providerAccountId)
     * @param appointmentId ID of the appointment
     * @param description Description of the payment
     * @return PaymentIntentResponse with clientSecret, paymentIntentId, and publishableKey
     */
    fun createPaymentIntent(
        amount: Int,
        currency: String = "eur",
        stripeAccountId: String,
        appointmentId: String,
        description: String = "Consulta médica"
    ): Result<PaymentIntentResponse> {
        return try {
            val url = URL("$BASE_URL/api/create-payment-intent")
            val connection = url.openConnection() as HttpURLConnection
            
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true
            connection.doInput = true
            connection.connectTimeout = 30000 // 30 segundos para cold start
            connection.readTimeout = 30000
            
            // Create JSON body - mesma estrutura do iOS
            // Platform takes 10% from the payment
            val platformFee = (amount * 0.10).toInt()
            
            val jsonBody = JSONObject().apply {
                put("amount", amount)
                put("currency", currency)
                put("providerAccountId", stripeAccountId)  // iOS usa providerAccountId
                put("appointmentId", appointmentId)
                put("description", "$description - $appointmentId")
                put("applicationFeeAmount", platformFee)  // 10% platform fee
                put("paymentMethodTypes", JSONArray().apply {
                    put("card")
                })
            }
            
            Log.d(TAG, "🚀 Creating Payment Intent...")
            Log.d(TAG, "   URL: $url")
            Log.d(TAG, "   Body: $jsonBody")
            
            // Send request
            val writer = OutputStreamWriter(connection.outputStream)
            writer.write(jsonBody.toString())
            writer.flush()
            writer.close()
            
            // Read response
            val responseCode = connection.responseCode
            Log.d(TAG, "📡 Response Code: $responseCode")
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    response.append(line)
                }
                reader.close()
                
                val jsonResponse = JSONObject(response.toString())
                Log.d(TAG, "✅ Payment Intent Created")
                Log.d(TAG, "   Response: $jsonResponse")
                
                // iOS recebe: clientSecret, paymentIntentId, publishableKey
                val clientSecret = jsonResponse.getString("clientSecret")
                val paymentIntentId = jsonResponse.getString("paymentIntentId")
                val publishableKey = jsonResponse.getString("publishableKey")
                
                Result.success(PaymentIntentResponse(clientSecret, paymentIntentId, publishableKey))
            } else {
                val errorReader = BufferedReader(InputStreamReader(connection.errorStream ?: connection.inputStream))
                val errorResponse = StringBuilder()
                var line: String?
                while (errorReader.readLine().also { line = it } != null) {
                    errorResponse.append(line)
                }
                errorReader.close()
                
                Log.e(TAG, "❌ Error creating Payment Intent: HTTP $responseCode")
                Log.e(TAG, "   Error: $errorResponse")
                Result.failure(Exception("HTTP Error $responseCode: $errorResponse"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Exception creating Payment Intent", e)
            Result.failure(e)
        }
    }
}

/**
 * Response from creating a Payment Intent
 * @param clientSecret The client secret to use with Stripe SDK
 * @param paymentIntentId The payment intent ID
 * @param publishableKey The Stripe publishable key
 */
data class PaymentIntentResponse(
    val clientSecret: String,
    val paymentIntentId: String,
    val publishableKey: String
)
