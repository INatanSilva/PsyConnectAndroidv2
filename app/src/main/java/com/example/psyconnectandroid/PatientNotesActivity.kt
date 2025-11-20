package com.example.psyconnectandroid

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Locale

class PatientNotesActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageView
    private lateinit var rvNotes: RecyclerView
    private lateinit var tvEmptyState: View
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var notesAdapter: PatientNoteAdapter

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val notes = mutableListOf<PatientNote>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_patient_notes)

        initializeViews()
        setupRecyclerView()
        setupBackButton()
        loadNotes()
    }

    override fun onResume() {
        super.onResume()
        loadNotes()
    }

    private fun initializeViews() {
        btnBack = findViewById(R.id.btnBack)
        rvNotes = findViewById(R.id.rvNotes)
        tvEmptyState = findViewById(R.id.tvEmptyState)
        swipeRefresh = findViewById(R.id.swipeRefresh)
    }

    private fun setupBackButton() {
        btnBack.setOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        notesAdapter = PatientNoteAdapter(notes) { note ->
            // Abrir detalhes da anotação
            openNoteDetails(note)
        }
        rvNotes.layoutManager = LinearLayoutManager(this)
        rvNotes.adapter = notesAdapter
        
        // Pull to refresh (igual ao iOS)
        swipeRefresh.setOnRefreshListener {
            loadNotes()
        }
    }

    private fun loadNotes() {
        swipeRefresh.isRefreshing = true
        
        val currentUser = auth.currentUser
        if (currentUser == null) {
            showEmptyState()
            swipeRefresh.isRefreshing = false
            return
        }

        val patientId = currentUser.uid

        android.util.Log.d("PatientNotesActivity", "Loading notes for patient: $patientId")

        // Filtro duplo: patientId + shareWithPatient == true (igual ao iOS)
        firestore.collection("patientNotes")
            .whereEqualTo("patientId", patientId)
            .whereEqualTo("shareWithPatient", true)
            .get()
            .addOnSuccessListener { documents ->
                android.util.Log.d("PatientNotesActivity", "Found ${documents.size()} notes")

                notes.clear()

                if (documents.isEmpty) {
                    showEmptyState()
                    return@addOnSuccessListener
                }

                // Processar cada anotação e buscar dados do doutor
                var processedCount = 0
                val totalCount = documents.size()

                for (document in documents) {
                    try {
                        val data = document.data
                        if (data != null) {
                            val note = PatientNote.fromMap(data, document.id)
                            val doctorId = note.doctorId

                            if (doctorId.isNotEmpty()) {
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

                                            val updatedNote = note.copy(
                                                doctorName = doctorName,
                                                doctorPhotoUrl = doctorPhotoUrl
                                            )
                                            notes.add(updatedNote)

                                            processedCount++
                                            if (processedCount == totalCount) {
                                                // Ordenar por updatedAt (mais recentes primeiro) - igual ao iOS
                                                notes.sortByDescending { it.updatedAt?.toDate()?.time ?: 0L }
                                                updateUI()
                                            }
                                        } else {
                                            processedCount++
                                            notes.add(note)
                                            if (processedCount == totalCount) {
                                                notes.sortByDescending { it.updatedAt?.toDate()?.time ?: 0L }
                                                updateUI()
                                            }
                                        }
                                    }
                                    .addOnFailureListener {
                                        processedCount++
                                        notes.add(note)
                                        if (processedCount == totalCount) {
                                            notes.sortByDescending { it.updatedAt?.toDate()?.time ?: 0L }
                                            updateUI()
                                        }
                                    }
                            } else {
                                processedCount++
                                notes.add(note)
                                if (processedCount == totalCount) {
                                    notes.sortByDescending { it.updatedAt?.toDate()?.time ?: 0L }
                                    updateUI()
                                }
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("PatientNotesActivity", "Error parsing note ${document.id}", e)
                        processedCount++
                        if (processedCount == totalCount) {
                            notes.sortByDescending { it.updatedAt?.toDate()?.time ?: 0L }
                            updateUI()
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                android.util.Log.e("PatientNotesActivity", "Error loading notes", e)
                showEmptyState()
                swipeRefresh.isRefreshing = false
            }
    }

    private fun updateUI() {
        swipeRefresh.isRefreshing = false
        
        if (notes.isEmpty()) {
            showEmptyState()
        } else {
            rvNotes.visibility = View.VISIBLE
            tvEmptyState.visibility = View.GONE
            notesAdapter.notifyDataSetChanged()
        }
    }

    private fun showEmptyState() {
        rvNotes.visibility = View.GONE
        tvEmptyState.visibility = View.VISIBLE
    }

    private fun openNoteDetails(note: PatientNote) {
        val intent = Intent(this, PatientNoteDetailActivity::class.java)
        intent.putExtra("note_id", note.id)
        intent.putExtra("doctor_id", note.doctorId)
        intent.putExtra("patient_id", note.patientId)
        startActivity(intent)
    }
}

