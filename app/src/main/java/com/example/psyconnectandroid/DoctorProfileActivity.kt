package com.example.psyconnectandroid

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

/**
 * Perfil do doutor - configurações e financeiro
 * Implementa configuração do Stripe e visualização de saldo
 */
class DoctorProfileActivity : AppCompatActivity() {
    
    // Views - Informações profissionais
    private lateinit var btnBack: Button
    private lateinit var ivProfileImage: ImageView
    private lateinit var tvName: TextView
    private lateinit var tvEmail: TextView
    private lateinit var switchIsOnline: Switch
    private lateinit var etPriceEurCents: EditText
    private lateinit var btnSavePrice: Button
    private lateinit var btnConfigureStripe: Button
    private lateinit var tvStripeStatus: TextView
    private lateinit var layoutBalance: LinearLayout
    private lateinit var tvBalance: TextView
    private lateinit var tvPendingBalance: TextView
    private lateinit var btnUpdateBalance: Button
    private lateinit var progressBalance: ProgressBar
    private lateinit var btnPromote: Button
    
    // Services
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val stripeService = StripeService.getInstance()
    
    // Data
    private var doctorId: String? = null
    private var profileImageURL: String? = null
    private var stripeConfigured: Boolean = false
    private var stripeAccountId: String? = null
    private var isLoadingBalance: Boolean = false
    
