package com.example.psyconnectandroid

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.util.Calendar

class DoctorDashboardActivity : AppCompatActivity() {

    private lateinit var toolbar: Toolbar
    private lateinit var tvWelcome: TextView
    private lateinit var btnManageAvailability: Button
    private lateinit var tvUpcomingAppointmentsCount: TextView
    private lateinit var tvTotalPatientsCount: TextView
    private lateinit var tvViewAllAppointments: TextView
    private lateinit var rvTodayAppointments: RecyclerView

    private lateinit var appointmentAdapter: AppointmentAdapter
    private val todayAppointments = mutableListOf<Appointment>()

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_doctor_dashboard)

        initializeViews()
        setupToolbar()
        setupRecyclerView()
        setupListeners()
    }
    
    override fun onResume() {
        super.onResume()
        // Reload data every time the screen is shown
        loadDoctorData()
        loadTodayAppointments()
        loadStats()
    }

    private fun initializeViews() {
        toolbar = findViewById(R.id.toolbarDoctorDashboard)
        tvWelcome = findViewById(R.id.tvDoctorWelcome)
        btnManageAvailability = findViewById(R.id.btnManageAvailability)
        tvUpcomingAppointmentsCount = findViewById(R.id.tvUpcomingAppointmentsCount)
        tvTotalPatientsCount = findViewById(R.id.tvTotalPatientsCount)
        tvViewAllAppointments = findViewById(R.id.tvViewAllAppointments)
        rvTodayAppointments = findViewById(R.id.rvTodayAppointments)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
    }

    private fun setupRecyclerView() {
        appointmentAdapter = AppointmentAdapter(todayAppointments, UserType.PSYCHOLOGIST) { appointment ->
            val intent = Intent(this, AppointmentDetailsDoctorActivity::class.java)
            intent.putExtra("APPOINTMENT_ID", appointment.id)
            startActivity(intent)
        }
        rvTodayAppointments.layoutManager = LinearLayoutManager(this)
        rvTodayAppointments.adapter = appointmentAdapter
    }
    
    private fun setupListeners() {
        btnManageAvailability.setOnClickListener {
            val intent = Intent(this, ManageAvailabilityActivity::class.java)
            startActivity(intent)
        }
        
        tvViewAllAppointments.setOnClickListener {
            val intent = Intent(this, DoctorScheduleActivity::class.java)
            startActivity(intent)
        }
    }

    private fun loadDoctorData() {
        val userId = auth.currentUser?.uid ?: return

        firestore.collection("doutores").document(userId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val doctorName = document.getString("name")
                        ?: document.getString("fullName")
                        ?: "Doutor(a)"
                    tvWelcome.text = "Olá, $doctorName!"
                } else {
                    loadDoctorDataFromUsers(userId)
                }
            }
            .addOnFailureListener {
                loadDoctorDataFromUsers(userId)
            }
    }

    private fun loadDoctorDataFromUsers(userId: String) {
        firestore.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                val doctorName = document.getString("fullName") ?: "Doutor(a)"
                tvWelcome.text = "Olá, $doctorName!"
            }
    }

    private fun loadTodayAppointments() {
        val doctorId = auth.currentUser?.uid ?: return
        
        val startOfDay = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time
        val endOfDay = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.time

        val startTimestamp = Timestamp(startOfDay)
        val endTimestamp = Timestamp(endOfDay)

        firestore.collection("appointments")
            .whereEqualTo("doctorId", doctorId)
            .whereGreaterThanOrEqualTo("startTime", startTimestamp)
            .whereLessThanOrEqualTo("startTime", endTimestamp)
            .orderBy("startTime", Query.Direction.ASCENDING)
            .get()
            .addOnSuccessListener { documents ->
                todayAppointments.clear()
                for (document in documents) {
                    todayAppointments.add(Appointment.fromMap(document.data, document.id))
                }
                appointmentAdapter.notifyDataSetChanged()
            }
    }
    
    private fun loadStats() {
        val doctorId = auth.currentUser?.uid ?: return

        firestore.collection("appointments")
            .whereEqualTo("doctorId", doctorId)
            .whereGreaterThan("startTime", Timestamp.now())
            .get()
            .addOnSuccessListener { documents ->
                tvUpcomingAppointmentsCount.text = documents.size().toString()
            }
        
        firestore.collection("appointments")
            .whereEqualTo("doctorId", doctorId)
            .get()
            .addOnSuccessListener { documents ->
                val patientIds = documents.mapNotNull { it.getString("patientId") }.distinct()
                tvTotalPatientsCount.text = patientIds.size.toString()
            }
    }
}
