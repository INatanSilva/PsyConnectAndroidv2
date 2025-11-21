package com.example.psyconnectandroid

import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Tela para criar/editar anotações clínicas do paciente
 */
class PatientNoteActivity : AppCompatActivity() {
    
    // Views
    private lateinit var etFeelingsAndSymptoms: EditText
    private lateinit var etPatientNeeds: EditText
    private lateinit var cbReferralNeeded: CheckBox
    private lateinit var etReferralDetails: EditText
    private lateinit var etDiagnosticHypotheses: EditText
    private lateinit var etTherapeuticGoals: EditText
    private lateinit var etInterventions: EditText
    private lateinit var etObservations: EditText
    private lateinit var etCarePlan: EditText
    private lateinit var spinnerRiskLevel: Spinner
    private lateinit var cbShareWithPatient: CheckBox
    private lateinit var btnSave: Button
    private lateinit var tvPatientName: TextView
    
    // Services
    private val notesService = NotesService()
    private val auth = FirebaseAuth.getInstance()
    
    // Data
    private var patientId: String? = null
    private var patientName: String? = null
    private var existingNote: PatientNote? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_patient_note)
        
        patientId = intent.getStringExtra("PATIENT_ID")
        patientName = intent.getStringExtra("PATIENT_NAME")
        
        if (patientId == null) {
            Toast.makeText(this, "Erro: ID do paciente não encontrado", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        initializeViews()
        setupClickListeners()
        loadNote()
    }
    
    private fun initializeViews() {
        tvPatientName = findViewById(R.id.tvPatientName)
        etFeelingsAndSymptoms = findViewById(R.id.etFeelingsAndSymptoms)
        etPatientNeeds = findViewById(R.id.etPatientNeeds)
        cbReferralNeeded = findViewById(R.id.cbReferralNeeded)
        etReferralDetails = findViewById(R.id.etReferralDetails)
        etDiagnosticHypotheses = findViewById(R.id.etDiagnosticHypotheses)
        etTherapeuticGoals = findViewById(R.id.etTherapeuticGoals)
        etInterventions = findViewById(R.id.etInterventions)
        etObservations = findViewById(R.id.etObservations)
        etCarePlan = findViewById(R.id.etCarePlan)
        spinnerRiskLevel = findViewById(R.id.spinnerRiskLevel)
        cbShareWithPatient = findViewById(R.id.cbShareWithPatient)
        btnSave = findViewById(R.id.btnSave)
        
        tvPatientName.text = patientName ?: "Paciente"
        
        // Setup risk level spinner
        val riskLevels = arrayOf("Baixo", "Moderado", "Alto")
        val adapter = android.widget.ArrayAdapter(this, android.R.layout.simple_spinner_item, riskLevels)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerRiskLevel.adapter = adapter
    }
    
    private fun setupClickListeners() {
        btnSave.setOnClickListener {
            saveNote()
        }
        
        cbReferralNeeded.setOnCheckedChangeListener { _, isChecked ->
            etReferralDetails.isEnabled = isChecked
        }
    }
    
    private fun loadNote() {
        val doctorId = auth.currentUser?.uid
        if (doctorId == null || patientId == null) return
        
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val note = notesService.getNote(doctorId, patientId!!)
                if (note != null) {
                    existingNote = note
                    populateFields(note)
                }
            } catch (e: Exception) {
                android.util.Log.e("PatientNote", "❌ Erro ao carregar anotação", e)
            }
        }
    }
    
    private fun populateFields(note: PatientNote) {
        etFeelingsAndSymptoms.setText(note.feelingsAndSymptoms)
        etPatientNeeds.setText(note.patientNeeds)
        cbReferralNeeded.isChecked = note.referralNeeded
        etReferralDetails.setText(note.referralDetails)
        etReferralDetails.isEnabled = note.referralNeeded
        etDiagnosticHypotheses.setText(note.diagnosticHypotheses)
        etTherapeuticGoals.setText(note.therapeuticGoals)
        etInterventions.setText(note.interventions)
        etObservations.setText(note.observations)
        etCarePlan.setText(note.carePlan)
        cbShareWithPatient.isChecked = note.shareWithPatient
        
        // Set risk level spinner
        val riskLevels = arrayOf("Baixo", "Moderado", "Alto")
        val index = riskLevels.indexOf(note.riskLevel)
        if (index >= 0) {
            spinnerRiskLevel.setSelection(index)
        }
    }
    
    private fun saveNote() {
        val doctorId = auth.currentUser?.uid
        if (doctorId == null || patientId == null) {
            Toast.makeText(this, "Erro: Usuário não autenticado", Toast.LENGTH_SHORT).show()
            return
        }
        
        val note = PatientNote(
            id = existingNote?.id ?: "",
            patientId = patientId!!,
            doctorId = doctorId,
            updatedAt = Timestamp.now(),
            shareWithPatient = cbShareWithPatient.isChecked,
            feelingsAndSymptoms = etFeelingsAndSymptoms.text.toString(),
            patientNeeds = etPatientNeeds.text.toString(),
            referralNeeded = cbReferralNeeded.isChecked,
            referralDetails = etReferralDetails.text.toString(),
            diagnosticHypotheses = etDiagnosticHypotheses.text.toString(),
            therapeuticGoals = etTherapeuticGoals.text.toString(),
            interventions = etInterventions.text.toString(),
            observations = etObservations.text.toString(),
            carePlan = etCarePlan.text.toString(),
            riskLevel = spinnerRiskLevel.selectedItem.toString()
        )
        
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val success = notesService.saveNote(note)
                if (success) {
                    Toast.makeText(this@PatientNoteActivity, "Anotação salva com sucesso", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this@PatientNoteActivity, "Erro ao salvar anotação", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                android.util.Log.e("PatientNote", "❌ Erro ao salvar anotação", e)
                Toast.makeText(this@PatientNoteActivity, "Erro ao salvar anotação", Toast.LENGTH_SHORT).show()
            }
        }
    }
}


