package com.example.psyconnectandroid

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Locale

class EditableAvailabilityAdapter(
    private val slots: List<AvailabilitySlot>,
    private val onRemoveClick: (AvailabilitySlot) -> Unit
) : RecyclerView.Adapter<EditableAvailabilityAdapter.SlotViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SlotViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_editable_availability_slot, parent, false)
        return SlotViewHolder(view)
    }

    override fun onBindViewHolder(holder: SlotViewHolder, position: Int) {
        holder.bind(slots[position])
    }

    override fun getItemCount(): Int = slots.size

    inner class SlotViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTime: TextView = itemView.findViewById(R.id.tvSlotTimeEditable)
        private val btnRemove: ImageButton = itemView.findViewById(R.id.btnRemoveSlot)

        fun bind(slot: AvailabilitySlot) {
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val startTimeStr = slot.startTime?.toDate()?.let { timeFormat.format(it) } ?: ""
            val endTimeStr = slot.endTime?.toDate()?.let { timeFormat.format(it) } ?: ""
            tvTime.text = "$startTimeStr - $endTimeStr"

            btnRemove.setOnClickListener {
                onRemoveClick(slot)
            }
        }
    }
}