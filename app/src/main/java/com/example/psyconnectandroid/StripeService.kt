package com.example.psyconnectandroid

import android.util.Log
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * Service to interact with Stripe payment gateway API
 * Base URL: https://stripe-backend-psyconnect.onrender.com
 */
object StripeService {
    
    private const val BASE_URL = "https://stripe-backend-psyconnect.onrender.com"
    private const val TAG = "StripeService"
    
    /**
     * Creates a Payment Intent for an appointment
     * @param amount Amount in cents (e.g., 5000 for 50.00 EUR)
     * @param currency Currency code (e.g., "eur")
     * @param doctorId ID of the doctor
     * @param patientId ID of the patient
     * @param appointmentId ID of the appointment (optional, can be null before creation)
     * @return JSONObject with clientSecret and paymentIntentId, or null if error
     */
    fun createPaymentIntent(
        amount: Int,
        currency: String = "eur",
        doctorId: String,
        patientId: String,
        appointmentId: String? = null
    ): Result<PaymentIntentResponse> {
        return try {
            val url = URL("$BASE_URL/createPaymentIntent")
            val connection = url.openConnection() as HttpURLConnection
            
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true
            connection.doInput = true
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            
            // Create JSON body
            val jsonBody = JSONObject().apply {
                put("amount", amount)
                put("currency", currency)
                put("doctorId", doctorId)
                put("patientId", patientId)
                if (appointmentId != null) {
                    put("appointmentId", appointmentId)
                }
            }
            
            Log.d(TAG, "Creating Payment Intent: $jsonBody")
            
            // Send request
            val writer = OutputStreamWriter(connection.outputStream)
            writer.write(jsonBody.toString())
            writer.flush()
            writer.close()
            
            // Read response
            val responseCode = connection.responseCode
            Log.d(TAG, "Response Code: $responseCode")
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    response.append(line)
                }
                reader.close()
                
                val jsonResponse = JSONObject(response.toString())
                Log.d(TAG, "Payment Intent Created: $jsonResponse")
                
                val clientSecret = jsonResponse.getString("clientSecret")
                val paymentIntentId = jsonResponse.getString("paymentIntentId")
                
                Result.success(PaymentIntentResponse(clientSecret, paymentIntentId))
            } else {
                val errorReader = BufferedReader(InputStreamReader(connection.errorStream))
                val errorResponse = StringBuilder()
                var line: String?
                while (errorReader.readLine().also { line = it } != null) {
                    errorResponse.append(line)
                }
                errorReader.close()
                
                Log.e(TAG, "Error creating Payment Intent: $errorResponse")
                Result.failure(Exception("HTTP Error $responseCode: $errorResponse"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception creating Payment Intent", e)
            Result.failure(e)
        }
    }
    
    /**
     * Creates a Checkout Session (fallback method)
     * @param amount Amount in cents
     * @param currency Currency code
     * @param doctorId ID of the doctor
     * @param patientId ID of the patient
     * @param successUrl URL to redirect on success
     * @param cancelUrl URL to redirect on cancel
     * @return JSONObject with session URL and sessionId, or null if error
     */
    fun createCheckoutSession(
        amount: Int,
        currency: String = "eur",
        doctorId: String,
        patientId: String,
        successUrl: String,
        cancelUrl: String
    ): Result<CheckoutSessionResponse> {
        return try {
            val url = URL("$BASE_URL/createCheckoutSession")
            val connection = url.openConnection() as HttpURLConnection
            
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true
            connection.doInput = true
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            
            // Create JSON body
            val jsonBody = JSONObject().apply {
                put("amount", amount)
                put("currency", currency)
                put("doctorId", doctorId)
                put("patientId", patientId)
                put("successUrl", successUrl)
                put("cancelUrl", cancelUrl)
            }
            
            Log.d(TAG, "Creating Checkout Session: $jsonBody")
            
            // Send request
            val writer = OutputStreamWriter(connection.outputStream)
            writer.write(jsonBody.toString())
            writer.flush()
            writer.close()
            
            // Read response
            val responseCode = connection.responseCode
            Log.d(TAG, "Response Code: $responseCode")
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    response.append(line)
                }
                reader.close()
                
                val jsonResponse = JSONObject(response.toString())
                Log.d(TAG, "Checkout Session Created: $jsonResponse")
                
                val sessionUrl = jsonResponse.getString("url")
                val sessionId = jsonResponse.getString("sessionId")
                
                Result.success(CheckoutSessionResponse(sessionUrl, sessionId))
            } else {
                val errorReader = BufferedReader(InputStreamReader(connection.errorStream))
                val errorResponse = StringBuilder()
                var line: String?
                while (errorReader.readLine().also { line = it } != null) {
                    errorResponse.append(line)
                }
                errorReader.close()
                
                Log.e(TAG, "Error creating Checkout Session: $errorResponse")
                Result.failure(Exception("HTTP Error $responseCode: $errorResponse"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception creating Checkout Session", e)
            Result.failure(e)
        }
    }
    
    /**
     * Checks the status of a payment
     * @param paymentIntentId ID of the payment intent
     * @return Payment status information
     */
    fun checkPaymentStatus(paymentIntentId: String): Result<PaymentStatusResponse> {
        return try {
            val url = URL("$BASE_URL/checkPaymentStatus?paymentIntentId=$paymentIntentId")
            val connection = url.openConnection() as HttpURLConnection
            
            connection.requestMethod = "GET"
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            
            Log.d(TAG, "Checking Payment Status: $paymentIntentId")
            
            // Read response
            val responseCode = connection.responseCode
            Log.d(TAG, "Response Code: $responseCode")
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    response.append(line)
                }
                reader.close()
                
                val jsonResponse = JSONObject(response.toString())
                Log.d(TAG, "Payment Status: $jsonResponse")
                
                val status = jsonResponse.getString("status")
                val amount = jsonResponse.optInt("amount", 0)
                val currency = jsonResponse.optString("currency", "eur")
                
                Result.success(PaymentStatusResponse(status, amount, currency))
            } else {
                val errorReader = BufferedReader(InputStreamReader(connection.errorStream))
                val errorResponse = StringBuilder()
                var line: String?
                while (errorReader.readLine().also { line = it } != null) {
                    errorResponse.append(line)
                }
                errorReader.close()
                
                Log.e(TAG, "Error checking Payment Status: $errorResponse")
                Result.failure(Exception("HTTP Error $responseCode: $errorResponse"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception checking Payment Status", e)
            Result.failure(e)
        }
    }
    
    /**
     * Creates an onboarding link for doctors (Stripe Connect)
     * @param doctorId ID of the doctor
     * @param email Doctor's email
     * @param refreshUrl URL to redirect for refreshing
     * @param returnUrl URL to redirect after completion
     * @return Onboarding URL
     */
    fun createOnboardingLink(
        doctorId: String,
        email: String,
        refreshUrl: String,
        returnUrl: String
    ): Result<OnboardingLinkResponse> {
        return try {
            val url = URL("$BASE_URL/createOnboardingLink")
            val connection = url.openConnection() as HttpURLConnection
            
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true
            connection.doInput = true
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            
            // Create JSON body
            val jsonBody = JSONObject().apply {
                put("doctorId", doctorId)
                put("email", email)
                put("refreshUrl", refreshUrl)
                put("returnUrl", returnUrl)
            }
            
            Log.d(TAG, "Creating Onboarding Link: $jsonBody")
            
            // Send request
            val writer = OutputStreamWriter(connection.outputStream)
            writer.write(jsonBody.toString())
            writer.flush()
            writer.close()
            
            // Read response
            val responseCode = connection.responseCode
            Log.d(TAG, "Response Code: $responseCode")
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    response.append(line)
                }
                reader.close()
                
                val jsonResponse = JSONObject(response.toString())
                Log.d(TAG, "Onboarding Link Created: $jsonResponse")
                
                val onboardingUrl = jsonResponse.getString("url")
                
                Result.success(OnboardingLinkResponse(onboardingUrl))
            } else {
                val errorReader = BufferedReader(InputStreamReader(connection.errorStream))
                val errorResponse = StringBuilder()
                var line: String?
                while (errorReader.readLine().also { line = it } != null) {
                    errorResponse.append(line)
                }
                errorReader.close()
                
                Log.e(TAG, "Error creating Onboarding Link: $errorResponse")
                Result.failure(Exception("HTTP Error $responseCode: $errorResponse"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception creating Onboarding Link", e)
            Result.failure(e)
        }
    }
}

// Data classes for responses
data class PaymentIntentResponse(
    val clientSecret: String,
    val paymentIntentId: String
)

data class CheckoutSessionResponse(
    val sessionUrl: String,
    val sessionId: String
)

data class PaymentStatusResponse(
    val status: String,
    val amount: Int,
    val currency: String
)

data class OnboardingLinkResponse(
    val onboardingUrl: String
)

