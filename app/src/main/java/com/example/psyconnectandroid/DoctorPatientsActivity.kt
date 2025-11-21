package com.example.psyconnectandroid

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Tela de pacientes do doutor
 * Gerencia solicitações de consulta e pacientes aceitos
 */
class DoctorPatientsActivity : AppCompatActivity() {
    
    // Views
    private lateinit var tvPendingCount: TextView
    private lateinit var tvAcceptedCount: TextView
    private lateinit var tvTotalCount: TextView
    private lateinit var btnPending: Button
    private lateinit var btnAccepted: Button
    private lateinit var rvRequests: RecyclerView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    
    // Services
    private val doctorService = DoctorService()
    
    // Adapter
    private lateinit var adapter: ConsultationRequestAdapter
    
    // Data
    private var pendingRequests: List<ConsultationRequest> = emptyList()
    private var acceptedRequests: List<ConsultationRequest> = emptyList()
    private var currentTab: TabType = TabType.PENDING
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_doctor_patients)
        
        initializeViews()
        setupRecyclerView()
        setupClickListeners()
        setupSwipeRefresh()
        loadData()
        
        // Setup realtime listener
        setupRealtimeListener()
    }
    
    private fun initializeViews() {
        tvPendingCount = findViewById(R.id.tvPendingCount)
        tvAcceptedCount = findViewById(R.id.tvAcceptedCount)
        tvTotalCount = findViewById(R.id.tvTotalCount)
        btnPending = findViewById(R.id.btnPending)
        btnAccepted = findViewById(R.id.btnAccepted)
        rvRequests = findViewById(R.id.rvRequests)
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)
    }
    
    private fun setupRecyclerView() {
        adapter = ConsultationRequestAdapter(
            requests = emptyList(),
            onAcceptClick = { request ->
                acceptRequest(request)
            },
            onRejectClick = { request ->
                rejectRequest(request)
            },
            onCallClick = { request ->
                startCall(request)
            },
            onChatClick = { request ->
                startChat(request)
            },
            onNotesClick = { request ->
                openNotes(request)
            }
        )
        rvRequests.layoutManager = LinearLayoutManager(this)
        rvRequests.adapter = adapter
    }
    
    private fun setupClickListeners() {
        btnPending.setOnClickListener {
            switchTab(TabType.PENDING)
        }
        
        btnAccepted.setOnClickListener {
            switchTab(TabType.ACCEPTED)
        }
    }
    
    private fun setupSwipeRefresh() {
        swipeRefreshLayout.setOnRefreshListener {
            loadData()
        }
    }
    
    private fun setupRealtimeListener() {
        doctorService.setupRealtimeListener(
            onPendingChanged = { pending ->
                pendingRequests = pending
                updateUI()
            },
            onAcceptedChanged = { accepted ->
                acceptedRequests = accepted
                updateUI()
            }
        )
    }
    
    private fun loadData() {
        swipeRefreshLayout.isRefreshing = true
        
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val (pending, accepted) = doctorService.loadData()
                pendingRequests = pending
                acceptedRequests = accepted
                updateUI()
            } catch (e: Exception) {
                android.util.Log.e("DoctorPatients", "❌ Erro ao carregar dados", e)
                Toast.makeText(this@DoctorPatientsActivity, "Erro ao carregar dados", Toast.LENGTH_SHORT).show()
            } finally {
                swipeRefreshLayout.isRefreshing = false
            }
        }
    }
    
    private fun updateUI() {
        // Atualizar contadores
        tvPendingCount.text = pendingRequests.size.toString()
        tvAcceptedCount.text = acceptedRequests.size.toString()
        tvTotalCount.text = (pendingRequests.size + acceptedRequests.size).toString()
        
        // Atualizar lista
        val currentList = when (currentTab) {
            TabType.PENDING -> pendingRequests
            TabType.ACCEPTED -> acceptedRequests
        }
        adapter = ConsultationRequestAdapter(
            requests = currentList,
            onAcceptClick = { request ->
                acceptRequest(request)
            },
            onRejectClick = { request ->
                rejectRequest(request)
            },
            onCallClick = { request ->
                startCall(request)
            },
            onChatClick = { request ->
                startChat(request)
            },
            onNotesClick = { request ->
                openNotes(request)
            }
        )
        rvRequests.adapter = adapter
    }
    
    private fun switchTab(tab: TabType) {
        currentTab = tab
        
        // Atualizar botões
        when (tab) {
            TabType.PENDING -> {
                btnPending.background = getDrawable(R.drawable.bg_button_pressed)
                btnPending.setTextColor(getColor(R.color.white))
                btnAccepted.background = getDrawable(R.drawable.bg_button)
                btnAccepted.setTextColor(getColor(R.color.text_primary))
            }
            TabType.ACCEPTED -> {
                btnPending.background = getDrawable(R.drawable.bg_button)
                btnPending.setTextColor(getColor(R.color.text_primary))
                btnAccepted.background = getDrawable(R.drawable.bg_button_pressed)
                btnAccepted.setTextColor(getColor(R.color.white))
            }
        }
        
        updateUI()
    }
    
    private fun acceptRequest(request: ConsultationRequest) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val success = doctorService.acceptRequest(request.id)
                if (success) {
                    Toast.makeText(this@DoctorPatientsActivity, "Consulta aceita", Toast.LENGTH_SHORT).show()
                    loadData()
                } else {
                    Toast.makeText(this@DoctorPatientsActivity, "Erro ao aceitar consulta", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                android.util.Log.e("DoctorPatients", "❌ Erro ao aceitar consulta", e)
                Toast.makeText(this@DoctorPatientsActivity, "Erro ao aceitar consulta", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun rejectRequest(request: ConsultationRequest) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val success = doctorService.rejectRequest(request.id)
                if (success) {
                    Toast.makeText(this@DoctorPatientsActivity, "Consulta recusada", Toast.LENGTH_SHORT).show()
                    loadData()
                } else {
                    Toast.makeText(this@DoctorPatientsActivity, "Erro ao recusar consulta", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                android.util.Log.e("DoctorPatients", "❌ Erro ao recusar consulta", e)
                Toast.makeText(this@DoctorPatientsActivity, "Erro ao recusar consulta", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun startCall(request: ConsultationRequest) {
        // TODO: Implementar chamada de vídeo
        val intent = Intent(this, CallActivity::class.java)
        intent.putExtra("PATIENT_ID", request.patientId)
        intent.putExtra("APPOINTMENT_ID", request.id)
        startActivity(intent)
    }
    
    private fun startChat(request: ConsultationRequest) {
        val intent = Intent(this, ChatActivity::class.java)
        intent.putExtra("PATIENT_ID", request.patientId)
        intent.putExtra("PATIENT_NAME", request.patientName)
        startActivity(intent)
    }
    
    private fun openNotes(request: ConsultationRequest) {
        val intent = Intent(this, PatientNoteActivity::class.java)
        intent.putExtra("PATIENT_ID", request.patientId)
        intent.putExtra("PATIENT_NAME", request.patientName)
        startActivity(intent)
    }
    
    private enum class TabType {
        PENDING,
        ACCEPTED
    }
}


