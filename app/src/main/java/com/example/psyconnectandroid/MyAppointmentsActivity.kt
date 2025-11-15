package com.example.psyconnectandroid

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlin.comparisons.nullsLast

class MyAppointmentsActivity : AppCompatActivity() {

    private lateinit var toolbar: Toolbar
    private lateinit var rvAppointments: RecyclerView
    private lateinit var appointmentAdapter: AppointmentAdapter

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val appointments = mutableListOf<Appointment>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_appointments)

        initializeViews()
        setupToolbar()
        setupRecyclerView()
        loadAppointments()
    }

    private fun initializeViews() {
        toolbar = findViewById(R.id.toolbarAppointments)
        rvAppointments = findViewById(R.id.rvAppointments)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener {
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
                    appointmentAdapter.notifyDataSetChanged()
                    return@addOnSuccessListener
                }
                
                // Processar cada documento
                for (document in querySnapshot.documents) {
                    try {
                        val data = document.data
                        if (data != null) {
                            val appointment = Appointment.fromMap(data, document.id)
                            appointments.add(appointment)
                            android.util.Log.d("MyAppointmentsActivity", "✅ Added appointment: ${appointment.doctorName} - ${appointment.startTime}")
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("MyAppointmentsActivity", "❌ Error parsing appointment ${document.id}", e)
                    }
                }
                
                // Ordenar manualmente por data/hora (mais recentes primeiro)
                appointments.sortWith(compareBy(nullsLast()) { it.startTime })
                appointments.reverse() // Para ter as mais recentes primeiro (DESCENDING)
                
                android.util.Log.d("MyAppointmentsActivity", "✅ Final appointments count: ${appointments.size}")
                appointmentAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                android.util.Log.e("MyAppointmentsActivity", "❌ Error loading appointments", e)
                Toast.makeText(this, "Erro ao carregar consultas: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
}