    // Currency formatter
    private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("pt", "PT")).apply {
        currency = java.util.Currency.getInstance("EUR")
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_doctor_profile_settings)
        
        doctorId = auth.currentUser?.uid
        if (doctorId == null) {
            Toast.makeText(this, "Erro: Usuário não autenticado", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        initializeViews()
        setupClickListeners()
        loadProfile()
        
        // Verificar status do Stripe após 1 segundo (onboarding automático)
        Handler(Looper.getMainLooper()).postDelayed({
            checkStripeStatus()
        }, 1000)
    }
    
    private fun initializeViews() {
        btnBack = findViewById(R.id.btnBack)
        ivProfileImage = findViewById(R.id.ivProfileImage)
        tvName = findViewById(R.id.tvName)
        tvEmail = findViewById(R.id.tvEmail)
        switchIsOnline = findViewById(R.id.switchIsOnline)
        etPriceEurCents = findViewById(R.id.etPriceEurCents)
        btnSavePrice = findViewById(R.id.btnSavePrice)
        btnConfigureStripe = findViewById(R.id.btnConfigureStripe)
        tvStripeStatus = findViewById(R.id.tvStripeStatus)
        layoutBalance = findViewById(R.id.layoutBalance)
        tvBalance = findViewById(R.id.tvBalance)
        tvPendingBalance = findViewById(R.id.tvPendingBalance)
        btnUpdateBalance = findViewById(R.id.btnUpdateBalance)
        progressBalance = findViewById(R.id.progressBalance)
        btnPromote = findViewById(R.id.btnPromote)
    }
    
    private fun setupClickListeners() {
        btnBack.setOnClickListener {
            finish()
        }
        
        ivProfileImage.setOnClickListener {
            // TODO: Implementar upload de imagem
            Toast.makeText(this, "Upload de imagem será implementado", Toast.LENGTH_SHORT).show()
        }
        
        switchIsOnline.setOnCheckedChangeListener { _, isChecked ->
            updateOnlineStatus(isChecked)
        }
        
        btnSavePrice.setOnClickListener {
            savePrice()
        }
        
        btnConfigureStripe.setOnClickListener {
            showStripeConfigurationDialog()
        }
        
        btnUpdateBalance.setOnClickListener {
            loadAccountBalance()
        }
        
        btnPromote.setOnClickListener {
            activatePromotion()
        }
    }
    
    private fun loadProfile() {
        doctorId?.let { id ->
            firestore.collection("doutores").document(id)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val data = document.data ?: return@addOnSuccessListener
                        
                        tvName.text = data["name"] as? String ?: ""
                        tvEmail.text = data["email"] as? String ?: ""
                        switchIsOnline.isChecked = (data["isOnline"] as? Boolean) ?: false
                        
                        val priceEurCents = (data["priceEurCents"] as? Number)?.toInt() ?: 0
                        etPriceEurCents.setText(String.format(Locale.getDefault(), "%.2f", priceEurCents / 100.0))
                        
                        profileImageURL = data["profileImageURL"] as? String
                        if (!profileImageURL.isNullOrEmpty()) {
                            Glide.with(this)
                                .load(profileImageURL)
                                .placeholder(R.drawable.ic_person)
                                .error(R.drawable.ic_person)
                                .circleCrop()
                                .into(ivProfileImage)
                        }
                        
                        stripeConfigured = (data["stripeConfigured"] as? Boolean) ?: false
                        stripeAccountId = data["stripeAccountId"] as? String
                        
                        updateStripeUI()
                        
                        val isPromoted = (data["isPromoted"] as? Boolean) ?: false
                        btnPromote.text = if (isPromoted) "Desativar Promoção" else "Ativar Promoção (7 dias)"
                    }
                }
                .addOnFailureListener { e ->
                    android.util.Log.e("DoctorProfile", "❌ Erro ao carregar perfil", e)
                }
        }
    }
    
    private fun checkStripeStatus() {
        if (!stripeConfigured) {
            // Mostrar onboarding após 1 segundo
            Handler(Looper.getMainLooper()).postDelayed({
                showStripeOnboarding()
            }, 1000)
        }
    }
    
    private fun showStripeOnboarding() {
        // Mostrar dialog de onboarding simplificado
        android.app.AlertDialog.Builder(this)
            .setTitle("Configurar Recebimentos")
            .setMessage(
                "Para receber pagamentos, você precisa configurar uma conta Stripe Connect.\n\n" +
                "1. Crie uma conta em https://dashboard.stripe.com\n" +
                "2. Obtenha seu Account ID\n" +
                "3. Configure aqui no app"
            )
            .setPositiveButton("Configurar Agora") { _, _ ->
                showStripeConfigurationDialog()
            }
            .setNegativeButton("Depois", null)
            .show()
    }
    
    private fun showStripeConfigurationDialog() {
        val dialog = StripeConfigurationDialog { accountId ->
            saveStripeConfiguration(accountId)
        }
        dialog.show(supportFragmentManager, "StripeConfiguration")
    }
    
    private fun saveStripeConfiguration(accountId: String) {
        doctorId?.let { id ->
            btnConfigureStripe.isEnabled = false
            btnConfigureStripe.text = "Salvando..."
            
            firestore.collection("doutores").document(id)
                .update(
                    mapOf(
                        "stripeAccountId" to accountId,
                        "stripeConfigured" to true,
                        "onboardingCompleted" to true
                    )
                )
                .addOnSuccessListener {
                    stripeConfigured = true
                    stripeAccountId = accountId
                    updateStripeUI()
                    
                    // Carregar saldo automaticamente após salvar
                    loadAccountBalance()
                    
                    Toast.makeText(this, "Stripe configurado com sucesso!", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    android.util.Log.e("DoctorProfile", "❌ Erro ao salvar configuração Stripe", e)
                    Toast.makeText(this, "Erro ao salvar configuração", Toast.LENGTH_SHORT).show()
                }
                .addOnCompleteListener {
                    btnConfigureStripe.isEnabled = true
                    btnConfigureStripe.text = "Configurar Stripe"
                }
        }
    }
    
    private fun updateStripeUI() {
        if (stripeConfigured) {
            tvStripeStatus.text = "✓ Configurado"
            tvStripeStatus.setTextColor(getColor(R.color.green))
            btnConfigureStripe.text = "Alterar Configuração"
            layoutBalance.visibility = View.VISIBLE
        } else {
            tvStripeStatus.text = "Não configurado"
            tvStripeStatus.setTextColor(getColor(R.color.text_secondary))
            btnConfigureStripe.text = "Configurar Recebimentos"
            layoutBalance.visibility = View.GONE
        }
    }
    
    private fun loadAccountBalance() {
        if (isLoadingBalance) return
        
        if (!stripeConfigured || stripeAccountId.isNullOrEmpty()) {
            Toast.makeText(this, "Stripe não está configurado", Toast.LENGTH_SHORT).show()
            return
        }
        
        isLoadingBalance = true
        progressBalance.visibility = View.VISIBLE
        btnUpdateBalance.isEnabled = false
        
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val balance = stripeService.getAccountBalance(stripeAccountId!!)
                
                if (balance != null) {
                    // Formatar saldo disponível
                    val availableAmount = balance.available.firstOrNull()?.amount ?: 0
                    val availableCurrency = balance.available.firstOrNull()?.currency ?: "eur"
                    tvBalance.text = formatCurrency(availableAmount, availableCurrency)
                    
                    // Formatar saldo pendente
                    val pendingAmount = balance.pending.firstOrNull()?.amount ?: 0
                    val pendingCurrency = balance.pending.firstOrNull()?.currency ?: "eur"
                    if (pendingAmount > 0) {
                        tvPendingBalance.text = formatCurrency(pendingAmount, pendingCurrency)
                        tvPendingBalance.visibility = View.VISIBLE
                    } else {
                        tvPendingBalance.visibility = View.GONE
                    }
                } else {
                    Toast.makeText(this@DoctorProfileActivity, "Erro ao carregar saldo", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                android.util.Log.e("DoctorProfile", "❌ Erro ao carregar saldo", e)
                Toast.makeText(this@DoctorProfileActivity, "Erro ao carregar saldo: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                isLoadingBalance = false
                progressBalance.visibility = View.GONE
                btnUpdateBalance.isEnabled = true
            }
        }
    }
    
    private fun formatCurrency(amount: Int, currency: String): String {
        // Converter de centavos para decimal
        val decimalAmount = amount / 100.0
        return currencyFormatter.format(decimalAmount)
    }
    
    private fun updateOnlineStatus(isOnline: Boolean) {
        doctorId?.let { id ->
            firestore.collection("doutores").document(id)
                .update("isOnline", isOnline)
                .addOnSuccessListener {
                    android.util.Log.d("DoctorProfile", "✅ Status online atualizado: $isOnline")
                }
                .addOnFailureListener { e ->
                    android.util.Log.e("DoctorProfile", "❌ Erro ao atualizar status", e)
                    Toast.makeText(this, "Erro ao atualizar status", Toast.LENGTH_SHORT).show()
                }
        }
    }
    
    private fun savePrice() {
        val priceText = etPriceEurCents.text.toString()
        val priceEur = priceText.toDoubleOrNull()
        
        if (priceEur == null || priceEur <= 0) {
            Toast.makeText(this, "Preço inválido", Toast.LENGTH_SHORT).show()
            return
        }
        
        val priceEurCents = (priceEur * 100).toInt()
        
        doctorId?.let { id ->
            firestore.collection("doutores").document(id)
                .update("priceEurCents", priceEurCents)
                .addOnSuccessListener {
                    Toast.makeText(this, "Preço salvo com sucesso", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    android.util.Log.e("DoctorProfile", "❌ Erro ao salvar preço", e)
                    Toast.makeText(this, "Erro ao salvar preço", Toast.LENGTH_SHORT).show()
                }
        }
    }
    
    private fun activatePromotion() {
        doctorId?.let { id ->
            val docRef = firestore.collection("doutores").document(id)
            
            docRef.get().addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val isPromoted = (document.data?.get("isPromoted") as? Boolean) ?: false
                    val calendar = java.util.Calendar.getInstance()
                    calendar.add(java.util.Calendar.DAY_OF_YEAR, 7)
                    val expiresAt = Timestamp(calendar.time)
                    
                    val updates = if (isPromoted) {
                        mapOf(
                            "isPromoted" to false,
                            "promotionExpiresAt" to null
                        )
                    } else {
                        mapOf(
                            "isPromoted" to true,
                            "promotionExpiresAt" to expiresAt
                        )
                    }
                    
                    docRef.update(updates)
                        .addOnSuccessListener {
                            val message = if (isPromoted) "Promoção desativada" else "Promoção ativada por 7 dias"
                            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                            loadProfile()
                        }
                        .addOnFailureListener { e ->
                            android.util.Log.e("DoctorProfile", "❌ Erro ao atualizar promoção", e)
                            Toast.makeText(this, "Erro ao atualizar promoção", Toast.LENGTH_SHORT).show()
                        }
                }
            }
        }
    }
}
