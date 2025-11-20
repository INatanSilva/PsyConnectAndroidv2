package com.example.psyconnectandroid

import android.content.Context
import android.content.Intent
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
        private val tvDate: TextView = itemView.findViewById(R.id.tvAppointmentDate)
        private val tvTime: TextView = itemView.findViewById(R.id.tvAppointmentTime)
        private val tvStatus: TextView = itemView.findViewById(R.id.tvAppointmentStatus)
        private val btnEnterVideo: android.widget.Button = itemView.findViewById(R.id.btnEnterVideo)
        private val btnChat: ImageView = itemView.findViewById(R.id.btnChat)
        private val btnNotes: ImageView = itemView.findViewById(R.id.btnNotes)

        fun bind(appointment: Appointment) {
            // Display patient name for doctors, and doctor name for patients
            tvName.text = if (userType == UserType.PSYCHOLOGIST) appointment.patientName else appointment.doctorName

            // Format date and time separately
            appointment.startTime?.toDate()?.let { startDate ->
                // Date format: "4 November 2025" (formato iOS)
                val dateFormat = SimpleDateFormat("d MMMM yyyy", Locale("en", "US"))
                tvDate.text = dateFormat.format(startDate)
                
                // Time format: "19:54 - 20:54"
                val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                val startTimeStr = timeFormat.format(startDate)
                
                appointment.endTime?.toDate()?.let { endDate ->
                    val endTimeStr = timeFormat.format(endDate)
                    tvTime.text = "$startTimeStr - $endTimeStr"
                } ?: run {
                    tvTime.text = startTimeStr
                }
            } ?: run {
                tvDate.text = ""
                tvTime.text = ""
            }

            // Status badge
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
                loadPatientPhoto(appointment.patientId, ivPhoto)
            } else {
                Glide.with(itemView.context)
                    .load(R.drawable.ic_person)
                    .circleCrop()
                    .into(ivPhoto)
            }

            // Botão Entrar (Video Call) - apenas para consultas agendadas/confirmadas
            val isScheduled = appointment.status.lowercase() in listOf("agendada", "scheduled", "confirmed")
            if (isScheduled) {
                btnEnterVideo.visibility = View.VISIBLE
                btnEnterVideo.setOnClickListener {
                    startVideoCall(appointment)
                }
            } else {
                btnEnterVideo.visibility = View.GONE
            }

            // Botão Chat - apenas para pacientes
            if (userType == UserType.PATIENT && appointment.doctorId.isNotEmpty()) {
                btnChat.visibility = View.VISIBLE
                btnChat.setOnClickListener {
                    openChatWithDoctor(appointment)
                }
            } else {
                btnChat.visibility = View.GONE
            }

            // Botão Anotações
            btnNotes.setOnClickListener {
                // TODO: Implementar tela de anotações
                android.widget.Toast.makeText(itemView.context, "Anotações em desenvolvimento", android.widget.Toast.LENGTH_SHORT).show()
            }
            
            itemView.setOnClickListener {
                onAppointmentClick(appointment)
            }
        }
        
        private fun loadDoctorPhoto(doctorId: String, imageView: ImageView) {
            // Verificar cache de fotos primeiro
            val cachedUrl = PhotoCache.get(doctorId) ?: doctorPhotoCache[doctorId]
            if (cachedUrl != null && cachedUrl.isNotEmpty()) {
                Glide.with(itemView.context)
                    .load(cachedUrl)
                    .placeholder(R.drawable.ic_person)
                    .error(R.drawable.ic_person)
                    .circleCrop()
                    .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.ALL)
                    .into(imageView)
                return
            }
            
            // Verificar cache de doctor completo
            val cachedDoctor = DoctorCache.get(doctorId)
            if (cachedDoctor != null && cachedDoctor.photoUrl.isNotEmpty()) {
                PhotoCache.put(doctorId, cachedDoctor.photoUrl)
                Glide.with(itemView.context)
                    .load(cachedDoctor.photoUrl)
                    .placeholder(R.drawable.ic_person)
                    .error(R.drawable.ic_person)
                    .circleCrop()
                    .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.ALL)
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
                            PhotoCache.put(doctorId, photoUrl)
                        }
                        
                        // Carregar imagem
                        if (photoUrl.isNotEmpty()) {
                            Glide.with(itemView.context)
                                .load(photoUrl)
                                .placeholder(R.drawable.ic_person)
                                .error(R.drawable.ic_person)
                                .circleCrop()
                                .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.ALL)
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
            // Verificar cache primeiro
            val cachedUrl = PhotoCache.get(patientId)
            if (cachedUrl != null && cachedUrl.isNotEmpty()) {
                Glide.with(itemView.context)
                    .load(cachedUrl)
                    .placeholder(R.drawable.ic_person)
                    .error(R.drawable.ic_person)
                    .circleCrop()
                    .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.ALL)
                    .into(imageView)
                return
            }
            
            // Buscar foto do paciente na coleção pacientes
            firestore.collection("pacientes").document(patientId).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val photoUrl = document.getString("photoUrl")
                            ?: document.getString("profileImageUrl")
                            ?: document.getString("profileImageURL")
                            ?: document.getString("imageUrl")
                            ?: ""
                        
                        // Salvar no cache
                        if (photoUrl.isNotEmpty()) {
                            PhotoCache.put(patientId, photoUrl)
                        }
                        
                        if (photoUrl.isNotEmpty()) {
                            Glide.with(itemView.context)
                                .load(photoUrl)
                                .placeholder(R.drawable.ic_person)
                                .error(R.drawable.ic_person)
                                .circleCrop()
                                .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.ALL)
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
                "agendada", "scheduled" -> R.drawable.bg_status_scheduled
                "confirmed", "confirmada" -> R.drawable.bg_status_confirmed
                "completed", "completada", "concluída" -> R.drawable.bg_status_completed
                "cancelled", "cancelada" -> R.drawable.bg_status_cancelled
                else -> R.drawable.bg_status_pending
            }
            return ContextCompat.getDrawable(context, drawableId)
        }
        
        private fun startVideoCall(appointment: Appointment) {
            val context = itemView.context
            val currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
            if (currentUserId == null) {
                android.widget.Toast.makeText(context, "Erro: usuário não autenticado", android.widget.Toast.LENGTH_SHORT).show()
                return
            }
            
            // Determinar quem é o caller e quem é o callee
            val (callerId, calleeId, patientName) = if (userType == UserType.PSYCHOLOGIST) {
                // Doutor inicia chamada para paciente
                Triple(currentUserId, appointment.patientId, appointment.patientName)
            } else {
                // Paciente inicia chamada para doutor
                Triple(currentUserId, appointment.doctorId, appointment.doctorName)
            }
            
            if (calleeId.isEmpty()) {
                android.widget.Toast.makeText(context, "Erro: ID do destinatário não encontrado", android.widget.Toast.LENGTH_SHORT).show()
                return
            }
            
            // Iniciar chamada
            CallService.initiateCall(
                patientId = calleeId,
                patientName = patientName.ifEmpty { "Usuário" },
                onSuccess = { callId ->
                    // Abrir CallActivity como iniciador
                    val intent = Intent(context, CallActivity::class.java).apply {
                        putExtra("CALL_ID", callId)
                        putExtra("IS_INITIATOR", true)
                        putExtra("CALLER_ID", callerId)
                        putExtra("PATIENT_NAME", patientName)
                    }
                    context.startActivity(intent)
                },
                onError = { e ->
                    android.util.Log.e("AppointmentAdapter", "Erro ao iniciar chamada", e)
                    android.widget.Toast.makeText(context, "Erro ao iniciar chamada: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                }
            )
        }
        
        private fun openChatWithDoctor(appointment: Appointment) {
            val context = itemView.context
            val doctorId = appointment.doctorId
            val doctorName = appointment.doctorName
            
            // Buscar foto do doutor
            val cachedUrl = PhotoCache.get(doctorId) ?: doctorPhotoCache[doctorId]
            val doctorPhotoUrl = cachedUrl ?: ""
            
            // Buscar dados do paciente atual
            val currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
            if (currentUserId == null) {
                android.widget.Toast.makeText(context, "Erro: usuário não autenticado", android.widget.Toast.LENGTH_SHORT).show()
                return
            }
            
            // Buscar nome e foto do paciente
            firestore.collection("pacientes").document(currentUserId).get()
                .addOnSuccessListener { patientDoc ->
                    val patientName = patientDoc.getString("name") 
                        ?: patientDoc.getString("fullName") 
                        ?: "Paciente"
                    val patientPhotoUrl = patientDoc.getString("profileImageURL") 
                        ?: patientDoc.getString("photoUrl") 
                        ?: ""
                    
                    // Criar ou obter chatRoom
                    ChatHelper.createOrGetChatRoom(
                        patientId = currentUserId,
                        doctorId = doctorId,
                        patientName = patientName,
                        doctorName = doctorName,
                        patientPhotoUrl = patientPhotoUrl,
                        doctorPhotoUrl = doctorPhotoUrl,
                        onSuccess = { chatRoomId ->
                            // Abrir ChatActivity
                            val intent = Intent(context, ChatActivity::class.java)
                            intent.putExtra("CHAT_ROOM_ID", chatRoomId)
                            intent.putExtra("PATIENT_ID", currentUserId)
                            intent.putExtra("DOCTOR_ID", doctorId)
                            intent.putExtra("PATIENT_NAME", patientName)
                            intent.putExtra("DOCTOR_NAME", doctorName)
                            intent.putExtra("PATIENT_PHOTO_URL", patientPhotoUrl)
                            intent.putExtra("DOCTOR_PHOTO_URL", doctorPhotoUrl)
                            context.startActivity(intent)
                        },
                        onFailure = { e ->
                            android.util.Log.e("AppointmentAdapter", "Error creating chat room", e)
                            android.widget.Toast.makeText(context, "Erro ao abrir conversa", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    )
                }
                .addOnFailureListener { e ->
                    // Se não conseguir buscar dados do paciente, usar valores padrão
                    ChatHelper.createOrGetChatRoom(
                        patientId = currentUserId,
                        doctorId = doctorId,
                        patientName = "Paciente",
                        doctorName = doctorName,
                        patientPhotoUrl = "",
                        doctorPhotoUrl = doctorPhotoUrl,
                        onSuccess = { chatRoomId ->
                            val intent = Intent(context, ChatActivity::class.java)
                            intent.putExtra("CHAT_ROOM_ID", chatRoomId)
                            intent.putExtra("PATIENT_ID", currentUserId)
                            intent.putExtra("DOCTOR_ID", doctorId)
                            intent.putExtra("PATIENT_NAME", "Paciente")
                            intent.putExtra("DOCTOR_NAME", doctorName)
                            intent.putExtra("PATIENT_PHOTO_URL", "")
                            intent.putExtra("DOCTOR_PHOTO_URL", doctorPhotoUrl)
                            context.startActivity(intent)
                        },
                        onFailure = { ex ->
                            android.util.Log.e("AppointmentAdapter", "Error creating chat room", ex)
                            android.widget.Toast.makeText(context, "Erro ao abrir conversa", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    )
                }
        }
    }
}