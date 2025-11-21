package com.example.psyconnectandroid

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Adapter para lista de atividades recentes
 */
class RecentActivityAdapter(
    private val activities: List<ActivityItem>
) : RecyclerView.Adapter<RecentActivityAdapter.ActivityViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActivityViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recent_activity, parent, false)
        return ActivityViewHolder(view)
    }

    override fun onBindViewHolder(holder: ActivityViewHolder, position: Int) {
        holder.bind(activities[position])
    }

    override fun getItemCount(): Int = activities.size

    inner class ActivityViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivIcon: ImageView = itemView.findViewById(R.id.ivActivityIcon)
        private val tvDescription: TextView = itemView.findViewById(R.id.tvActivityDescription)
        private val tvTimestamp: TextView = itemView.findViewById(R.id.tvActivityTimestamp)
        private val tvAmount: TextView = itemView.findViewById(R.id.tvActivityAmount)

        fun bind(activity: ActivityItem) {
            // Ícone baseado no tipo
            val iconRes = when (activity.type) {
                ActivityType.PAYMENT -> R.drawable.ic_payment
                ActivityType.CONSULTATION -> R.drawable.ic_video
                ActivityType.CANCELLATION -> R.drawable.ic_cancel
            }
            ivIcon.setImageResource(iconRes)

            // Descrição
            tvDescription.text = activity.description

            // Timestamp formatado
            tvTimestamp.text = formatTimestamp(activity.timestamp)

            // Valor (se houver)
            if (activity.amount != null && activity.amount > 0) {
                tvAmount.text = "€${String.format("%.2f", activity.amount)}"
                tvAmount.visibility = View.VISIBLE
            } else {
                tvAmount.visibility = View.GONE
            }
        }

        private fun formatTimestamp(timestamp: Date): String {
            val now = Date()
            val diff = now.time - timestamp.time
            val days = TimeUnit.MILLISECONDS.toDays(diff)
            val hours = TimeUnit.MILLISECONDS.toHours(diff)
            val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)

            return when {
                days > 7 -> {
                    val weeks = days / 7
                    if (weeks == 1L) "1 wk ago" else "$weeks wk ago"
                }
                days > 0 -> {
                    if (days == 1L) "1 day ago" else "$days days ago"
                }
                hours > 0 -> {
                    if (hours == 1L) "1 hr ago" else "$hours hr ago"
                }
                minutes > 0 -> {
                    if (minutes == 1L) "1 min ago" else "$minutes min ago"
                }
                else -> "Just now"
            }
        }
    }
}



