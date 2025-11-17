package com.example.psyconnectandroid

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Locale

class AppointmentAdapter(
    private val appointments: List<Appointment>,
    private val userType: UserType, // Differentiates view for patient or doctor
    private val onAppointmentClick: (Appointment) -> Unit
) : RecyclerView.Adapter<AppointmentAdapter.AppointmentViewHolder>() {
    
    private val firestore = FirebaseFirestore.getInstance()
    private val doctorPhotoCache = mutableMapOf<String, String>() // Cache de fotos de doutores

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppointmentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_appointment_card, parent, false)
        android.util.Log.d("AppointmentAdapter", "Creating view holder for position $viewType")
        return AppointmentViewHolder(view)
    }

    override fun onBindViewHolder(holder: AppointmentViewHolder, position: Int) {
        android.util.Log.d("AppointmentAdapter", "Binding appointment at position $position: ${appointments[position].doctorName}")
        holder.bind(appointments[position])
    }

    override fun getItemCount(): Int {
        android.util.Log.d("AppointmentAdapter", "getItemCount called: ${appointments.size}")
        return appointments.size
    }

    inner class AppointmentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivPhoto: ImageView = itemView.findViewById(R.id.ivAppointmentDoctorPhoto)
        private val tvName: TextView = itemView.findViewById(R.id.tvAppointmentDoctorName)
        private val tvSpecialization: TextView = itemView.findViewById(R.id.tvAppointmentSpecialization)
        private val tvDateTime: TextView = itemView.findViewById(R.id.tvAppointmentDateTime)
        private val tvStatus: TextView = itemView.findViewById(R.id.tvAppointmentStatus)

        fun bind(appointment: Appointment) {
            // Display patient name for doctors, and doctor name for patients
            tvName.text = if (userType == UserType.PSYCHOLOGIST) appointment.patientName else appointment.doctorName

            // Load and display specialization for patient view
            if (userType == UserType.PATIENT && appointment.doctorId.isNotEmpty()) {
                loadDoctorSpecialization(appointment.doctorId, tvSpecialization)
            } else {
                tvSpecialization.visibility = View.GONE
            }

            appointment.startTime?.toDate()?.let {
                val format = SimpleDateFormat("d 'de' MMMM, HH:mm", Locale("pt", "BR"))
                tvDateTime.text = format.format(it)
            }

            tvStatus.text = when (appointment.status.lowercase()) {
                "agendada", "scheduled", "confirmed" -> "Agendada"
                "completed", "completada", "concluída" -> "Concluída"
                "cancelled", "cancelada" -> "Cancelada"
                else -> "Pendente"
            }
            tvStatus.background = getStatusDrawable(itemView.context, appointment.status)

            // Carregar foto do doutor para pacientes, ou foto do paciente para doutores
            if (userType == UserType.PATIENT && appointment.doctorId.isNotEmpty()) {
                loadDoctorPhoto(appointment.doctorId, ivPhoto)
            } else if (userType == UserType.PSYCHOLOGIST && appointment.patientId.isNotEmpty()) {
                // Para doutores, podemos buscar foto do paciente também (se necessário)
                loadPatientPhoto(appointment.patientId, ivPhoto)
            } else {
                // Placeholder se não tiver ID
                Glide.with(itemView.context)
                    .load(R.drawable.ic_person)
                    .circleCrop()
                    .into(ivPhoto)
            }

            itemView.setOnClickListener {
                onAppointmentClick(appointment)
            }
        }
        
        private fun loadDoctorPhoto(doctorId: String, imageView: ImageView) {
            // Verificar cache primeiro
            val cachedUrl = doctorPhotoCache[doctorId]
            if (cachedUrl != null && cachedUrl.isNotEmpty()) {
                Glide.with(itemView.context)
                    .load(cachedUrl)
                    .placeholder(R.drawable.ic_person)
                    .error(R.drawable.ic_person)
                    .circleCrop()
                    .into(imageView)
                return
            }
            
            // Buscar do Firestore se não estiver no cache
            firestore.collection("doutores").document(doctorId).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val photoUrl = document.getString("profileImageURL")
                            ?: document.getString("profileImageUrl")
                            ?: document.getString("photoUrl")
                            ?: document.getString("photo")
                            ?: document.getString("imageUrl")
                            ?: ""
                        
                        // Atualizar cache
                        if (photoUrl.isNotEmpty()) {
                            doctorPhotoCache[doctorId] = photoUrl
                        }
                        
                        // Carregar imagem
                        if (photoUrl.isNotEmpty()) {
                            Glide.with(itemView.context)
                                .load(photoUrl)
                                .placeholder(R.drawable.ic_person)
                                .error(R.drawable.ic_person)
                                .circleCrop()
                                .into(imageView)
                        } else {
                            Glide.with(itemView.context)
                                .load(R.drawable.ic_person)
                                .circleCrop()
                                .into(imageView)
                        }
                    } else {
                        Glide.with(itemView.context)
                            .load(R.drawable.ic_person)
                            .circleCrop()
                            .into(imageView)
                    }
                }
                .addOnFailureListener {
                    // Em caso de erro, usar placeholder
                    Glide.with(itemView.context)
                        .load(R.drawable.ic_person)
                        .circleCrop()
                        .into(imageView)
                }
        }
        
        private fun loadPatientPhoto(patientId: String, imageView: ImageView) {
            // Buscar foto do paciente na coleção pacientes
            firestore.collection("pacientes").document(patientId).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val photoUrl = document.getString("photoUrl")
                            ?: document.getString("profileImageUrl")
                            ?: document.getString("profileImageURL")
                            ?: document.getString("imageUrl")
                            ?: ""
                        
                        if (photoUrl.isNotEmpty()) {
                            Glide.with(itemView.context)
                                .load(photoUrl)
                                .placeholder(R.drawable.ic_person)
                                .error(R.drawable.ic_person)
                                .circleCrop()
                                .into(imageView)
                        } else {
                            Glide.with(itemView.context)
                                .load(R.drawable.ic_person)
                                .circleCrop()
                                .into(imageView)
                        }
                    } else {
                        Glide.with(itemView.context)
                            .load(R.drawable.ic_person)
                            .circleCrop()
                            .into(imageView)
                    }
                }
                .addOnFailureListener {
                    Glide.with(itemView.context)
                        .load(R.drawable.ic_person)
                        .circleCrop()
                        .into(imageView)
                }
        }
        
        private fun loadDoctorSpecialization(doctorId: String, textView: TextView) {
            firestore.collection("doutores").document(doctorId).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val specialization = document.getString("specialization")
                            ?: document.getString("specialisation")
                            ?: document.getString("specialty")
                            ?: "Especialista"
                        
                        textView.text = specialization
                        textView.visibility = View.VISIBLE
                    } else {
                        textView.visibility = View.GONE
                    }
                }
                .addOnFailureListener {
                    textView.visibility = View.GONE
                }
        }

        private fun getStatusDrawable(context: Context, status: String): android.graphics.drawable.Drawable? {
            val drawableId = when (status.lowercase()) {
                "agendada", "scheduled" -> R.drawable.bg_status_confirmed // Usa confirmed para agendada
                "confirmed", "confirmada" -> R.drawable.bg_status_confirmed
                "completed", "completada", "concluída" -> R.drawable.bg_status_completed
                "cancelled", "cancelada" -> R.drawable.bg_status_cancelled
                else -> R.drawable.bg_status_pending
            }
            return ContextCompat.getDrawable(context, drawableId)
        }
    }
}