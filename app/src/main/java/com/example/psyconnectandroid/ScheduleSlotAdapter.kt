package com.example.psyconnectandroid

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Adapter para slots da agenda
 */
class ScheduleSlotAdapter(
    private val slots: List<DoctorAvailability>,
    private val onSlotClick: (DoctorAvailability) -> Unit
) : RecyclerView.Adapter<ScheduleSlotAdapter.SlotViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SlotViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_schedule_slot, parent, false)
        return SlotViewHolder(view)
    }

    override fun onBindViewHolder(holder: SlotViewHolder, position: Int) {
        holder.bind(slots[position])
    }

    override fun getItemCount(): Int = slots.size

    inner class SlotViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTime: TextView = itemView.findViewById(R.id.tvTime)
        private val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        private val tvPatientName: TextView = itemView.findViewById(R.id.tvPatientName)
        private val btnAction: Button = itemView.findViewById(R.id.btnAction)

        fun bind(slot: DoctorAvailability) {
            // Format time
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val startTime = slot.startTime?.toDate()
            val endTime = slot.endTime?.toDate()
            
            if (startTime != null && endTime != null) {
                tvTime.text = "${timeFormat.format(startTime)} - ${timeFormat.format(endTime)}"
            } else {
                tvTime.text = "Horário não definido"
            }
            
            // Status
            when {
                slot.isBooked -> {
                    tvStatus.text = "Ocupado"
                    tvStatus.setTextColor(itemView.context.getColor(R.color.red))
                    tvPatientName.text = slot.patientName ?: "Paciente"
                    tvPatientName.visibility = View.VISIBLE
                    btnAction.text = "Cancelar"
                }
                slot.isAvailable -> {
                    tvStatus.text = "Disponível"
                    tvStatus.setTextColor(itemView.context.getColor(R.color.green))
                    tvPatientName.visibility = View.GONE
                    btnAction.text = "Desabilitar"
                }
                else -> {
                    tvStatus.text = "Indisponível"
                    tvStatus.setTextColor(itemView.context.getColor(R.color.text_secondary))
                    tvPatientName.visibility = View.GONE
                    btnAction.text = "Habilitar"
                }
            }
            
            btnAction.setOnClickListener {
                onSlotClick(slot)
            }
        }
    }
}


