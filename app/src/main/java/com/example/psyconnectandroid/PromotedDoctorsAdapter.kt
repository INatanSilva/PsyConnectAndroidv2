package com.example.psyconnectandroid

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

/**
 * Adapter para lista horizontal de doutores promovidos
 * Segue o design do iOS com cards de 200dp de largura e 280dp de altura
 */
class PromotedDoctorsAdapter(
    private val doctors: List<Doctor>,
    private val onScheduleClick: (Doctor) -> Unit
) : RecyclerView.Adapter<PromotedDoctorsAdapter.PromotedDoctorViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PromotedDoctorViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_promoted_doctor_card, parent, false)
        return PromotedDoctorViewHolder(view)
    }

    override fun onBindViewHolder(holder: PromotedDoctorViewHolder, position: Int) {
        holder.bind(doctors[position])
    }

    override fun getItemCount(): Int = doctors.size

    inner class PromotedDoctorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivPhoto: ImageView = itemView.findViewById(R.id.ivDoctorPhoto)
        private val ivPromotionBadge: ImageView = itemView.findViewById(R.id.ivPromotionBadge)
        private val tvName: TextView = itemView.findViewById(R.id.tvDoctorName)
        private val tvSpecialization: TextView = itemView.findViewById(R.id.tvDoctorSpecialization)
        private val tvPrice: TextView = itemView.findViewById(R.id.tvDoctorPrice)
        private val viewOnlineStatus: View = itemView.findViewById(R.id.viewOnlineStatus)
        private val btnSchedule: Button = itemView.findViewById(R.id.btnSchedule)

        fun bind(doctor: Doctor) {
            // Nome
            tvName.text = doctor.name

            // Especialidade
            if (doctor.specialization.isNotEmpty()) {
                tvSpecialization.text = doctor.specialization
            } else {
                tvSpecialization.text = "Psicólogo"
            }

            // Preço
            tvPrice.text = doctor.getPriceFormatted()

            // Status online
            viewOnlineStatus.visibility = if (doctor.isAvailable) View.VISIBLE else View.GONE

            // Badge de promoção (sempre visível para doutores promovidos)
            ivPromotionBadge.visibility = if (doctor.isPromoted && doctor.isPromotionValid()) {
                View.VISIBLE
            } else {
                View.GONE
            }

            // Carregar foto do doutor
            if (doctor.photoUrl.isNotEmpty()) {
                Glide.with(itemView.context)
                    .load(doctor.photoUrl)
                    .placeholder(R.drawable.ic_person)
                    .error(R.drawable.ic_person)
                    .centerCrop()
                    .into(ivPhoto)
            } else {
                Glide.with(itemView.context)
                    .load(R.drawable.ic_person)
                    .centerCrop()
                    .into(ivPhoto)
            }

            // Botão de agendar
            btnSchedule.setOnClickListener {
                onScheduleClick(doctor)
            }

            // Click no card inteiro também abre o perfil
            itemView.setOnClickListener {
                onScheduleClick(doctor)
            }
        }
    }
}

