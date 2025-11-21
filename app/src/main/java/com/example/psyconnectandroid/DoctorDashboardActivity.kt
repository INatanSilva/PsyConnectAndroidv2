package com.example.psyconnectandroid

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Dashboard do Doutor - Tela principal
 * Segue a estrutura do iOS conforme documentação
 */
class DoctorDashboardActivity : AppCompatActivity() {
    
    // Views - Header com métricas
    private lateinit var ivProfileImage: ImageView
    private lateinit var tvTotalConsultations: TextView
    private lateinit var tvMonthlyEarnings: TextView
    private lateinit var tvTotalPatients: TextView
    private lateinit var tvAverageRating: TextView
    
    // Views - Summary cards
    private lateinit var tvCompletedConsultations: TextView
    private lateinit var tvPendingConsultations: TextView
    private lateinit var tvCancelledConsultations: TextView
    
    // Section buttons
    private lateinit var btnOverview: Button
    private lateinit var btnConsultations: Button
    private lateinit var btnEarnings: Button
    private lateinit var btnPatients: Button
    private lateinit var btnPerformance: Button
    
    // Content layouts
    private lateinit var layoutOverview: LinearLayout
    private lateinit var layoutConsultations: LinearLayout
    private lateinit var layoutEarnings: LinearLayout
    private lateinit var layoutPatients: LinearLayout
    private lateinit var layoutPerformance: LinearLayout
    
    // RecyclerView
    private lateinit var rvRecentActivity: RecyclerView
    
    // Bottom navigation
    private lateinit var navHome: LinearLayout
    private lateinit var navPatients: LinearLayout
    private lateinit var navSchedule: LinearLayout
    private lateinit var navNotes: LinearLayout
    
    // SwipeRefreshLayout
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    
    // Services
    private val statsService = DoctorStatsService()
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    // Adapter
    private lateinit var recentActivityAdapter: RecentActivityAdapter
    
