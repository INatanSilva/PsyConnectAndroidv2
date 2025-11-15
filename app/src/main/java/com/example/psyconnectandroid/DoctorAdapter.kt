package com.example.psyconnectandroid

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class DoctorAdapter(
    private val doctors: List<Doctor>,
    private val onScheduleClick: (Doctor) -> Unit
) : RecyclerView.Adapter<DoctorAdapter.DoctorViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DoctorViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_doctor_card, parent, false)
        return DoctorViewHolder(view)
    }

    override fun onBindViewHolder(holder: DoctorViewHolder, position: Int) {
        holder.bind(doctors[position])
    }

    override fun getItemCount(): Int = doctors.size

    inner class DoctorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivPhoto: ImageView = itemView.findViewById(R.id.ivDoctorPhoto)
        private val tvName: TextView = itemView.findViewById(R.id.tvDoctorName)
        private val tvSpecialization: TextView = itemView.findViewById(R.id.tvDoctorSpecialization)
        private val layoutRating: View = itemView.findViewById(R.id.layoutRating)
        private val tvRating: TextView = itemView.findViewById(R.id.tvDoctorRating)
        private val tvReviews: TextView = itemView.findViewById(R.id.tvDoctorReviews)
        private val tvPrice: TextView = itemView.findViewById(R.id.tvDoctorPrice)
        private val viewOnlineStatus: View = itemView.findViewById(R.id.viewOnlineStatus)
        private val btnSchedule: Button = itemView.findViewById(R.id.btnSchedule)

        fun bind(doctor: Doctor) {
            tvName.text = doctor.name
            
            // Specialization
            if (doctor.specialization.isNotEmpty()) {
                tvSpecialization.text = doctor.specialization
                tvSpecialization.visibility = View.VISIBLE
            } else {
                tvSpecialization.text = "Psicólogo"
                tvSpecialization.visibility = View.VISIBLE
            }
            
            // Rating
            if (doctor.rating > 0) {
                layoutRating.visibility = View.VISIBLE
                tvRating.text = String.format("%.1f", doctor.rating)
                // You can add reviews count if available in Doctor model
                tvReviews.text = ""
            } else {
                layoutRating.visibility = View.GONE
            }
            
            // Price
            tvPrice.text = doctor.getPriceFormatted()
            
            // Online status
            viewOnlineStatus.visibility = if (doctor.isAvailable) View.VISIBLE else View.GONE

            // Load doctor photo from Firestore
            if (doctor.photoUrl.isNotEmpty()) {
                android.util.Log.d("DoctorAdapter", "Loading doctor photo for ${doctor.name}: ${doctor.photoUrl.take(50)}...")
                Glide.with(itemView.context)
                    .load(doctor.photoUrl)
                    .placeholder(R.drawable.ic_person)
                    .error(R.drawable.ic_person)
                    .centerCrop()
                    .into(ivPhoto)
            } else {
                android.util.Log.d("DoctorAdapter", "No photo URL for doctor: ${doctor.name}")
                // No photo URL available, use placeholder
                Glide.with(itemView.context)
                    .load(R.drawable.ic_person)
                    .centerCrop()
                    .into(ivPhoto)
            }

            btnSchedule.setOnClickListener {
                onScheduleClick(doctor)
            }
        }
    }
}

