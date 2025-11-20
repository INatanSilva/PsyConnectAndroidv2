package com.example.psyconnectandroid

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class DoctorsActivity : AppCompatActivity() {
    
    private lateinit var searchEditText: EditText
    private lateinit var recyclerViewDoctors: RecyclerView
    private lateinit var skeletonDoctorsGrid: LinearLayout
    private lateinit var chipAll: TextView
    private lateinit var chipPsicologia: TextView
    private lateinit var chipPsiquiatria: TextView
    private lateinit var chipTerapia: TextView
    private lateinit var navHome: LinearLayout
    private lateinit var navAppointments: LinearLayout
    private lateinit var navDoctors: LinearLayout
    private lateinit var navNotes: LinearLayout
    private lateinit var navProfile: LinearLayout
    
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    
    private val allDoctors = mutableListOf<Doctor>()
    private val filteredDoctors = mutableListOf<Doctor>()
    private var currentFilter = "Todas"
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_doctors)
        
        initializeViews()
        setupClickListeners()
        setupRecyclerView()
        loadDoctors()
    }
    
    private fun initializeViews() {
        searchEditText = findViewById(R.id.searchEditText)
        recyclerViewDoctors = findViewById(R.id.recyclerViewDoctors)
        skeletonDoctorsGrid = findViewById(R.id.skeletonDoctorsGrid)
        chipAll = findViewById(R.id.chipAll)
        chipPsicologia = findViewById(R.id.chipPsicologia)
        chipPsiquiatria = findViewById(R.id.chipPsiquiatria)
        chipTerapia = findViewById(R.id.chipTerapia)
        
        // Bottom navigation
        navHome = findViewById(R.id.navHome)
        navAppointments = findViewById(R.id.navAppointments)
        navDoctors = findViewById(R.id.navDoctors)
        navNotes = findViewById(R.id.navNotes)
        navProfile = findViewById(R.id.navProfile)
        
        // Start skeleton animation
        startSkeletonAnimation()
    }
    
    private fun startSkeletonAnimation() {
        val animation = android.view.animation.AnimationUtils.loadAnimation(this, R.anim.skeleton_shimmer)
        skeletonDoctorsGrid.startAnimation(animation)
    }
    
    private fun showSkeleton() {
        skeletonDoctorsGrid.visibility = View.VISIBLE
        recyclerViewDoctors.visibility = View.GONE
    }
    
    private fun hideSkeleton() {
        skeletonDoctorsGrid.visibility = View.GONE
        recyclerViewDoctors.visibility = View.VISIBLE
    }
    
    private fun setupClickListeners() {
        // Filter chips
        chipAll.setOnClickListener { 
            selectFilter("Todas")
            filterDoctors("Todas")
        }
        chipPsicologia.setOnClickListener { 
            selectFilter("Psicologia")
            filterDoctors("Psicologia")
        }
        chipPsiquiatria.setOnClickListener { 
            selectFilter("Psiquiatria")
            filterDoctors("Psiquiatria")
        }
        chipTerapia.setOnClickListener { 
            selectFilter("Terapia")
            filterDoctors("Terapia")
        }
        
        // Search
        searchEditText.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                performSearch()
            }
        }
        
        // Bottom navigation
        navHome.setOnClickListener {
            val intent = Intent(this, PatientActivity::class.java)
            startActivity(intent)
            finish()
        }
        
        navAppointments.setOnClickListener {
            val intent = Intent(this, MyAppointmentsActivity::class.java)
            startActivity(intent)
        }
        
        navDoctors.setOnClickListener {
            // Already on doctors screen
        }
        
        navNotes.setOnClickListener {
            val intent = Intent(this, PatientNotesActivity::class.java)
            startActivity(intent)
        }
        
        navProfile.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }
    }
    
    private fun setupRecyclerView() {
        // Grid layout with 2 columns
        recyclerViewDoctors.layoutManager = GridLayoutManager(this, 2)
    }
    
    private fun loadDoctors() {
        // Tentar carregar do cache primeiro
        val cachedDoctors = DoctorCache.getAll()
        if (cachedDoctors.isNotEmpty()) {
            android.util.Log.d("DoctorsActivity", "📦 Carregando ${cachedDoctors.size} doutores do cache")
            allDoctors.clear()
            allDoctors.addAll(cachedDoctors)
            filterDoctors(currentFilter)
        }
        
        // Buscar do Firestore em background para atualizar cache
        firestore.collection("doutores")
            .get()
            .addOnSuccessListener { documents ->
                allDoctors.clear()
                for (document in documents) {
                    try {
                        val doctor = Doctor.fromMap(document.data, document.id)
                        allDoctors.add(doctor)
                        // Salvar no cache
                        DoctorCache.put(doctor.id, doctor)
                    } catch (e: Exception) {
                        android.util.Log.e("DoctorsActivity", "Error parsing doctor ${document.id}", e)
                    }
                }
                
                android.util.Log.d("DoctorsActivity", "🔄 ${allDoctors.size} doutores carregados do Firestore e salvos no cache")
                filterDoctors(currentFilter)
            }
            .addOnFailureListener { e ->
                android.util.Log.e("DoctorsActivity", "Error loading doctors", e)
                // Se falhar e não tiver cache, mostrar erro
                if (allDoctors.isEmpty()) {
                    Toast.makeText(this, "Erro ao carregar doutores", Toast.LENGTH_SHORT).show()
                }
            }
    }
    
    private fun filterDoctors(filter: String) {
        currentFilter = filter
        filteredDoctors.clear()
        
        when (filter) {
            "Todas" -> filteredDoctors.addAll(allDoctors)
            "Psicologia" -> {
                filteredDoctors.addAll(allDoctors.filter { 
                    it.specialization.contains("Psicologia", ignoreCase = true) ||
                    it.specialization.contains("Psicólogo", ignoreCase = true)
                })
            }
            "Psiquiatria" -> {
                filteredDoctors.addAll(allDoctors.filter { 
                    it.specialization.contains("Psiquiatria", ignoreCase = true) ||
                    it.specialization.contains("Psiquiatra", ignoreCase = true)
                })
            }
            "Terapia" -> {
                filteredDoctors.addAll(allDoctors.filter { 
                    it.specialization.contains("Terapia", ignoreCase = true) ||
                    it.specialization.contains("Terapeuta", ignoreCase = true)
                })
            }
        }
        
        updateDoctorsUI()
    }
    
    private fun performSearch() {
        val query = searchEditText.text.toString().trim().lowercase()
        
        if (query.isEmpty()) {
            filterDoctors(currentFilter)
            return
        }
        
        filteredDoctors.clear()
        filteredDoctors.addAll(allDoctors.filter { 
            it.name.lowercase().contains(query) ||
            it.specialization.lowercase().contains(query) ||
            it.email.lowercase().contains(query)
        })
        
        updateDoctorsUI()
    }
    
    private fun updateDoctorsUI() {
        recyclerViewDoctors.adapter = DoctorsGridAdapter(filteredDoctors) { doctor ->
            connectDoctor(doctor)
        }
        hideSkeleton()
    }
    
    private fun selectFilter(filter: String) {
        // Reset all chips
        resetFilterChips()
        
        // Highlight selected chip
        when (filter) {
            "Todas" -> chipAll.background = getDrawable(R.drawable.bg_filter_selected)
            "Psicologia" -> chipPsicologia.background = getDrawable(R.drawable.bg_filter_selected)
            "Psiquiatria" -> chipPsiquiatria.background = getDrawable(R.drawable.bg_filter_selected)
            "Terapia" -> chipTerapia.background = getDrawable(R.drawable.bg_filter_selected)
        }
    }
    
    private fun resetFilterChips() {
        chipAll.background = getDrawable(R.drawable.bg_filter_unselected)
        chipPsicologia.background = getDrawable(R.drawable.bg_filter_unselected)
        chipPsiquiatria.background = getDrawable(R.drawable.bg_filter_unselected)
        chipTerapia.background = getDrawable(R.drawable.bg_filter_unselected)
    }
    
    private fun connectDoctor(doctor: Doctor) {
        Toast.makeText(this, "Conectar com ${doctor.name}", Toast.LENGTH_SHORT).show()
        // TODO: Navigate to scheduling screen
    }
    
}







