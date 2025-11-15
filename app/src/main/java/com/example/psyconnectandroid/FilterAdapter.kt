package com.example.psyconnectandroid

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView

class FilterAdapter(
    private val filterOptions: List<String>,
    private val onFilterSelected: (String) -> Unit
) : RecyclerView.Adapter<FilterAdapter.FilterViewHolder>() {

    private var selectedPosition = 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FilterViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_filter_chip, parent, false)
        return FilterViewHolder(view)
    }

    override fun onBindViewHolder(holder: FilterViewHolder, position: Int) {
        holder.bind(filterOptions[position], position == selectedPosition)
    }

    override fun getItemCount(): Int = filterOptions.size

    inner class FilterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardView: CardView = itemView as CardView
        private val tvFilterText: TextView = itemView.findViewById(R.id.tvFilterText)

        fun bind(filter: String, isSelected: Boolean) {
            tvFilterText.text = filter
            
            // Update background based on selection state
            cardView.setCardBackgroundColor(
                if (isSelected) {
                    itemView.context.getColor(R.color.primary_blue)
                } else {
                    itemView.context.getColor(R.color.light_gray)
                }
            )
            
            // Update text color for better contrast
            tvFilterText.setTextColor(
                if (isSelected) {
                    itemView.context.getColor(R.color.white)
                } else {
                    itemView.context.getColor(R.color.text_primary)
                }
            )
            
            itemView.setOnClickListener {
                val previousPosition = selectedPosition
                selectedPosition = adapterPosition
                
                // Notify previous item to update
                notifyItemChanged(previousPosition)
                // Notify current item to update
                notifyItemChanged(selectedPosition)
                
                // Call the callback
                onFilterSelected(filter)
            }
        }
    }
}

