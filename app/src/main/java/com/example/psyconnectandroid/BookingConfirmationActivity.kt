package com.example.psyconnectandroid

import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Locale

class BookingConfirmationActivity : AppCompatActivity() {

    private lateinit var toolbar: Toolbar
    private lateinit var ivDoctorPhoto: ImageView
    private lateinit var tvDoctorName: TextView
    private lateinit var tvDoctorSpecialization: TextView
    private lateinit var tvDate: TextView
    private lateinit var tvTime: TextView
    private lateinit var tvPrice: TextView
    private lateinit var tvTotal: TextView
    private lateinit var btnConfirm: Button

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private var doctorId: String? = null
    private var slotId: String? = null
    private var doctor: Doctor? = null
    private var slot: AvailabilitySlot? = null
    private var patientName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_booking_confirmation)

        doctorId = intent.getStringExtra("DOCTOR_ID")
        slotId = intent.getStringExtra("SLOT_ID")

        if (doctorId == null || slotId == null) {
            Toast.makeText(this, "Erro: Informações do agendamento inválidas.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        initializeViews()
        setupToolbar()
        loadPatientInfo()
        loadData()
        
        btnConfirm.setOnClickListener {
            createAppointment()
        }
    }

    private fun initializeViews() {
        toolbar = findViewById(R.id.toolbar)
        ivDoctorPhoto = findViewById(R.id.ivBookingDoctorPhoto)
        tvDoctorName = findViewById(R.id.tvBookingDoctorName)
        tvDoctorSpecialization = findViewById(R.id.tvBookingDoctorSpecialization)
        tvDate = findViewById(R.id.tvBookingDate)
        tvTime = findViewById(R.id.tvBookingTime)
        tvPrice = findViewById(R.id.tvBookingPrice)
        tvTotal = findViewById(R.id.tvBookingTotal)
        btnConfirm = findViewById(R.id.btnConfirmBooking)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun loadData() {
        // Load Doctor Info
        firestore.collection("doutores").document(doctorId!!)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    doctor = Doctor.fromMap(document.data!!, document.id)
                    populateDoctorInfo()
                }
            }

        // Load Slot Info
        firestore.collection("doctorAvailability").document(slotId!!)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    slot = AvailabilitySlot.fromMap(document.data!!, document.id)
                    populateSlotInfo()
                }
            }
    }

    private fun loadPatientInfo() {
        val currentUser = auth.currentUser ?: return
        val patientId = currentUser.uid

        firestore.collection("pacientes").document(patientId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    patientName = extractPatientName(document) ?: currentUser.displayName
                } else {
                    loadPatientInfoFromUsers(patientId)
                }
            }
            .addOnFailureListener {
                loadPatientInfoFromUsers(patientId)
            }
    }

    private fun loadPatientInfoFromUsers(patientId: String) {
        val currentUser = auth.currentUser ?: return
        firestore.collection("users").document(patientId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    patientName = extractPatientName(document) ?: currentUser.displayName
                } else {
                    patientName = currentUser.displayName ?: currentUser.email
                }
            }
            .addOnFailureListener {
                patientName = currentUser.displayName ?: currentUser.email
            }
    }

    private fun extractPatientName(document: com.google.firebase.firestore.DocumentSnapshot): String? {
        return document.getString("name")
            ?: document.getString("fullName")
            ?: document.getString("displayName")
    }

    private fun populateDoctorInfo() {
        doctor?.let {
            tvDoctorName.text = it.name
            tvDoctorSpecialization.text = it.specialization
            if (it.photoUrl.isNotEmpty()) {
                Glide.with(this).load(it.photoUrl).circleCrop().into(ivDoctorPhoto)
            }
            
            // Assuming price is part of doctor's profile
            val price = it.priceEurCents / 100.0
            val serviceFee = 2.50 // Example service fee
            val total = price + serviceFee
            tvPrice.text = String.format(Locale.US, "%.2f €", price)
            tvTotal.text = String.format(Locale.US, "%.2f €", total)
        }
    }

    private fun populateSlotInfo() {
        slot?.let {
            val dateFormat = SimpleDateFormat("EEEE, d 'de' MMMM 'de' yyyy", Locale("pt", "BR"))
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            
            it.startTime?.toDate()?.let { start ->
                tvDate.text = dateFormat.format(start)
                val startTimeStr = timeFormat.format(start)
                val endTimeStr = it.endTime?.toDate()?.let { end -> timeFormat.format(end) } ?: ""
                tvTime.text = "$startTimeStr - $endTimeStr"
            }
        }
    }
    
    private fun createAppointment() {
        val patientId = auth.currentUser?.uid
        if (patientId == null || doctor == null || slot == null) {
            Toast.makeText(this, "Não foi possível confirmar o agendamento.", Toast.LENGTH_SHORT).show()
            return
        }

        val appointment = hashMapOf(
            "patientId" to patientId,
            "doctorId" to doctorId,
            "startTime" to slot?.startTime,
            "endTime" to slot?.endTime,
            "status" to "confirmed",
            "createdAt" to com.google.firebase.Timestamp.now(),
            "doctorName" to doctor?.name,
            "patientName" to (patientName ?: auth.currentUser?.displayName ?: auth.currentUser?.email ?: "Paciente")
        )

        firestore.collection("appointments").add(appointment)
            .addOnSuccessListener {
                // Mark slot as booked
                firestore.collection("doctorAvailability").document(slotId!!).update("isBooked", true)
                
                Toast.makeText(this, "Consulta agendada com sucesso!", Toast.LENGTH_LONG).show()
                // Redirect to a confirmation or appointments screen
                finish() 
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Erro ao agendar: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
