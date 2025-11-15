package com.example.psyconnectandroid

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Locale

class AvailabilityAdapter(
    private val slots: List<AvailabilitySlot>,
    private val onSlotClick: (AvailabilitySlot) -> Unit
) : RecyclerView.Adapter<AvailabilityAdapter.SlotViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SlotViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_availability_slot, parent, false)
        return SlotViewHolder(view)
    }

    override fun onBindViewHolder(holder: SlotViewHolder, position: Int) {
        holder.bind(slots[position])
    }

    override fun getItemCount(): Int = slots.size

    inner class SlotViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvDate: TextView = itemView.findViewById(R.id.tvSlotDate)
        private val tvTime: TextView = itemView.findViewById(R.id.tvSlotTime)

        fun bind(slot: AvailabilitySlot) {
            slot.startTime?.toDate()?.let {
                val dateFormat = SimpleDateFormat("d MMM", Locale.getDefault())
                val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                tvDate.text = dateFormat.format(it)
                tvTime.text = timeFormat.format(it)
            }

            itemView.setOnClickListener {
                onSlotClick(slot)
            }
        }
    }
}