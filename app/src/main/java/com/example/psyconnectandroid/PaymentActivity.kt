package com.example.psyconnectandroid

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PaymentActivity : AppCompatActivity() {

    private lateinit var webView: WebView
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
    private var sessionId: String? = null

    @SuppressLint("SetJavaScriptEnabled")
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
        initializePayment()
    }

    private fun initializeViews() {
        webView = findViewById(R.id.webViewPayment)
        progressBar = findViewById(R.id.progressBarPayment)
        toolbar = findViewById(R.id.toolbarPayment)
        
        // Enable JavaScript for Stripe
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
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
        
        // Use Checkout Session (WebView method) as it's simpler for integration
        CoroutineScope(Dispatchers.IO).launch {
            val result = StripeService.createCheckoutSession(
                amount = amount,
                currency = "eur",
                doctorId = doctorId!!,
                patientId = patientId!!,
                successUrl = "psyconnect://payment/success",
                cancelUrl = "psyconnect://payment/cancel"
            )
            
            withContext(Dispatchers.Main) {
                result.onSuccess { response ->
                    sessionId = response.sessionId
                    android.util.Log.d("PaymentActivity", "Checkout Session created: ${response.sessionUrl}")
                    loadPaymentUrl(response.sessionUrl)
                }.onFailure { error ->
                    android.util.Log.e("PaymentActivity", "Error creating Checkout Session", error)
                    progressBar.visibility = View.GONE
                    Toast.makeText(
                        this@PaymentActivity,
                        "Erro ao iniciar pagamento: ${error.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                }
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun loadPaymentUrl(url: String) {
        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                progressBar.visibility = View.VISIBLE
                
                android.util.Log.d("PaymentActivity", "Page started: $url")
                
                // Check for success/cancel URLs
                url?.let {
                    when {
                        it.startsWith("psyconnect://payment/success") -> {
                            handlePaymentSuccess()
                        }
                        it.startsWith("psyconnect://payment/cancel") -> {
                            handlePaymentCancel()
                        }
                    }
                }
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                progressBar.visibility = View.GONE
                android.util.Log.d("PaymentActivity", "Page finished: $url")
            }

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url?.toString()
                
                android.util.Log.d("PaymentActivity", "URL Loading: $url")
                
                url?.let {
                    when {
                        it.startsWith("psyconnect://payment/success") -> {
                            handlePaymentSuccess()
                            return true
                        }
                        it.startsWith("psyconnect://payment/cancel") -> {
                            handlePaymentCancel()
                            return true
                        }
                    }
                }
                
                return false
            }
        }
        
        webView.loadUrl(url)
    }

    private fun handlePaymentSuccess() {
        android.util.Log.d("PaymentActivity", "✅ Payment successful!")
        
        progressBar.visibility = View.VISIBLE
        
        // Create appointment after successful payment
        createAppointment()
    }

    private fun handlePaymentCancel() {
        android.util.Log.d("PaymentActivity", "❌ Payment cancelled")
        Toast.makeText(this, "Pagamento cancelado", Toast.LENGTH_SHORT).show()
        
        setResult(RESULT_CANCELED)
        finish()
    }

    private fun createAppointment() {
        val appointment = hashMapOf(
            "patientId" to patientId,
            "doctorId" to doctorId,
            "startTime" to appointmentStartTime,
            "endTime" to appointmentEndTime,
            "status" to "confirmed",
            "paymentStatus" to "paid",
            "paymentAmount" to amount,
            "createdAt" to com.google.firebase.Timestamp.now(),
            "doctorName" to doctorName,
            "patientName" to patientName,
            "sessionId" to sessionId
        )

        firestore.collection("appointments").add(appointment)
            .addOnSuccessListener { documentReference ->
                android.util.Log.d("PaymentActivity", "✅ Appointment created: ${documentReference.id}")
                
                // Mark slot as booked
                if (slotId != null) {
                    firestore.collection("doctorAvailability")
                        .document(slotId!!)
                        .update("isBooked", true)
                        .addOnSuccessListener {
                            android.util.Log.d("PaymentActivity", "✅ Slot marked as booked")
                        }
                }
                
                progressBar.visibility = View.GONE
                
                // Show success message
                Toast.makeText(this, "Consulta agendada e paga com sucesso!", Toast.LENGTH_LONG).show()
                
                // Return to main activity
                val intent = Intent(this, PatientActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                finish()
            }
            .addOnFailureListener { e ->
                android.util.Log.e("PaymentActivity", "❌ Error creating appointment", e)
                progressBar.visibility = View.GONE
                Toast.makeText(this, "Erro ao criar consulta: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
    
    companion object {
        const val RESULT_PAYMENT_SUCCESS = 1
        const val RESULT_PAYMENT_FAILED = 2
    }
}

