package com.example.psyconnectandroid

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import java.text.SimpleDateFormat
import java.util.Locale

class PatientNoteAdapter(
    private val notes: List<PatientNote>,
    private val onNoteClick: (PatientNote) -> Unit
) : RecyclerView.Adapter<PatientNoteAdapter.NoteViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_patient_note, parent, false)
        return NoteViewHolder(view)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        holder.bind(notes[position])
    }

    override fun getItemCount(): Int = notes.size

    inner class NoteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivDoctorPhoto: ImageView = itemView.findViewById(R.id.ivDoctorPhoto)
        private val tvUpdatedAt: TextView = itemView.findViewById(R.id.tvUpdatedAt)
        private val tvTitle: TextView = itemView.findViewById(R.id.tvNoteTitle)
        private val ivArrow: ImageView = itemView.findViewById(R.id.ivArrow)

        fun bind(note: PatientNote) {
            // Foto do doutor
            if (note.doctorPhotoUrl.isNotEmpty()) {
                Glide.with(itemView.context)
                    .load(note.doctorPhotoUrl)
                    .placeholder(R.drawable.ic_person)
                    .error(R.drawable.ic_person)
                    .circleCrop()
                    .into(ivDoctorPhoto)
            } else {
                ivDoctorPhoto.setImageResource(R.drawable.ic_person)
            }

            // Data de atualização: "Atualizado em 3 Nov 2025 at 21:09" (formato iOS)
            note.updatedAt?.toDate()?.let { date ->
                val dateFormat = SimpleDateFormat("d MMM yyyy", Locale.ENGLISH)
                val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                val dateStr = dateFormat.format(date)
                val timeStr = timeFormat.format(date)
                tvUpdatedAt.text = "Atualizado em $dateStr at $timeStr"
            } ?: run {
                tvUpdatedAt.text = ""
            }

            // Preview: therapeuticGoals ou feelingsAndSymptoms (igual ao iOS)
            val preview = note.getPreview()
            val title = if (preview.isNotEmpty()) {
                // Limitar a 50 caracteres
                if (preview.length > 50) {
                    preview.take(47) + "..."
                } else {
                    preview
                }
            } else {
                "Sem conteúdo"
            }
            tvTitle.text = title

            // Click listener
            itemView.setOnClickListener {
                onNoteClick(note)
            }
        }
    }
}

