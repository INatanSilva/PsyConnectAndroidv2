package com.example.psyconnectandroid

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.comparisons.nullsLast

class MyAppointmentsActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageView
    private lateinit var rvAppointments: RecyclerView
    private lateinit var emptyStateLayout: LinearLayout
    private lateinit var cardPendingRequests: androidx.cardview.widget.CardView
    private lateinit var tvAppointmentsCount: TextView
    private lateinit var appointmentAdapter: AppointmentAdapter

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val appointments = mutableListOf<Appointment>()
    private val pendingAppointments = mutableListOf<Appointment>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_appointments)

        initializeViews()
        setupBackButton()
        setupRecyclerView()
        loadAppointments()
    }

    private fun initializeViews() {
        btnBack = findViewById(R.id.btnBack)
        rvAppointments = findViewById(R.id.rvAppointments)
        emptyStateLayout = findViewById(R.id.emptyStateLayout)
        cardPendingRequests = findViewById(R.id.cardPendingRequests)
        tvAppointmentsCount = findViewById(R.id.tvAppointmentsCount)
    }

    private fun setupBackButton() {
        btnBack.setOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        appointmentAdapter = AppointmentAdapter(appointments, UserType.PATIENT) { appointment ->
            // Handle click on an appointment, e.g., navigate to a detail screen
            Toast.makeText(this, "Clicked on appointment with ${appointment.doctorName}", Toast.LENGTH_SHORT).show()
        }
        rvAppointments.layoutManager = LinearLayoutManager(this)
        rvAppointments.adapter = appointmentAdapter
    }

    private fun loadAppointments() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "Usuário não autenticado.", Toast.LENGTH_SHORT).show()
            return
        }

        android.util.Log.d("MyAppointmentsActivity", "🔍 Loading appointments for userId: $userId")

        // Buscar todas as consultas sem orderBy para evitar necessidade de índice composto
        firestore.collection("appointments")
            .whereEqualTo("patientId", userId)
            .get()
            .addOnSuccessListener { querySnapshot ->
                android.util.Log.d("MyAppointmentsActivity", "✅ Found ${querySnapshot.size()} total appointments")
                
                appointments.clear()
                
                if (querySnapshot.isEmpty) {
                    android.util.Log.d("MyAppointmentsActivity", "⚠️ No appointments found")
                    pendingAppointments.clear()
                    appointments.clear()
                    updateUI()
                    return@addOnSuccessListener
                }
                
                // Separar pendentes e agendadas
                pendingAppointments.clear()
                appointments.clear()
                
                for (document in querySnapshot.documents) {
                    try {
                        val data = document.data
                        if (data != null) {
                            val appointment = Appointment.fromMap(data, document.id)
                            val status = appointment.status.lowercase()
                            
                            // Pendentes: status "pending" ou sem status definido
                            if (status == "pending" || status == "pendente" || status.isEmpty()) {
                                pendingAppointments.add(appointment)
                            } else {
                                // Agendadas: status "agendada", "scheduled", "confirmed"
                                if (status in listOf("agendada", "scheduled", "confirmed", "confirmada")) {
                                    appointments.add(appointment)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("MyAppointmentsActivity", "❌ Error parsing appointment ${document.id}", e)
                    }
                }
                
                // Ordenar agendadas por data/hora (mais próximas primeiro)
                appointments.sortWith(compareBy(nullsLast()) { it.startTime })
                
                android.util.Log.d("MyAppointmentsActivity", "✅ Pending: ${pendingAppointments.size}, Scheduled: ${appointments.size}")
                
                updateUI()
            }
            .addOnFailureListener { e ->
                android.util.Log.e("MyAppointmentsActivity", "❌ Error loading appointments", e)
                Toast.makeText(this, "Erro ao carregar consultas: ${e.message}", Toast.LENGTH_LONG).show()
                pendingAppointments.clear()
                appointments.clear()
                updateUI()
            }
    }
    
    private fun updateUI() {
        // Atualizar seção de pendentes
        if (pendingAppointments.isEmpty()) {
            cardPendingRequests.visibility = View.VISIBLE
        } else {
            // TODO: Mostrar lista de pendentes se houver
            cardPendingRequests.visibility = View.VISIBLE
        }
        
        // Atualizar contagem e lista de agendadas
        if (appointments.isNotEmpty()) {
            tvAppointmentsCount.text = appointments.size.toString()
            tvAppointmentsCount.visibility = View.VISIBLE
            rvAppointments.visibility = View.VISIBLE
            emptyStateLayout.visibility = View.GONE
            appointmentAdapter.notifyDataSetChanged()
        } else {
            tvAppointmentsCount.visibility = View.GONE
            rvAppointments.visibility = View.GONE
            emptyStateLayout.visibility = View.VISIBLE
        }
    }
}