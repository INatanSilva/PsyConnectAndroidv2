package com.example.psyconnectandroid

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AllDoctorsActivity : AppCompatActivity() {
    
    private lateinit var recyclerViewDoctors: RecyclerView
    private lateinit var recyclerViewFilters: RecyclerView
    private lateinit var etSearch: EditText
    
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    
    private val allDoctors = mutableListOf<Doctor>()
    private val filteredDoctors = mutableListOf<Doctor>()
    
    private val filterOptions = listOf("Todas", "Psicologia", "Psiquiatria", "Terapia", "Nutrição")
    private var selectedFilter = "Todas"
    
    private lateinit var navHome: LinearLayout
    private lateinit var navAppointments: LinearLayout
    private lateinit var navDoctors: LinearLayout
    private lateinit var navNotes: LinearLayout
    private lateinit var navProfile: LinearLayout
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_all_doctors)
        
        initializeViews()
        setupClickListeners()
        setupRecyclerViews()
        loadDoctors()
        setupSearch()
    }
    
    private fun initializeViews() {
        recyclerViewDoctors = findViewById(R.id.recyclerViewDoctors)
        recyclerViewFilters = findViewById(R.id.recyclerViewFilters)
        etSearch = findViewById(R.id.etSearch)
        
        navHome = findViewById(R.id.navHome)
        navAppointments = findViewById(R.id.navAppointments)
        navDoctors = findViewById(R.id.navDoctors)
        navNotes = findViewById(R.id.navNotes)
        navProfile = findViewById(R.id.navProfile)
    }
    
    private fun setupClickListeners() {
        navHome.setOnClickListener {
            val intent = Intent(this, PatientActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }
        
        navAppointments.setOnClickListener {
            Toast.makeText(this, "Em breve: Tela de consultas", Toast.LENGTH_SHORT).show()
        }
        
        navNotes.setOnClickListener {
            Toast.makeText(this, "Em breve: Tela de anotações", Toast.LENGTH_SHORT).show()
        }
        
        navProfile.setOnClickListener {
            logout()
        }
    }
    
    private fun setupRecyclerViews() {
        // Doctors Grid (2 columns)
        recyclerViewDoctors.layoutManager = GridLayoutManager(this, 2)
        
        // Filters (horizontal)
        recyclerViewFilters.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        val filterAdapter = FilterAdapter(filterOptions) { filter ->
            selectedFilter = filter
            filterDoctors()
        }
        recyclerViewFilters.adapter = filterAdapter
    }
    
    private fun setupSearch() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                filterDoctors()
            }
        })
    }
    
    private fun loadDoctors() {
        firestore.collection("doutores")
            .get()
            .addOnSuccessListener { documents ->
                allDoctors.clear()
                for (document in documents) {
                    try {
                        val doctor = Doctor.fromMap(document.data, document.id)
                        allDoctors.add(doctor)
                    } catch (e: Exception) {
                        android.util.Log.e("AllDoctorsActivity", "Error parsing doctor ${document.id}", e)
                    }
                }
                
                filterDoctors()
            }
            .addOnFailureListener { e ->
                android.util.Log.e("AllDoctorsActivity", "Error loading doctors", e)
                Toast.makeText(this, "Erro ao carregar doutores", Toast.LENGTH_SHORT).show()
            }
    }
    
    private fun filterDoctors() {
        filteredDoctors.clear()
        
        val searchQuery = etSearch.text.toString().lowercase().trim()
        
        for (doctor in allDoctors) {
            // Filter by search query
            val matchesSearch = searchQuery.isEmpty() || 
                    doctor.name.lowercase().contains(searchQuery) ||
                    doctor.specialization.lowercase().contains(searchQuery)
            
            // Filter by category
            val matchesFilter = selectedFilter == "Todas" || 
                    doctor.specialization.contains(selectedFilter, ignoreCase = true)
            
            if (matchesSearch && matchesFilter) {
                filteredDoctors.add(doctor)
            }
        }
        
        recyclerViewDoctors.adapter = DoctorsGridAdapter(filteredDoctors) { doctor ->
            connectToDoctor(doctor)
        }
    }
    
    private fun connectToDoctor(doctor: Doctor) {
        val intent = Intent(this, DoctorProfileActivity::class.java)
        intent.putExtra("DOCTOR_ID", doctor.id)
        startActivity(intent)
    }
    
    private fun logout() {
        auth.signOut()
        Toast.makeText(this, "Logout realizado", Toast.LENGTH_SHORT).show()
        
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