    // Current section
    private var currentSection: DashboardSection = DashboardSection.OVERVIEW
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_doctor_dashboard)
        
        initializeViews()
        setupRecyclerView()
        setupClickListeners()
        setupSwipeRefresh()
        loadProfileImage()
        loadStats()
    }
    
    override fun onResume() {
        super.onResume()
        // Recarregar estatísticas quando a tela é exibida novamente
        loadStats()
    }
    
    private fun initializeViews() {
        // Profile image
        ivProfileImage = findViewById(R.id.ivProfileImage)
        
        // Metric cards
        tvTotalConsultations = findViewById(R.id.tvTotalConsultations)
        tvMonthlyEarnings = findViewById(R.id.tvMonthlyEarnings)
        tvTotalPatients = findViewById(R.id.tvTotalPatients)
        tvAverageRating = findViewById(R.id.tvAverageRating)
        
        // Summary cards
        tvCompletedConsultations = findViewById(R.id.tvCompletedConsultations)
        tvPendingConsultations = findViewById(R.id.tvPendingConsultations)
        tvCancelledConsultations = findViewById(R.id.tvCancelledConsultations)
        
        // Section buttons
        btnOverview = findViewById(R.id.btnOverview)
        btnConsultations = findViewById(R.id.btnConsultations)
        btnEarnings = findViewById(R.id.btnEarnings)
        btnPatients = findViewById(R.id.btnPatients)
        btnPerformance = findViewById(R.id.btnPerformance)
        
        // Content layouts
        layoutOverview = findViewById(R.id.layoutOverview)
        layoutConsultations = findViewById(R.id.layoutConsultations)
        layoutEarnings = findViewById(R.id.layoutEarnings)
        layoutPatients = findViewById(R.id.layoutPatients)
        layoutPerformance = findViewById(R.id.layoutPerformance)
        
        // RecyclerView
        rvRecentActivity = findViewById(R.id.rvRecentActivity)
        
        // Bottom navigation
        navHome = findViewById(R.id.navHome)
        navPatients = findViewById(R.id.navPatients)
        navSchedule = findViewById(R.id.navSchedule)
        navNotes = findViewById(R.id.navNotes)
        
        // SwipeRefreshLayout
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)
    }
    
    private fun setupRecyclerView() {
        recentActivityAdapter = RecentActivityAdapter(emptyList())
        rvRecentActivity.layoutManager = LinearLayoutManager(this)
        rvRecentActivity.adapter = recentActivityAdapter
    }
    
    private fun setupSwipeRefresh() {
        swipeRefreshLayout.setOnRefreshListener {
            loadStats()
        }
    }
    
    private fun setupClickListeners() {
        // Profile image click - navegar para perfil
        ivProfileImage.setOnClickListener {
            val intent = Intent(this, DoctorProfileActivity::class.java)
            startActivity(intent)
        }
        
        // Section buttons
        btnOverview.setOnClickListener {
            switchSection(DashboardSection.OVERVIEW)
        }
        
        btnConsultations.setOnClickListener {
            switchSection(DashboardSection.CONSULTATIONS)
        }
        
        btnEarnings.setOnClickListener {
            switchSection(DashboardSection.EARNINGS)
        }
        
        btnPatients.setOnClickListener {
            switchSection(DashboardSection.PATIENTS)
        }
        
        btnPerformance.setOnClickListener {
            switchSection(DashboardSection.PERFORMANCE)
        }
        
        // Bottom navigation
        navHome.setOnClickListener {
            // Já está na tela inicial
        }
        
        navPatients.setOnClickListener {
            val intent = Intent(this, DoctorPatientsActivity::class.java)
            startActivity(intent)
        }
        
        navSchedule.setOnClickListener {
            val intent = Intent(this, DoctorScheduleActivity::class.java)
            startActivity(intent)
        }
        
        navNotes.setOnClickListener {
            val intent = Intent(this, DoctorNotesListActivity::class.java)
            startActivity(intent)
        }
    }
    
    /**
     * Carrega imagem do perfil
     */
    private fun loadProfileImage() {
        val doctorId = auth.currentUser?.uid
        if (doctorId == null) return
        
        firestore.collection("doutores").document(doctorId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val profileImageURL = document.getString("profileImageURL")
                    if (!profileImageURL.isNullOrEmpty()) {
                        Glide.with(this)
                            .load(profileImageURL)
                            .placeholder(R.drawable.ic_person)
                            .error(R.drawable.ic_person)
                            .circleCrop()
                            .into(ivProfileImage)
                    }
                }
            }
    }
    
    /**
     * Carrega estatísticas do doutor
     */
    private fun loadStats() {
        swipeRefreshLayout.isRefreshing = true
        
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val stats = statsService.loadStats()
                if (stats != null) {
                    updateUI(stats)
                } else {
                    android.util.Log.e("DoctorDashboard", "❌ Não foi possível carregar estatísticas")
                }
            } catch (e: Exception) {
                android.util.Log.e("DoctorDashboard", "❌ Erro ao carregar estatísticas", e)
            } finally {
                swipeRefreshLayout.isRefreshing = false
            }
        }
    }
    
    /**
     * Atualiza a UI com as estatísticas
     */
    private fun updateUI(stats: DoctorStats) {
        // Métricas principais
        tvTotalConsultations.text = stats.totalConsultations.toString()
        tvMonthlyEarnings.text = "€${String.format("%.2f", stats.monthlyEarnings)}"
        tvTotalPatients.text = stats.totalPatients.toString()
        tvAverageRating.text = String.format("%.1f", stats.averageRating)
        
        // Cards de resumo
        tvCompletedConsultations.text = stats.completedConsultations.toString()
        tvPendingConsultations.text = stats.pendingConsultations.toString()
        tvCancelledConsultations.text = stats.cancelledConsultations.toString()
        
        // Atividades recentes
        recentActivityAdapter = RecentActivityAdapter(stats.recentActivity)
        rvRecentActivity.adapter = recentActivityAdapter
        
        android.util.Log.d("DoctorDashboard", "✅ UI atualizada com ${stats.totalConsultations} consultas")
    }
    
    /**
     * Alterna entre seções
     */
    private fun switchSection(section: DashboardSection) {
        currentSection = section
        
        // Resetar todos os botões
        resetSectionButtons()
        
        // Esconder todos os layouts
        layoutOverview.visibility = View.GONE
        layoutConsultations.visibility = View.GONE
        layoutEarnings.visibility = View.GONE
        layoutPatients.visibility = View.GONE
        layoutPerformance.visibility = View.GONE
        
        // Mostrar layout selecionado e atualizar botão
        when (section) {
            DashboardSection.OVERVIEW -> {
                layoutOverview.visibility = View.VISIBLE
                btnOverview.background = getDrawable(R.drawable.bg_button_pressed)
                btnOverview.setTextColor(getColor(R.color.white))
            }
            DashboardSection.CONSULTATIONS -> {
                layoutConsultations.visibility = View.VISIBLE
                btnConsultations.background = getDrawable(R.drawable.bg_button_pressed)
                btnConsultations.setTextColor(getColor(R.color.white))
            }
            DashboardSection.EARNINGS -> {
                layoutEarnings.visibility = View.VISIBLE
                btnEarnings.background = getDrawable(R.drawable.bg_button_pressed)
                btnEarnings.setTextColor(getColor(R.color.white))
            }
            DashboardSection.PATIENTS -> {
                layoutPatients.visibility = View.VISIBLE
                btnPatients.background = getDrawable(R.drawable.bg_button_pressed)
                btnPatients.setTextColor(getColor(R.color.white))
            }
            DashboardSection.PERFORMANCE -> {
                layoutPerformance.visibility = View.VISIBLE
                btnPerformance.background = getDrawable(R.drawable.bg_button_pressed)
                btnPerformance.setTextColor(getColor(R.color.white))
            }
        }
    }
    
    /**
     * Reseta os botões de seção para o estado padrão
     */
    private fun resetSectionButtons() {
        btnOverview.background = getDrawable(R.drawable.bg_button)
        btnOverview.setTextColor(getColor(R.color.text_primary))
        
        btnConsultations.background = getDrawable(R.drawable.bg_button)
        btnConsultations.setTextColor(getColor(R.color.text_primary))
        
        btnEarnings.background = getDrawable(R.drawable.bg_button)
        btnEarnings.setTextColor(getColor(R.color.text_primary))
        
        btnPatients.background = getDrawable(R.drawable.bg_button)
        btnPatients.setTextColor(getColor(R.color.text_primary))
        
        btnPerformance.background = getDrawable(R.drawable.bg_button)
        btnPerformance.setTextColor(getColor(R.color.text_primary))
    }
}

/**
 * Enum para seções do dashboard
 */
enum class DashboardSection {
    OVERVIEW,
    CONSULTATIONS,
    EARNINGS,
    PATIENTS,
    PERFORMANCE
}
