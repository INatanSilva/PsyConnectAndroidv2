package com.example.psyconnectandroid

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Lista todas as anotações do doutor
 */
class DoctorNotesListActivity : AppCompatActivity() {
    
    // Views
    private lateinit var rvNotes: RecyclerView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    
    // Services
    private val notesService = NotesService()
    private val auth = FirebaseAuth.getInstance()
    
    // Adapter
    private lateinit var adapter: PatientNoteAdapter
    
    // Data
    private val notes = mutableListOf<PatientNote>()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_doctor_notes_list)
        
        initializeViews()
        setupRecyclerView()
        setupSwipeRefresh()
        loadNotes()
    }
    
    private fun initializeViews() {
        rvNotes = findViewById(R.id.rvNotes)
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)
    }
    
    private fun setupRecyclerView() {
        adapter = PatientNoteAdapter(notes) { note ->
            // Navegar para edição
            val intent = Intent(this, PatientNoteActivity::class.java)
            intent.putExtra("PATIENT_ID", note.patientId)
            startActivity(intent)
        }
        rvNotes.layoutManager = LinearLayoutManager(this)
        rvNotes.adapter = adapter
    }
    
    private fun setupSwipeRefresh() {
        swipeRefreshLayout.setOnRefreshListener {
            loadNotes()
        }
    }
    
    private fun loadNotes() {
        val doctorId = auth.currentUser?.uid
        if (doctorId == null) {
            android.util.Log.e("DoctorNotesList", "❌ Usuário não autenticado")
            return
        }
        
        swipeRefreshLayout.isRefreshing = true
        
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val loadedNotes = notesService.getAllNotes(doctorId)
                notes.clear()
                notes.addAll(loadedNotes)
                adapter.notifyDataSetChanged()
            } catch (e: Exception) {
                android.util.Log.e("DoctorNotesList", "❌ Erro ao carregar anotações", e)
            } finally {
                swipeRefreshLayout.isRefreshing = false
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        loadNotes()
    }
}


