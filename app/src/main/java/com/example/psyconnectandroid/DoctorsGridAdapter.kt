package com.example.psyconnectandroid

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class DoctorsGridAdapter(
    private val doctors: List<Doctor>,
    private val onConnectClick: (Doctor) -> Unit
) : RecyclerView.Adapter<DoctorsGridAdapter.DoctorGridViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DoctorGridViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_doctor_grid, parent, false)
        return DoctorGridViewHolder(view)
    }

    override fun onBindViewHolder(holder: DoctorGridViewHolder, position: Int) {
        holder.bind(doctors[position])
    }

    override fun getItemCount(): Int = doctors.size

    inner class DoctorGridViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivPhoto: ImageView = itemView.findViewById(R.id.ivDoctorPhoto)
        private val ivFeaturedStar: ImageView = itemView.findViewById(R.id.ivFeaturedStar)
        private val tvName: TextView = itemView.findViewById(R.id.tvDoctorName)
        private val tvPrice: TextView = itemView.findViewById(R.id.tvDoctorPrice)
        private val tvOnlineStatus: TextView = itemView.findViewById(R.id.tvOnlineStatus)
        private val viewOnlineIndicator: View = itemView.findViewById(R.id.viewOnlineIndicator)
        private val btnConnect: Button = itemView.findViewById(R.id.btnConnect)

        fun bind(doctor: Doctor) {
            tvName.text = doctor.name
            
            // Price
            if (doctor.priceEurCents > 0) {
                tvPrice.text = doctor.getPriceFormatted()
                tvPrice.visibility = View.VISIBLE
            } else {
                tvPrice.visibility = View.GONE
            }
            
            // Online status
            if (doctor.isAvailable) {
                tvOnlineStatus.visibility = View.VISIBLE
                viewOnlineIndicator.visibility = View.VISIBLE
                tvOnlineStatus.text = "Online"
            } else {
                tvOnlineStatus.visibility = View.GONE
                viewOnlineIndicator.visibility = View.GONE
            }
            
            // Featured/Promoted indicator
            if (doctor.isPromoted && doctor.isPromotionValid()) {
                ivFeaturedStar.visibility = View.VISIBLE
            } else {
                ivFeaturedStar.visibility = View.GONE
            }

            // Load doctor photo from Firestore
            if (doctor.photoUrl.isNotEmpty()) {
                Glide.with(itemView.context)
                    .load(doctor.photoUrl)
                    .placeholder(R.drawable.ic_person)
                    .error(R.drawable.ic_person)
                    .circleCrop()
                    .into(ivPhoto)
            } else {
                Glide.with(itemView.context)
                    .load(R.drawable.ic_person)
                    .circleCrop()
                    .into(ivPhoto)
            }

            btnConnect.setOnClickListener {
                onConnectClick(doctor)
            }
        }
    }
}







