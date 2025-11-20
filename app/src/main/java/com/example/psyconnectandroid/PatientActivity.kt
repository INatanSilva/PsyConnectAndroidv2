package com.example.psyconnectandroid

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.comparisons.nullsLast

class PatientActivity : AppCompatActivity() {
    
    private lateinit var recyclerViewDoctors: RecyclerView
    private lateinit var recyclerViewAppointments: RecyclerView
    private lateinit var cardFeaturedDoctor: View
    private lateinit var tvFeaturedDoctorName: TextView
    private lateinit var tvFeaturedDoctorSpecialization: TextView
    private lateinit var tvFeaturedDoctorPrice: TextView
    private lateinit var tvFeaturedDoctorRating: TextView
    private lateinit var tvFeaturedDoctorReviews: TextView
    private lateinit var layoutFeaturedRating: View
    private lateinit var ivFeaturedDoctorPhoto: ImageView
    private lateinit var viewOnlineStatus: View
    private lateinit var btnFeaturedSchedule: Button
    private lateinit var btnFindHelp: Button
    private lateinit var tvAppointmentCount: TextView
    private lateinit var viewAppointmentBadge: LinearLayout
    private lateinit var ivViewAllDoctors: ImageView
    private lateinit var navHome: LinearLayout
    private lateinit var navAppointments: LinearLayout
    private lateinit var navDoctors: LinearLayout
    private lateinit var navNotes: LinearLayout
    private lateinit var navProfile: LinearLayout
    
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    
    private val doctors = mutableListOf<Doctor>()
    private val promotedDoctors = mutableListOf<Doctor>()
    private val appointments = mutableListOf<Appointment>()
    private var patientId: String? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_patient)
        
        initializeViews()
        setupClickListeners()
        setupRecyclerViews()
        loadUserData()
    }
    
    override fun onResume() {
        super.onResume()
        // Reload data every time the screen is shown to reflect changes
        loadPatientDataAndAppointments()
        loadDoctors()
        
        // Escutar chamadas recebidas
        setupIncomingCallListener()
    }
    
    override fun onPause() {
        super.onPause()
        // Parar de escutar chamadas recebidas
        CallService.stopListeningToIncomingCalls()
    }
    
    private fun setupIncomingCallListener() {
        CallService.onIncomingCall = { call ->
            // Abrir IncomingCallActivity
            val intent = Intent(this, IncomingCallActivity::class.java).apply {
                putExtra("CALL_ID", call.callId)
                putExtra("CALLER_ID", call.callerId)
                putExtra("PATIENT_NAME", call.patientName)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            startActivity(intent)
        }
        
        CallService.listenToIncomingCalls()
    }
    
    private fun initializeViews() {
        recyclerViewDoctors = findViewById(R.id.recyclerViewDoctors)
        recyclerViewAppointments = findViewById(R.id.recyclerViewAppointments)
        cardFeaturedDoctor = findViewById(R.id.cardFeaturedDoctor)
        tvFeaturedDoctorName = findViewById(R.id.tvFeaturedDoctorName)
        tvFeaturedDoctorSpecialization = findViewById(R.id.tvFeaturedDoctorSpecialization)
        tvFeaturedDoctorPrice = findViewById(R.id.tvFeaturedDoctorPrice)
        tvFeaturedDoctorRating = findViewById(R.id.tvFeaturedDoctorRating)
        tvFeaturedDoctorReviews = findViewById(R.id.tvFeaturedDoctorReviews)
        layoutFeaturedRating = findViewById(R.id.layoutFeaturedRating)
        ivFeaturedDoctorPhoto = findViewById(R.id.ivFeaturedDoctorPhoto)
        viewOnlineStatus = findViewById(R.id.viewOnlineStatus)
        btnFeaturedSchedule = findViewById(R.id.btnFeaturedSchedule)
        btnFindHelp = findViewById(R.id.btnFindHelp)
        tvAppointmentCount = findViewById(R.id.tvAppointmentCount)
        viewAppointmentBadge = findViewById(R.id.viewAppointmentBadge)
        ivViewAllDoctors = findViewById(R.id.ivViewAllDoctors)
        
        // Bottom navigation
        navHome = findViewById(R.id.navHome)
        navAppointments = findViewById(R.id.navAppointments)
        navDoctors = findViewById(R.id.navDoctors)
        navNotes = findViewById(R.id.navNotes)
        navProfile = findViewById(R.id.navProfile)
    }
    
    private fun setupClickListeners() {
        btnFindHelp.setOnClickListener {
            val intent = Intent(this, AllDoctorsActivity::class.java)
            startActivity(intent)
        }
        
        cardFeaturedDoctor.setOnClickListener {
            val featuredDoctor = promotedDoctors.firstOrNull { it.isPromotionValid() }
            featuredDoctor?.let { scheduleDoctor(it) }
        }
        
        ivViewAllDoctors.setOnClickListener {
            val intent = Intent(this, AllDoctorsActivity::class.java)
            startActivity(intent)
        }
        
        // Bottom navigation click listeners
        navHome.setOnClickListener {
            // Already on home
        }
        
        navAppointments.setOnClickListener {
            val intent = Intent(this, MyAppointmentsActivity::class.java)
            startActivity(intent)
        }
        
        navDoctors.setOnClickListener {
            val intent = Intent(this, AllDoctorsActivity::class.java)
            startActivity(intent)
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
    
    private fun setupRecyclerViews() {
        // RecyclerView para doutores (horizontal)
        recyclerViewDoctors.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        
        // RecyclerView para consultas (vertical) - seção "Próximas Consultas"
        recyclerViewAppointments.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        recyclerViewAppointments.setHasFixedSize(false)
        
        android.util.Log.d("PatientActivity", "✅ RecyclerViews configured")
    }
    
    private fun loadUserData() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            return
        }
        
        // Load patient data and appointments immediately after login
        loadPatientDataAndAppointments()
    }
    
    private fun loadPatientDataAndAppointments() {
        val currentUser = auth.currentUser ?: return
        val userId = currentUser.uid
        
        // First, try to find patient in the pacientes collection
        firestore.collection("pacientes").document(userId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    patientId = userId
                    android.util.Log.d("PatientActivity", "Patient found in pacientes collection: $userId")
                    loadAppointments(userId)
                } else {
                    // Try to find by authUid field
                    findPatientByAuthUid(userId)
                }
            }
            .addOnFailureListener {
                // Fallback: try to find by authUid field
                findPatientByAuthUid(userId)
            }
    }
    
    private fun findPatientByAuthUid(authUid: String) {
        firestore.collection("pacientes")
            .whereEqualTo("authUid", authUid)
            .limit(1)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val patientDoc = documents.first()
                    patientId = patientDoc.id
                    android.util.Log.d("PatientActivity", "Patient found by authUid: ${patientDoc.id}")
                    loadAppointments(patientDoc.id)
                } else {
                    // Last fallback: use auth UID directly
                    patientId = authUid
                    android.util.Log.d("PatientActivity", "Using auth UID as patientId: $authUid")
                    loadAppointments(authUid)
                }
            }
            .addOnFailureListener {
                // Last fallback: use auth UID directly
                patientId = authUid
                android.util.Log.d("PatientActivity", "Error finding patient, using auth UID: $authUid")
                loadAppointments(authUid)
            }
    }
    
    private fun loadDoctors() {
        loadPromotedDoctors()
        
        android.util.Log.d("PatientActivity", "🔍 Loading available doctors from Firestore...")
        
        // Load all doctors and sort locally to avoid Firestore composite index requirements
        firestore.collection("doutores")
            .get()
            .addOnSuccessListener { documents ->
                android.util.Log.d("PatientActivity", "✅ Found ${documents.size()} total doctors in Firestore")
                
                doctors.clear()
                val tempDoctors = mutableListOf<Doctor>()
                
                for (document in documents) {
                    try {
                        val doctor = Doctor.fromMap(document.data, document.id)
                        
                        // Only add non-promoted doctors or doctors with expired promotions
                        if (!doctor.isPromoted || !doctor.isPromotionValid()) {
                            tempDoctors.add(doctor)
                            android.util.Log.d("PatientActivity", "   Doctor: ${doctor.name}, Rating: ${doctor.rating}, Online: ${doctor.isAvailable}")
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("PatientActivity", "Error parsing doctor ${document.id}", e)
                    }
                }
                
                // Sort by rating in descending order (highest rating first)
                tempDoctors.sortByDescending { it.rating }
                
                // Take only top 10
                doctors.addAll(tempDoctors.take(10))
                
                android.util.Log.d("PatientActivity", "✅ Loaded ${doctors.size} available doctors (sorted by rating)")
                updateDoctorsUI()
            }
            .addOnFailureListener { e ->
                android.util.Log.e("PatientActivity", "❌ Error loading doctors from Firestore", e)
                Toast.makeText(this, "Erro ao carregar doutores: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
    
    private fun loadPromotedDoctors() {
        android.util.Log.d("PatientActivity", "🔍 Loading promoted doctors from Firestore...")
        
        firestore.collection("doutores")
            .whereEqualTo("isPromoted", true)
            .get()
            .addOnSuccessListener { documents ->
                android.util.Log.d("PatientActivity", "✅ Found ${documents.size()} promoted doctors")
                
                promotedDoctors.clear()
                for (document in documents) {
                    try {
                        val doctor = Doctor.fromMap(document.data, document.id)
                        if (doctor.isPromotionValid()) {
                            promotedDoctors.add(doctor)
                            android.util.Log.d("PatientActivity", "   Promoted Doctor: ${doctor.name}, Rating: ${doctor.rating}")
                        } else {
                            android.util.Log.d("PatientActivity", "   Skipped expired promotion: ${doctor.name}")
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("PatientActivity", "Error parsing promoted doctor ${document.id}", e)
                    }
                }
                
                android.util.Log.d("PatientActivity", "✅ Loaded ${promotedDoctors.size} valid promoted doctors")
                updateDoctorsUI()
            }
            .addOnFailureListener { e ->
                android.util.Log.e("PatientActivity", "❌ Error loading promoted doctors", e)
            }
    }
    
    private fun loadAllDoctors() {
        android.util.Log.d("PatientActivity", "🔍 Loading all doctors (fallback method)...")
        
        firestore.collection("doutores")
            .get()
            .addOnSuccessListener { documents ->
                android.util.Log.d("PatientActivity", "✅ Loaded ${documents.size()} doctors (fallback)")
                
                doctors.clear()
                promotedDoctors.clear()
                val tempDoctors = mutableListOf<Doctor>()
                
                for (document in documents) {
                    try {
                        val doctor = Doctor.fromMap(document.data, document.id)
                        if (doctor.isPromoted && doctor.isPromotionValid()) {
                            promotedDoctors.add(doctor)
                        } else {
                            tempDoctors.add(doctor)
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("PatientActivity", "Error parsing doctor ${document.id}", e)
                    }
                }
                
                // Sort by rating in descending order
                tempDoctors.sortByDescending { it.rating }
                
                // Take only top 10 for available doctors
                doctors.addAll(tempDoctors.take(10))
                
                android.util.Log.d("PatientActivity", "✅ Fallback complete: ${promotedDoctors.size} promoted, ${doctors.size} available")
                updateDoctorsUI()
            }
            .addOnFailureListener { e ->
                android.util.Log.e("PatientActivity", "❌ Error in fallback load", e)
            }
    }
    
    private fun updateDoctorsUI() {
        val featuredDoctor = promotedDoctors.firstOrNull()
        if (featuredDoctor != null) {
            cardFeaturedDoctor.visibility = View.VISIBLE
            tvFeaturedDoctorName.text = featuredDoctor.name
            tvFeaturedDoctorSpecialization.text = featuredDoctor.specialization
            tvFeaturedDoctorPrice.text = featuredDoctor.getPriceFormatted()
            if (featuredDoctor.rating > 0) {
                layoutFeaturedRating.visibility = View.VISIBLE
                tvFeaturedDoctorRating.text = String.format("%.1f", featuredDoctor.rating)
            } else {
                layoutFeaturedRating.visibility = View.GONE
            }
            viewOnlineStatus.visibility = if (featuredDoctor.isAvailable) View.VISIBLE else View.GONE
            if (featuredDoctor.photoUrl.isNotEmpty()) {
                Glide.with(this).load(featuredDoctor.photoUrl).centerCrop().into(ivFeaturedDoctorPhoto)
            }
        } else {
            cardFeaturedDoctor.visibility = View.GONE
        }
        
        val allDoctors = (promotedDoctors.drop(1) + doctors).distinctBy { it.id }
        recyclerViewDoctors.adapter = DoctorAdapter(allDoctors) { doctor ->
            scheduleDoctor(doctor)
        }
    }
    
    private fun loadAppointments(patientIdToUse: String) {
        if (patientIdToUse.isEmpty()) {
            android.util.Log.e("PatientActivity", "PatientId is empty, cannot load appointments")
            appointments.clear()
            updateAppointmentsUI()
            return
        }
        
        android.util.Log.d("PatientActivity", "🔍 Loading appointments for patientId: $patientIdToUse")
        
        // Buscar todas as consultas do paciente usando patientId
        firestore.collection("appointments")
            .whereEqualTo("patientId", patientIdToUse)
            .get()
            .addOnSuccessListener { querySnapshot ->
                android.util.Log.d("PatientActivity", "✅ Found ${querySnapshot.size()} total appointments for patientId: $patientIdToUse")
                
                appointments.clear()
                val now = com.google.firebase.Timestamp.now()
                
                if (querySnapshot.isEmpty) {
                    android.util.Log.d("PatientActivity", "⚠️ No appointments found for this patient")
                    updateAppointmentsUI()
                    return@addOnSuccessListener
                }
                
                // Processar cada documento de consulta
                for (document in querySnapshot.documents) {
                    try {
                        val data = document.data
                        if (data == null) {
                            android.util.Log.w("PatientActivity", "⚠️ Document ${document.id} has null data")
                            continue
                        }
                        
                        val appointment = Appointment.fromMap(data, document.id)
                        android.util.Log.d("PatientActivity", "📋 Processing appointment: ${appointment.doctorName}, startTime: ${appointment.startTime}, status: ${appointment.status}")
                        
                        // Mostrar consultas com status "Agendada" ou "Confirmada"
                        // Incluir tanto futuras quanto do passado recente (últimos 30 dias) para dar visibilidade ao paciente
                        val appointmentTime = appointment.startTime
                        val status = appointment.status.lowercase()
                        
                        // Filtrar por status: mostrar apenas consultas agendadas/confirmadas (não completadas ou canceladas)
                        val isValidStatus = status == "agendada" || status == "confirmada" || status == "scheduled" || status == "confirmed"
                        
                        if (!isValidStatus) {
                            android.util.Log.d("PatientActivity", "⏭️ Skipped appointment with status: ${appointment.status}")
                            continue
                        }
                        
                        if (appointmentTime != null) {
                            // Mostrar consultas futuras OU do passado recente (últimos 30 dias)
                            val thirtyDaysAgo = com.google.firebase.Timestamp(now.seconds - (30 * 24 * 60 * 60), now.nanoseconds)
                            val isFutureOrRecent = appointmentTime.compareTo(now) > 0 || appointmentTime.compareTo(thirtyDaysAgo) > 0
                            
                            if (isFutureOrRecent) {
                                appointments.add(appointment)
                                if (appointmentTime.compareTo(now) > 0) {
                                    android.util.Log.d("PatientActivity", "✅ Added future appointment: ${appointment.doctorName} at ${appointment.startTime}")
                                } else {
                                    android.util.Log.d("PatientActivity", "✅ Added recent past appointment: ${appointment.doctorName} at ${appointment.startTime}")
                                }
                            } else {
                                android.util.Log.d("PatientActivity", "⏭️ Skipped old appointment (>30 days): ${appointment.doctorName} at ${appointment.startTime}")
                            }
                        } else {
                            // Se não tem startTime, ainda assim mostrar se tiver status válido (casos especiais)
                            appointments.add(appointment)
                            android.util.Log.w("PatientActivity", "⚠️ Added appointment without startTime: ${appointment.doctorName}, status: ${appointment.status}")
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("PatientActivity", "❌ Error parsing appointment ${document.id}", e)
                        android.util.Log.e("PatientActivity", "Document data: ${document.data}")
                    }
                }
                
                // Ordenar por data/hora em ordem crescente (próximas primeiro)
                appointments.sortWith(compareBy(nullsLast()) { it.startTime })
                
                // Limitar a 10 mais próximas
                val limitedAppointments = appointments.take(10)
                appointments.clear()
                appointments.addAll(limitedAppointments)
                
                android.util.Log.d("PatientActivity", "✅ Final appointments count: ${appointments.size}")
                
                // Atualizar UI na thread principal
                runOnUiThread {
                    updateAppointmentsUI()
                }
            }
            .addOnFailureListener { e ->
                android.util.Log.e("PatientActivity", "❌ Error loading appointments", e)
                android.util.Log.e("PatientActivity", "Error details: ${e.message}")
                e.printStackTrace()
                
                runOnUiThread {
                    Toast.makeText(this, "Erro ao carregar consultas: ${e.message}", Toast.LENGTH_LONG).show()
                    appointments.clear()
                    updateAppointmentsUI()
                }
            }
    }
    
    private fun updateAppointmentsUI() {
        android.util.Log.d("PatientActivity", "🎨 updateAppointmentsUI called with ${appointments.size} appointments")
        
        // Mostrar badge com contagem total de consultas futuras
        if (appointments.isNotEmpty()) {
            viewAppointmentBadge.visibility = View.VISIBLE
            tvAppointmentCount.text = appointments.size.toString()
            android.util.Log.d("PatientActivity", "✅ Showing badge with count: ${appointments.size}")
        } else {
            viewAppointmentBadge.visibility = View.GONE
            android.util.Log.d("PatientActivity", "⚠️ No appointments, hiding badge")
        }
        
        // Mostrar até 3 consultas na seção "Próximas Consultas" (limite ideal para UI/UX)
        // Isso evita sobrecarregar a tela e mantém o foco nas próximas consultas mais importantes
        val appointmentsToShow = appointments.take(3)
        android.util.Log.d("PatientActivity", "📱 Displaying ${appointmentsToShow.size} appointments in RecyclerView (limit: 3)")
        
        if (appointmentsToShow.isNotEmpty()) {
            for (i in appointmentsToShow.indices) {
                val apt = appointmentsToShow[i]
                android.util.Log.d("PatientActivity", "   Appointment $i: ${apt.doctorName} - ${apt.startTime} - Status: ${apt.status}")
            }
        } else {
            android.util.Log.d("PatientActivity", "   No appointments to display")
        }
        
        // Garantir que o RecyclerView seja visível quando há consultas
        if (appointmentsToShow.isNotEmpty()) {
            recyclerViewAppointments.visibility = View.VISIBLE
            
            // Criar ou atualizar o adapter
            val currentAdapter = recyclerViewAppointments.adapter as? AppointmentAdapter
            if (currentAdapter == null) {
                // Criar novo adapter
                recyclerViewAppointments.adapter = AppointmentAdapter(appointmentsToShow, UserType.PATIENT) { appointment ->
                    // Ao clicar em uma consulta, abre a tela de consultas completa
                    enterAppointment(appointment)
                }
                android.util.Log.d("PatientActivity", "✅ Created new adapter with ${appointmentsToShow.size} appointments")
            } else {
                // Atualizar adapter existente
                // Como a lista mudou, criamos um novo adapter com os novos dados
                recyclerViewAppointments.adapter = AppointmentAdapter(appointmentsToShow, UserType.PATIENT) { appointment ->
                    enterAppointment(appointment)
                }
            }
            
            // Notificar mudanças
            recyclerViewAppointments.adapter?.notifyDataSetChanged()
            android.util.Log.d("PatientActivity", "✅ RecyclerView updated and visible")
        } else {
            // Sem consultas, esconder o RecyclerView
            recyclerViewAppointments.visibility = View.GONE
            android.util.Log.d("PatientActivity", "⚠️ No appointments to show, hiding RecyclerView")
        }
        
        android.util.Log.d("PatientActivity", "✅ UI updated successfully - RecyclerView visibility: ${recyclerViewAppointments.visibility}")
    }
    
    private fun scheduleDoctor(doctor: Doctor) {
        val intent = Intent(this, DoctorProfileActivity::class.java)
        intent.putExtra("DOCTOR_ID", doctor.id)
        startActivity(intent)
    }
    
    private fun enterAppointment(appointment: Appointment) {
        val intent = Intent(this, MyAppointmentsActivity::class.java)
        startActivity(intent)
    }
    
    private fun logout() {
        auth.signOut()
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
