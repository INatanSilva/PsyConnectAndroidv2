package com.example.psyconnectandroid

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.firebase.firestore.FirebaseFirestore
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * PaymentActivity - Handles payment processing using Stripe PaymentSheet
 * Same approach as iOS app
 */
class PaymentActivity : AppCompatActivity() {

    private lateinit var progressBar: ProgressBar
    private lateinit var toolbar: Toolbar
    private val firestore = FirebaseFirestore.getInstance()
    
    private var doctorId: String? = null
    private var patientId: String? = null
    private var slotId: String? = null
    private var amount: Int = 0
    private var doctorName: String? = null
    private var patientName: String? = null
    private var appointmentStartTime: com.google.firebase.Timestamp? = null
    private var appointmentEndTime: com.google.firebase.Timestamp? = null
    
    private var paymentIntentId: String? = null
    private var appointmentId: String? = null
    
    private lateinit var paymentSheet: PaymentSheet

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment)

        // Get data from intent
        doctorId = intent.getStringExtra("DOCTOR_ID")
        patientId = intent.getStringExtra("PATIENT_ID")
        slotId = intent.getStringExtra("SLOT_ID")
        amount = intent.getIntExtra("AMOUNT", 0)
        doctorName = intent.getStringExtra("DOCTOR_NAME")
        patientName = intent.getStringExtra("PATIENT_NAME")
        
        val startTimeLong = intent.getLongExtra("START_TIME", 0)
        val endTimeLong = intent.getLongExtra("END_TIME", 0)
        if (startTimeLong > 0) {
            appointmentStartTime = com.google.firebase.Timestamp(startTimeLong / 1000, 0)
        }
        if (endTimeLong > 0) {
            appointmentEndTime = com.google.firebase.Timestamp(endTimeLong / 1000, 0)
        }

        if (doctorId == null || patientId == null || amount == 0) {
            Toast.makeText(this, "Erro: Informações de pagamento inválidas.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        initializeViews()
        setupToolbar()
        
        // Initialize Stripe PaymentSheet
        paymentSheet = PaymentSheet(this, ::onPaymentSheetResult)
        
        // Start payment flow
        initializePayment()
    }

    private fun initializeViews() {
        progressBar = findViewById(R.id.progressBarPayment)
        toolbar = findViewById(R.id.toolbarPayment)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Pagamento Seguro"
        toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun initializePayment() {
        progressBar.visibility = View.VISIBLE
        
        Log.d(TAG, "🚀 Iniciando pagamento...")
        Log.d(TAG, "   Amount: $amount cents (€${amount/100.0})")
        Log.d(TAG, "   Doctor ID: $doctorId")
        Log.d(TAG, "   Patient ID: $patientId")
        
        // Generate appointment ID
        appointmentId = UUID.randomUUID().toString().uppercase()
        Log.d(TAG, "   Appointment ID: $appointmentId")
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Step 1: Get doctor's Stripe Account ID
                Log.d(TAG, "📋 Step 1: Getting doctor's Stripe Account ID...")
                val stripeAccountId = getDoctorStripeAccountId(doctorId!!)
                
                if (stripeAccountId == null) {
                    throw Exception("Médico não tem conta Stripe configurada")
                }
                
                Log.d(TAG, "✅ Stripe Account ID: $stripeAccountId")
                
                // Step 2: Create Payment Intent
                Log.d(TAG, "📋 Step 2: Creating Payment Intent...")
                val result = StripeService.createPaymentIntent(
                    amount = amount,
                    currency = "eur",
                    stripeAccountId = stripeAccountId,
                    appointmentId = appointmentId!!,
                    description = "Consulta médica"
                )
                
                result.onSuccess { response ->
                    Log.d(TAG, "✅ Payment Intent created successfully!")
                    Log.d(TAG, "   Client Secret: ${response.clientSecret}")
                    Log.d(TAG, "   Payment Intent ID: ${response.paymentIntentId}")
                    Log.d(TAG, "   Publishable Key: ${response.publishableKey}")
                    
                    paymentIntentId = response.paymentIntentId
                    
                    withContext(Dispatchers.Main) {
                        // Initialize Stripe with publishable key
                        PaymentConfiguration.init(
                            applicationContext,
                            response.publishableKey
                        )
                        
                        // Step 3: Show PaymentSheet
                        Log.d(TAG, "📋 Step 3: Presenting PaymentSheet...")
                        presentPaymentSheet(response.clientSecret)
                    }
                }.onFailure { error ->
                    Log.e(TAG, "❌ Error creating Payment Intent", error)
                    withContext(Dispatchers.Main) {
                        progressBar.visibility = View.GONE
                        Toast.makeText(
                            this@PaymentActivity,
                            "Erro ao iniciar pagamento: ${error.message}",
                            Toast.LENGTH_LONG
                        ).show()
                        finish()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Exception in payment initialization", e)
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    Toast.makeText(
                        this@PaymentActivity,
                        "Erro: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                }
            }
        }
    }

    private suspend fun getDoctorStripeAccountId(doctorId: String): String? {
        return try {
            Log.d(TAG, "   Buscando na coleção 'doutores' para ID: $doctorId")
            
            val doc = firestore.collection("doutores")
                .document(doctorId)
                .get()
                .await()
            
            Log.d(TAG, "   Document exists: ${doc.exists()}")
            
            if (!doc.exists()) {
                Log.e(TAG, "   ❌ Documento do médico não existe na coleção 'doutores'!")
                return null
            }
            
            // Log todos os dados do documento
            val allData = doc.data
            Log.d(TAG, "   📋 Todos os campos do documento:")
            allData?.forEach { (key, value) ->
                Log.d(TAG, "      $key: $value (${value?.javaClass?.simpleName})")
            }
            
            // Pegar stripeAccountId
            val stripeAccountId = doc.getString("stripeAccountId")
            Log.d(TAG, "   🔑 stripeAccountId extraído: '$stripeAccountId'")
            
            // Verificar stripeConfigured (é um boolean)
            val stripeConfigured = doc.getBoolean("stripeConfigured") ?: false
            Log.d(TAG, "   ✓ stripeConfigured: $stripeConfigured")
            
            Log.d(TAG, "   📊 Validação:")
            Log.d(TAG, "      stripeConfigured: $stripeConfigured")
            Log.d(TAG, "      stripeAccountId vazio?: ${stripeAccountId.isNullOrEmpty()}")
            
            if (stripeConfigured && !stripeAccountId.isNullOrEmpty()) {
                Log.d(TAG, "   ✅ Tudo OK! Retornando: $stripeAccountId")
                return stripeAccountId
            } else {
                Log.e(TAG, "   ❌ Falha na validação:")
                Log.e(TAG, "      stripeConfigured = $stripeConfigured (precisa ser true)")
                Log.e(TAG, "      stripeAccountId = '$stripeAccountId' (não pode estar vazio)")
                return null
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Exception getting doctor's Stripe Account ID", e)
            e.printStackTrace()
            return null
        }
    }

    private fun presentPaymentSheet(clientSecret: String) {
        progressBar.visibility = View.GONE
        
        val configuration = PaymentSheet.Configuration.Builder("PsyConnect")
            .build()
        
        paymentSheet.presentWithPaymentIntent(
            clientSecret,
            configuration
        )
    }

    private fun onPaymentSheetResult(paymentSheetResult: PaymentSheetResult) {
        when(paymentSheetResult) {
            is PaymentSheetResult.Completed -> {
                Log.d(TAG, "✅ Payment completed successfully!")
                progressBar.visibility = View.VISIBLE
                createAppointment()
            }
            is PaymentSheetResult.Canceled -> {
                Log.d(TAG, "⚠️ Payment canceled by user")
                Toast.makeText(this, "Pagamento cancelado", Toast.LENGTH_SHORT).show()
                finish()
            }
            is PaymentSheetResult.Failed -> {
                Log.e(TAG, "❌ Payment failed: ${paymentSheetResult.error.message}")
                Toast.makeText(
                    this,
                    "Erro no pagamento: ${paymentSheetResult.error.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
                finish()
            }
        }
    }

    private fun createAppointment() {
        val appointment = hashMapOf(
            "id" to appointmentId,
            "patientId" to patientId,
            "doctorId" to doctorId,
            "startTime" to appointmentStartTime,
            "endTime" to appointmentEndTime,
            "status" to "confirmed",
            "paymentStatus" to "paid",
            "paymentAmount" to amount,
            "paymentIntentId" to paymentIntentId,
            "createdAt" to com.google.firebase.Timestamp.now(),
            "doctorName" to doctorName,
            "patientName" to patientName
        )

        firestore.collection("appointments").add(appointment)
            .addOnSuccessListener { documentReference ->
                Log.d(TAG, "✅ Appointment created: ${documentReference.id}")
                
                // Mark slot as booked
                if (slotId != null) {
                    firestore.collection("doctorAvailability")
                        .document(slotId!!)
                        .update(mapOf(
                            "isBooked" to true,
                            "isAvailable" to false,
                            "patientId" to patientId,
                            "patientName" to patientName,
                            "appointmentId" to appointmentId
                        ))
                        .addOnSuccessListener {
                            Log.d(TAG, "✅ Slot marked as booked")
                        }
                }
                
                progressBar.visibility = View.GONE
                
                // Show success message
                Toast.makeText(this, "✅ Consulta agendada e paga com sucesso!", Toast.LENGTH_LONG).show()
                
                // Return to main activity
                val intent = Intent(this, PatientActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                finish()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "❌ Error creating appointment", e)
                progressBar.visibility = View.GONE
                Toast.makeText(this, "Erro ao criar consulta: ${e.message}", Toast.LENGTH_LONG).show()
                finish()
            }
    }
    
    companion object {
        private const val TAG = "PaymentActivity"
    }
}
