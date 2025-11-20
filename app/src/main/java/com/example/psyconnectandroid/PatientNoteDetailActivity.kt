package com.example.psyconnectandroid

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Locale

class PatientNoteDetailActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageView
    private lateinit var ivDoctorPhoto: ImageView
    private lateinit var tvDoctorName: TextView
    private lateinit var tvUpdatedAt: TextView
    
    // Campos da anotação (read-only)
    private lateinit var tvFeelingsAndSymptoms: TextView
    private lateinit var tvPatientNeeds: TextView
    private lateinit var tvReferralNeeded: TextView
    private lateinit var tvReferralDetails: TextView
    private lateinit var tvDiagnosticHypotheses: TextView
    private lateinit var tvTherapeuticGoals: TextView
    private lateinit var tvInterventions: TextView
    private lateinit var tvObservations: TextView
    private lateinit var tvCarePlan: TextView
    private lateinit var tvRiskLevel: TextView
    
    private lateinit var layoutFeelings: View
    private lateinit var layoutPatientNeeds: View
    private lateinit var layoutReferral: View
    private lateinit var layoutReferralDetails: View
    private lateinit var layoutDiagnostic: View
    private lateinit var layoutTherapeutic: View
    private lateinit var layoutInterventions: View
    private lateinit var layoutObservations: View
    private lateinit var layoutCarePlan: View
    private lateinit var layoutRiskLevel: View

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_patient_note_detail)

        initializeViews()
        setupBackButton()
        loadNote()
    }

    private fun initializeViews() {
        btnBack = findViewById(R.id.btnBack)
        ivDoctorPhoto = findViewById(R.id.ivDoctorPhoto)
        tvDoctorName = findViewById(R.id.tvDoctorName)
        tvUpdatedAt = findViewById(R.id.tvUpdatedAt)
        
        tvFeelingsAndSymptoms = findViewById(R.id.tvFeelingsAndSymptoms)
        tvPatientNeeds = findViewById(R.id.tvPatientNeeds)
        tvReferralNeeded = findViewById(R.id.tvReferralNeeded)
        tvReferralDetails = findViewById(R.id.tvReferralDetails)
        tvDiagnosticHypotheses = findViewById(R.id.tvDiagnosticHypotheses)
        tvTherapeuticGoals = findViewById(R.id.tvTherapeuticGoals)
        tvInterventions = findViewById(R.id.tvInterventions)
        tvObservations = findViewById(R.id.tvObservations)
        tvCarePlan = findViewById(R.id.tvCarePlan)
        tvRiskLevel = findViewById(R.id.tvRiskLevel)
        
        layoutFeelings = findViewById(R.id.layoutFeelings)
        layoutPatientNeeds = findViewById(R.id.layoutPatientNeeds)
        layoutReferral = findViewById(R.id.layoutReferral)
        layoutReferralDetails = findViewById(R.id.layoutReferralDetails)
        layoutDiagnostic = findViewById(R.id.layoutDiagnostic)
        layoutTherapeutic = findViewById(R.id.layoutTherapeutic)
        layoutInterventions = findViewById(R.id.layoutInterventions)
        layoutObservations = findViewById(R.id.layoutObservations)
        layoutCarePlan = findViewById(R.id.layoutCarePlan)
        layoutRiskLevel = findViewById(R.id.layoutRiskLevel)
    }

    private fun setupBackButton() {
        btnBack.setOnClickListener {
            finish()
        }
    }

    private fun loadNote() {
        val noteId = intent.getStringExtra("note_id")
        val doctorId = intent.getStringExtra("doctor_id")
        val patientId = intent.getStringExtra("patient_id")
        
        val currentUser = auth.currentUser
        if (currentUser == null || patientId != currentUser.uid) {
            finish()
            return
        }

        if (doctorId.isNullOrEmpty() || patientId.isNullOrEmpty()) {
            finish()
            return
        }

        // Buscar anotação específica com filtro shareWithPatient == true (igual ao iOS)
        firestore.collection("patientNotes")
            .whereEqualTo("doctorId", doctorId)
            .whereEqualTo("patientId", patientId)
            .whereEqualTo("shareWithPatient", true)
            .limit(1)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    android.util.Log.e("PatientNoteDetailActivity", "Note not found or not shared")
                    finish()
                    return@addOnSuccessListener
                }

                val document = documents.first()
                val data = document.data
                if (data != null) {
                    val note = PatientNote.fromMap(data, document.id)
                    
                    // Buscar dados do doutor
                    firestore.collection("doutores").document(doctorId).get()
                        .addOnSuccessListener { doctorDoc ->
                            if (doctorDoc.exists()) {
                                val doctorName = doctorDoc.getString("name")
                                    ?: doctorDoc.getString("fullName")
                                    ?: "Doutor"
                                val doctorPhotoUrl = doctorDoc.getString("profileImageURL")
                                    ?: doctorDoc.getString("photoUrl")
                                    ?: ""

                                displayNote(note.copy(
                                    doctorName = doctorName,
                                    doctorPhotoUrl = doctorPhotoUrl
                                ))
                            } else {
                                displayNote(note)
                            }
                        }
                        .addOnFailureListener {
                            displayNote(note)
                        }
                }
            }
            .addOnFailureListener { e ->
                android.util.Log.e("PatientNoteDetailActivity", "Error loading note", e)
                finish()
            }
    }

    private fun displayNote(note: PatientNote) {
        // Foto e nome do doutor
        if (note.doctorPhotoUrl.isNotEmpty()) {
            Glide.with(this)
                .load(note.doctorPhotoUrl)
                .placeholder(R.drawable.ic_person)
                .error(R.drawable.ic_person)
                .circleCrop()
                .into(ivDoctorPhoto)
        } else {
            ivDoctorPhoto.setImageResource(R.drawable.ic_person)
        }
        
        tvDoctorName.text = note.doctorName.ifEmpty { "Doutor" }
        
        // Data de atualização
        note.updatedAt?.toDate()?.let { date ->
            val dateFormat = SimpleDateFormat("d MMM yyyy", Locale.ENGLISH)
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val dateStr = dateFormat.format(date)
            val timeStr = timeFormat.format(date)
            tvUpdatedAt.text = "Atualizado em $dateStr at $timeStr"
        } ?: run {
            tvUpdatedAt.text = ""
        }

        // Exibir campos (ocultar se vazios)
        displayField(layoutFeelings, tvFeelingsAndSymptoms, note.feelingsAndSymptoms, "Queixas e Sintomas")
        displayField(layoutPatientNeeds, tvPatientNeeds, note.patientNeeds, "Necessidades")
        
        // Referral
        if (note.referralNeeded) {
            layoutReferral.visibility = View.VISIBLE
            tvReferralNeeded.text = "Sim"
            displayField(layoutReferralDetails, tvReferralDetails, note.referralDetails, "Detalhes do Encaminhamento")
        } else {
            layoutReferral.visibility = View.GONE
            layoutReferralDetails.visibility = View.GONE
        }
        
        displayField(layoutDiagnostic, tvDiagnosticHypotheses, note.diagnosticHypotheses, "Hipóteses Diagnósticas")
        displayField(layoutTherapeutic, tvTherapeuticGoals, note.therapeuticGoals, "Objetivos Terapêuticos")
        displayField(layoutInterventions, tvInterventions, note.interventions, "Intervenções")
        displayField(layoutObservations, tvObservations, note.observations, "Observações")
        displayField(layoutCarePlan, tvCarePlan, note.carePlan, "Plano de Cuidado")
        displayField(layoutRiskLevel, tvRiskLevel, note.riskLevel, "Nível de Risco")
    }

    private fun displayField(layout: View, textView: TextView, content: String, label: String) {
        if (content.isNotEmpty()) {
            layout.visibility = View.VISIBLE
            textView.text = content
        } else {
            layout.visibility = View.GONE
        }
    }
}

