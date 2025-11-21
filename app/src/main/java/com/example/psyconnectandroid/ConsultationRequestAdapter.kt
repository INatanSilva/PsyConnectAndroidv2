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
 * Adapter para lista de solicitações de consulta
 */
class ConsultationRequestAdapter(
    private val requests: List<ConsultationRequest>,
    private val onAcceptClick: (ConsultationRequest) -> Unit,
    private val onRejectClick: (ConsultationRequest) -> Unit,
    private val onCallClick: (ConsultationRequest) -> Unit,
    private val onChatClick: (ConsultationRequest) -> Unit,
    private val onNotesClick: (ConsultationRequest) -> Unit
) : RecyclerView.Adapter<ConsultationRequestAdapter.RequestViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RequestViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_consultation_request, parent, false)
        return RequestViewHolder(view)
    }

    override fun onBindViewHolder(holder: RequestViewHolder, position: Int) {
        holder.bind(requests[position])
    }

    override fun getItemCount(): Int = requests.size

    inner class RequestViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvPatientName: TextView = itemView.findViewById(R.id.tvPatientName)
        private val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        private val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        private val tvTime: TextView = itemView.findViewById(R.id.tvTime)
        private val btnAccept: Button = itemView.findViewById(R.id.btnAccept)
        private val btnReject: Button = itemView.findViewById(R.id.btnReject)
        private val btnCall: Button = itemView.findViewById(R.id.btnCall)
        private val btnChat: Button = itemView.findViewById(R.id.btnChat)
        private val btnNotes: Button = itemView.findViewById(R.id.btnNotes)

        fun bind(request: ConsultationRequest) {
            tvPatientName.text = request.patientName
            tvStatus.text = request.status
            
            // Formatar data e horário
            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            
            val date = request.date?.toDate() ?: request.scheduledStartTime?.toDate()
            if (date != null) {
                tvDate.text = dateFormat.format(date)
                tvTime.text = timeFormat.format(date)
            } else {
                tvDate.text = "Não agendado"
                tvTime.text = ""
            }
            
            // Mostrar/ocultar botões baseado no status
            if (request.isPending) {
                btnAccept.visibility = View.VISIBLE
                btnReject.visibility = View.VISIBLE
                btnCall.visibility = View.GONE
                btnChat.visibility = View.GONE
                btnNotes.visibility = View.GONE
            } else if (request.isAccepted) {
                btnAccept.visibility = View.GONE
                btnReject.visibility = View.GONE
                btnCall.visibility = View.VISIBLE
                btnChat.visibility = View.VISIBLE
                btnNotes.visibility = View.VISIBLE
            } else {
                btnAccept.visibility = View.GONE
                btnReject.visibility = View.GONE
                btnCall.visibility = View.GONE
                btnChat.visibility = View.GONE
                btnNotes.visibility = View.GONE
            }
            
            // Click listeners
            btnAccept.setOnClickListener {
                onAcceptClick(request)
            }
            
            btnReject.setOnClickListener {
                onRejectClick(request)
            }
            
            btnCall.setOnClickListener {
                onCallClick(request)
            }
            
            btnChat.setOnClickListener {
                onChatClick(request)
            }
            
            btnNotes.setOnClickListener {
                onNotesClick(request)
            }
        }
    }
}


