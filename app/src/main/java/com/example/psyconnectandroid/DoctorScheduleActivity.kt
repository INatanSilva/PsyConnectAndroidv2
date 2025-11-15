package com.example.psyconnectandroid

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class DoctorScheduleActivity : AppCompatActivity() {

    private lateinit var toolbar: Toolbar
    private lateinit var rvSchedule: RecyclerView
    private lateinit var appointmentAdapter: AppointmentAdapter

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val appointments = mutableListOf<Appointment>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_doctor_schedule)

        initializeViews()
        setupToolbar()
        setupRecyclerView()
        loadAppointments()
    }

    private fun initializeViews() {
        toolbar = findViewById(R.id.toolbarDoctorSchedule)
        rvSchedule = findViewById(R.id.rvDoctorSchedule)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        appointmentAdapter = AppointmentAdapter(appointments, UserType.PSYCHOLOGIST) { appointment ->
            val intent = Intent(this, AppointmentDetailsDoctorActivity::class.java)
            intent.putExtra("APPOINTMENT_ID", appointment.id)
            startActivity(intent)
        }
        rvSchedule.layoutManager = LinearLayoutManager(this)
        rvSchedule.adapter = appointmentAdapter
    }

    private fun loadAppointments() {
        val doctorId = auth.currentUser?.uid
        if (doctorId == null) {
            Toast.makeText(this, "Doutor não autenticado.", Toast.LENGTH_SHORT).show()
            return
        }

        firestore.collection("appointments")
            .whereEqualTo("doctorId", doctorId)
            .orderBy("startTime", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                appointments.clear()
                for (document in documents) {
                    val appointment = Appointment.fromMap(document.data, document.id)
                    appointments.add(appointment)
                }
                appointmentAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Erro ao carregar agenda: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}