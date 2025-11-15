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
            Toast.makeText(this, "Funcionalidade de agendamento principal em breve!", Toast.LENGTH_SHORT).show()
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
            // Buscar sem orderBy para evitar necessidade de índice composto
            firestore.collection("doctorAvailability")
                .whereEqualTo("doctorId", id)
                .whereEqualTo("isBooked", false)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    availabilitySlots.clear()
                    
                    for (document in querySnapshot.documents) {
                        try {
                            val data = document.data
                            if (data != null) {
                                val slot = AvailabilitySlot.fromMap(data, document.id)
                                availabilitySlots.add(slot)
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("DoctorProfileActivity", "Error parsing availability slot ${document.id}", e)
                        }
                    }
                    
                    // Ordenar manualmente por startTime (mais próximos primeiro)
                    availabilitySlots.sortWith(compareBy(nullsLast()) { it.startTime })
                    
                    // Filtrar apenas slots futuros e limitar a 10
                    val now = com.google.firebase.Timestamp.now()
                    val futureSlots = availabilitySlots.filter { 
                        it.startTime != null && it.startTime!!.compareTo(now) > 0 
                    }.take(10)
                    
                    availabilitySlots.clear()
                    availabilitySlots.addAll(futureSlots)
                    
                    availabilityAdapter.notifyDataSetChanged()
                }
                .addOnFailureListener { e ->
                    android.util.Log.e("DoctorProfileActivity", "Error loading availability", e)
                    // Tentar buscar sem filtro de isBooked se a query anterior falhar
                    firestore.collection("doctorAvailability")
                        .whereEqualTo("doctorId", id)
                        .get()
                        .addOnSuccessListener { querySnapshot ->
                            availabilitySlots.clear()
                            val now = com.google.firebase.Timestamp.now()
                            for (document in querySnapshot.documents) {
                                try {
                                    val data = document.data
                                    if (data != null) {
                                        val slot = AvailabilitySlot.fromMap(data, document.id)
                                        // Filtrar manualmente por isBooked e data futura
                                        if (!slot.isBooked && slot.startTime != null && slot.startTime!!.compareTo(now) > 0) {
                                            availabilitySlots.add(slot)
                                        }
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.e("DoctorProfileActivity", "Error parsing slot ${document.id}", e)
                                }
                            }
                            availabilitySlots.sortWith(compareBy(nullsLast()) { it.startTime })
                            val limitedSlots = availabilitySlots.take(10)
                            availabilitySlots.clear()
                            availabilitySlots.addAll(limitedSlots)
                            availabilityAdapter.notifyDataSetChanged()
                        }
                        .addOnFailureListener { e2 ->
                            Toast.makeText(this, "Erro ao carregar disponibilidade: ${e2.message}", Toast.LENGTH_SHORT).show()
                        }
                }
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
