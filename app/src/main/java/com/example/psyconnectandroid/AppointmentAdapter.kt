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
import java.text.SimpleDateFormat
import java.util.Locale

class AppointmentAdapter(
    private val appointments: List<Appointment>,
    private val userType: UserType, // Differentiates view for patient or doctor
    private val onAppointmentClick: (Appointment) -> Unit
) : RecyclerView.Adapter<AppointmentAdapter.AppointmentViewHolder>() {

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
        private val tvDateTime: TextView = itemView.findViewById(R.id.tvAppointmentDateTime)
        private val tvStatus: TextView = itemView.findViewById(R.id.tvAppointmentStatus)

        fun bind(appointment: Appointment) {
            // Display patient name for doctors, and doctor name for patients
            tvName.text = if (userType == UserType.PSYCHOLOGIST) appointment.patientName else appointment.doctorName

            appointment.startTime?.toDate()?.let {
                val format = SimpleDateFormat("d 'de' MMMM, HH:mm", Locale("pt", "BR"))
                tvDateTime.text = format.format(it)
            }

            tvStatus.text = appointment.status.replaceFirstChar { it.titlecase() }
            tvStatus.background = getStatusDrawable(itemView.context, appointment.status)

            // TODO: Fetch the correct photo URL (patient's for doctor, doctor's for patient)
            Glide.with(itemView.context)
                .load(R.drawable.ic_person)
                .circleCrop()
                .into(ivPhoto)

            itemView.setOnClickListener {
                onAppointmentClick(appointment)
            }
        }

        private fun getStatusDrawable(context: Context, status: String): android.graphics.drawable.Drawable? {
            val drawableId = when (status.lowercase()) {
                "confirmed" -> R.drawable.bg_status_confirmed
                "completed" -> R.drawable.bg_status_completed
                "cancelled" -> R.drawable.bg_status_cancelled
                else -> R.drawable.bg_status_pending
            }
            return ContextCompat.getDrawable(context, drawableId)
        }
    }
}