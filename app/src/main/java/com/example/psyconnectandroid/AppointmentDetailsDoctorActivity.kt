package com.example.psyconnectandroid

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Locale

class AppointmentDetailsDoctorActivity : AppCompatActivity() {

    private lateinit var toolbar: Toolbar
    private lateinit var ivPatientPhoto: ImageView
    private lateinit var tvPatientName: TextView
    private lateinit var tvAppointmentDate: TextView
    private lateinit var tvAppointmentTime: TextView
    private lateinit var tvAppointmentStatus: TextView
    private lateinit var etPatientNotes: EditText
    private lateinit var btnSaveNotes: Button

    private val firestore = FirebaseFirestore.getInstance()
    private var appointmentId: String? = null
    private var patientId: String? = null
    private var notesDocumentId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_appointment_details_doctor)

        appointmentId = intent.getStringExtra("APPOINTMENT_ID")
        if (appointmentId == null) {
            finish()
            return
        }

        initializeViews()
        setupToolbar()
        loadAppointmentData()

        btnSaveNotes.setOnClickListener { saveNotes() }
    }

    private fun initializeViews() {
        toolbar = findViewById(R.id.toolbarAppointmentDetails)
        ivPatientPhoto = findViewById(R.id.ivPatientPhoto)
        tvPatientName = findViewById(R.id.tvPatientName)
        tvAppointmentDate = findViewById(R.id.tvAppointmentDate)
        tvAppointmentTime = findViewById(R.id.tvAppointmentTime)
        tvAppointmentStatus = findViewById(R.id.tvAppointmentStatusDetails)
        etPatientNotes = findViewById(R.id.etPatientNotes)
        btnSaveNotes = findViewById(R.id.btnSaveNotes)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }
    }

    private fun loadAppointmentData() {
        firestore.collection("appointments").document(appointmentId!!).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val appointment = Appointment.fromMap(doc.data!!, doc.id)
                    patientId = appointment.patientId
                    
                    val dateFormat = SimpleDateFormat("EEEE, d 'de' MMMM 'de' yyyy", Locale("pt", "BR"))
                    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

                    tvAppointmentDate.text = "Data: ${appointment.startTime?.toDate()?.let { dateFormat.format(it) }}"
                    val startTime = appointment.startTime?.toDate()?.let { timeFormat.format(it) } ?: ""
                    val endTime = appointment.endTime?.toDate()?.let { timeFormat.format(it) } ?: ""
                    tvAppointmentTime.text = "Horário: $startTime - $endTime"
                    tvAppointmentStatus.text = "Status: ${appointment.status.capitalize()}"

                    loadPatientData()
                    loadNotes()
                }
            }
    }

    private fun loadPatientData() {
        val patientDocumentId = patientId ?: return

        firestore.collection("pacientes").document(patientDocumentId).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    populatePatientInfo(doc)
                } else {
                    loadPatientDataFromUsers(patientDocumentId)
                }
            }
            .addOnFailureListener {
                loadPatientDataFromUsers(patientDocumentId)
            }
    }

    private fun loadPatientDataFromUsers(patientDocumentId: String) {
        firestore.collection("users").document(patientDocumentId).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    populatePatientInfo(doc)
                }
            }
    }

    private fun populatePatientInfo(doc: com.google.firebase.firestore.DocumentSnapshot) {
        val patientName = doc.getString("name")
            ?: doc.getString("fullName")
            ?: doc.getString("displayName")
            ?: "Paciente"
        tvPatientName.text = patientName

        val photoUrl = doc.getString("photoUrl")
            ?: doc.getString("profileImageUrl")
            ?: doc.getString("profileImageURL")
            ?: doc.getString("imageUrl")
            ?: doc.getString("avatarUrl")

        if (!photoUrl.isNullOrEmpty()) {
            Glide.with(this).load(photoUrl).circleCrop().into(ivPatientPhoto)
        } else {
            ivPatientPhoto.setImageResource(R.drawable.ic_person)
        }
    }

    private fun loadNotes() {
        firestore.collection("patientNotes")
            .whereEqualTo("patientId", patientId)
            .whereEqualTo("doctorId", com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid)
            .limit(1)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val noteDoc = documents.first()
                    notesDocumentId = noteDoc.id
                    etPatientNotes.setText(noteDoc.getString("content"))
                }
            }
    }

    private fun saveNotes() {
        val notesContent = etPatientNotes.text.toString()
        val data = hashMapOf(
            "patientId" to patientId,
            "doctorId" to com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid,
            "content" to notesContent,
            "lastUpdatedAt" to com.google.firebase.Timestamp.now()
        )

        val task = if (notesDocumentId != null) {
            firestore.collection("patientNotes").document(notesDocumentId!!).set(data)
        } else {
            firestore.collection("patientNotes").add(data)
        }

        task.addOnSuccessListener {
            Toast.makeText(this, "Anotações salvas!", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener {
            Toast.makeText(this, "Erro ao salvar anotações.", Toast.LENGTH_SHORT).show()
        }
    }
}
