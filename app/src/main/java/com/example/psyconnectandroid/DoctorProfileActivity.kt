package com.example.psyconnectandroid

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.util.Locale
import kotlin.comparisons.nullsLast

class DoctorProfileActivity : AppCompatActivity() {

    private lateinit var ivDoctorProfilePhoto: ImageView
    private lateinit var btnProfileBack: ImageButton
    private lateinit var tvDoctorProfileName: TextView
    private lateinit var tvDoctorProfileSpecialization: TextView
    private lateinit var tvDoctorProfileRating: TextView
    private lateinit var tvDoctorProfileAbout: TextView
    private lateinit var rvAvailability: RecyclerView
    private lateinit var rvReviews: RecyclerView
    private lateinit var btnBookAppointment: Button

    private val firestore = FirebaseFirestore.getInstance()
    private var doctorId: String? = null

    private lateinit var availabilityAdapter: AvailabilityAdapter
    private lateinit var reviewsAdapter: ReviewsAdapter

    private val availabilitySlots = mutableListOf<AvailabilitySlot>()
    private val reviews = mutableListOf<Review>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_doctor_profile)

        doctorId = intent.getStringExtra("DOCTOR_ID")

        if (doctorId == null) {
            Toast.makeText(this, "Erro: ID do doutor não encontrado.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        initializeViews()
        setupRecyclerViews()
        setupListeners()
        loadDoctorProfile()
        loadAvailability()
        loadReviews()
    }

    private fun initializeViews() {
        ivDoctorProfilePhoto = findViewById(R.id.ivDoctorProfilePhoto)
        btnProfileBack = findViewById(R.id.btnProfileBack)
        tvDoctorProfileName = findViewById(R.id.tvDoctorProfileName)
        tvDoctorProfileSpecialization = findViewById(R.id.tvDoctorProfileSpecialization)
        tvDoctorProfileRating = findViewById(R.id.tvDoctorProfileRating)
        tvDoctorProfileAbout = findViewById(R.id.tvDoctorProfileAbout)
        rvAvailability = findViewById(R.id.rvAvailability)
        rvReviews = findViewById(R.id.rvReviews)
        btnBookAppointment = findViewById(R.id.btnBookAppointment)
    }
    
    private fun setupRecyclerViews() {
        // Availability
        availabilityAdapter = AvailabilityAdapter(availabilitySlots) { slot ->
            val intent = Intent(this, BookingConfirmationActivity::class.java)
            intent.putExtra("DOCTOR_ID", doctorId)
            intent.putExtra("SLOT_ID", slot.id)
            startActivity(intent)
        }
        rvAvailability.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rvAvailability.adapter = availabilityAdapter

        // Reviews
        reviewsAdapter = ReviewsAdapter(reviews)
        rvReviews.layoutManager = LinearLayoutManager(this)
        rvReviews.adapter = reviewsAdapter
    }

    private fun setupListeners() {
        btnProfileBack.setOnClickListener {
            finish()
        }
        btnBookAppointment.setOnClickListener {
            // Show available slots or message if none available
            if (availabilitySlots.isEmpty()) {
                Toast.makeText(this, "Nenhum horário disponível no momento.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Selecione um horário disponível acima.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadDoctorProfile() {
        doctorId?.let { id ->
            firestore.collection("doutores").document(id)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val doctor = Doctor.fromMap(document.data!!, document.id)
                        populateDoctorInfo(doctor)
                    } else {
                        Toast.makeText(this, "Doutor não encontrado.", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Erro ao carregar perfil: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun populateDoctorInfo(doctor: Doctor) {
        tvDoctorProfileName.text = doctor.name
        tvDoctorProfileSpecialization.text = doctor.specialization
        tvDoctorProfileAbout.text = "Descrição do doutor virá do Firestore." // Placeholder

        if (doctor.photoUrl.isNotEmpty()) {
            Glide.with(this)
                .load(doctor.photoUrl)
                .into(ivDoctorProfilePhoto)
        }
    }
    
    private fun loadAvailability() {
        doctorId?.let { id ->
            android.util.Log.d("DoctorProfileActivity", "🔍 Loading availability for doctor: $id")
            
            // Buscar todas as disponibilidades do doutor e filtrar localmente
            firestore.collection("doctorAvailability")
                .whereEqualTo("doctorId", id)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    val now = com.google.firebase.Timestamp.now()
                    android.util.Log.d("DoctorProfileActivity", "✅ Found ${querySnapshot.size()} total availability slots")
                    android.util.Log.d("DoctorProfileActivity", "⏰ Current time: ${now.toDate()}")
                    android.util.Log.d("DoctorProfileActivity", "⏰ Current timestamp seconds: ${now.seconds}")
                    
                    availabilitySlots.clear()
                    
                    for (document in querySnapshot.documents) {
                        try {
                            val data = document.data
                            if (data != null) {
                                android.util.Log.d("DoctorProfileActivity", "   📄 Processing slot ${document.id}")
                                android.util.Log.d("DoctorProfileActivity", "      Fields: ${data.keys}")
                                
                                val slot = AvailabilitySlot.fromMap(data, document.id)
                                
                                android.util.Log.d("DoctorProfileActivity", "      Slot: isBooked=${slot.isBooked}, isAvailable=${slot.isAvailable}")
                                android.util.Log.d("DoctorProfileActivity", "      Date (consulta): ${slot.date?.toDate()}")
                                android.util.Log.d("DoctorProfileActivity", "      StartTime (horário): ${slot.startTime?.toDate()}")
                                
                                // Usar o campo DATE para verificar se a consulta está no futuro
                                // DATE = data da consulta, StartTime = horário específico
                                val dateToCheck = slot.date ?: slot.startTime
                                
                                // Filtrar: não reservado, disponível e no futuro
                                if (!slot.isBooked && slot.isAvailable && dateToCheck != null) {
                                    val comparison = dateToCheck.compareTo(now)
                                    val diffSeconds = dateToCheck.seconds - now.seconds
                                    val diffHours = diffSeconds / 3600.0
                                    
                                    android.util.Log.d("DoctorProfileActivity", "      ⏰ Date comparison: $comparison (>0 = future)")
                                    android.util.Log.d("DoctorProfileActivity", "      ⏰ Difference: $diffSeconds seconds (${String.format("%.2f", diffHours)} hours)")
                                    
                                    if (comparison > 0) {
                                        availabilitySlots.add(slot)
                                        android.util.Log.d("DoctorProfileActivity", "      ✅ Added to list (${String.format("%.2f", diffHours)} hours in future)")
                                    } else {
                                        android.util.Log.d("DoctorProfileActivity", "      ⏭️ Skipped - past date (${String.format("%.2f", Math.abs(diffHours))} hours ago)")
                                    }
                                } else {
                                    val reason = when {
                                        slot.isBooked -> "already booked"
                                        !slot.isAvailable -> "not available (isAvailable=false)"
                                        dateToCheck == null -> "no date or startTime"
                                        else -> "unknown"
                                    }
                                    android.util.Log.d("DoctorProfileActivity", "      ⏭️ Skipped - $reason")
                                }
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("DoctorProfileActivity", "❌ Error parsing availability slot ${document.id}", e)
                            e.printStackTrace()
                        }
                    }
                    
                    // Ordenar manualmente por date/startTime (mais próximos primeiro)
                    availabilitySlots.sortWith(compareBy(nullsLast()) { it.date ?: it.startTime })
                    
                    // Limitar a 10 mais próximos
                    val limitedSlots = availabilitySlots.take(10)
                    availabilitySlots.clear()
                    availabilitySlots.addAll(limitedSlots)
                    
                    android.util.Log.d("DoctorProfileActivity", "✅ Final availability count: ${availabilitySlots.size}")
                    
                    // Atualizar UI
                    availabilityAdapter.notifyDataSetChanged()
                    
                    if (availabilitySlots.isEmpty()) {
                        android.util.Log.w("DoctorProfileActivity", "⚠️ No available slots found for this doctor")
                        android.util.Log.w("DoctorProfileActivity", "   Possible reasons:")
                        android.util.Log.w("DoctorProfileActivity", "   - All slots are in the past")
                        android.util.Log.w("DoctorProfileActivity", "   - All slots are booked")
                        android.util.Log.w("DoctorProfileActivity", "   - isAvailable field is false")
                    }
                }
                .addOnFailureListener { e ->
                    android.util.Log.e("DoctorProfileActivity", "❌ Error loading availability from Firestore", e)
                    e.printStackTrace()
                    Toast.makeText(this, "Erro ao carregar disponibilidade: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } ?: run {
            android.util.Log.e("DoctorProfileActivity", "❌ Doctor ID is null, cannot load availability")
        }
    }
    
    private fun loadReviews() {
        doctorId?.let { id ->
            // Buscar sem orderBy para evitar necessidade de índice composto
            firestore.collection("avaliacoes")
                .whereEqualTo("doctorId", id)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    reviews.clear()
                    var totalRating = 0.0
                    
                    for (document in querySnapshot.documents) {
                        try {
                            val data = document.data
                            if (data != null) {
                                val review = Review.fromMap(data)
                                reviews.add(review)
                                totalRating += review.rating
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("DoctorProfileActivity", "Error parsing review ${document.id}", e)
                        }
                    }
                    
                    // Ordenar manualmente por createdAt (mais recentes primeiro)
                    reviews.sortWith(compareBy(nullsLast()) { it.createdAt })
                    reviews.reverse() // Reverter para ter mais recentes primeiro (DESCENDING)
                    
                    // Limitar a 10 mais recentes/melhores
                    val limitedReviews = reviews.take(10)
                    reviews.clear()
                    reviews.addAll(limitedReviews)
                    
                    // Calcular rating médio
                    val allReviews = querySnapshot.documents.mapNotNull { doc ->
                        try {
                            Review.fromMap(doc.data ?: return@mapNotNull null)
                        } catch (e: Exception) {
                            null
                        }
                    }
                    
                    if (allReviews.isNotEmpty()) {
                        val avgRating = allReviews.map { it.rating }.average()
                        tvDoctorProfileRating.text = String.format(Locale.US, "%.1f (%d avaliações)", avgRating, allReviews.size)
                    } else {
                        tvDoctorProfileRating.text = "Nenhuma avaliação"
                    }
                    
                    reviewsAdapter.notifyDataSetChanged()
                }
                .addOnFailureListener { e ->
                    android.util.Log.e("DoctorProfileActivity", "Error loading reviews", e)
                    Toast.makeText(this, "Erro ao carregar avaliações: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
