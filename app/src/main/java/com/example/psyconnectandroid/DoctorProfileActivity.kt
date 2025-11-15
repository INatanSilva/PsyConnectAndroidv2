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
            firestore.collection("doctorAvailability")
                .whereEqualTo("doctorId", id)
                .whereEqualTo("isBooked", false)
                .orderBy("startTime", Query.Direction.ASCENDING)
                .limit(10)
                .get()
                .addOnSuccessListener { documents ->
                    availabilitySlots.clear()
                    for (document in documents) {
                        // Pass document.id to fromMap
                        availabilitySlots.add(AvailabilitySlot.fromMap(document.data, document.id))
                    }
                    availabilityAdapter.notifyDataSetChanged()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Erro ao carregar disponibilidade: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
    
    private fun loadReviews() {
        doctorId?.let { id ->
            firestore.collection("avaliacoes")
                .whereEqualTo("doctorId", id)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(10)
                .get()
                .addOnSuccessListener { documents ->
                    reviews.clear()
                    var totalRating = 0.0
                    for (document in documents) {
                        val review = Review.fromMap(document.data)
                        reviews.add(review)
                        totalRating += review.rating
                    }
                    
                    if (reviews.isNotEmpty()) {
                        val avgRating = totalRating / reviews.size
                        tvDoctorProfileRating.text = String.format(Locale.US, "%.1f (%d avaliações)", avgRating, reviews.size)
                    } else {
                        tvDoctorProfileRating.text = "Nenhuma avaliação"
                    }
                    
                    reviewsAdapter.notifyDataSetChanged()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Erro ao carregar avaliações: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
