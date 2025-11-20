package com.example.psyconnectandroid

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatRoomAdapter(
    private val chatRooms: List<ChatRoom>,
    private val currentUserId: String,
    private val onChatRoomClick: (ChatRoom) -> Unit
) : RecyclerView.Adapter<ChatRoomAdapter.ChatRoomViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatRoomViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat_room, parent, false)
        return ChatRoomViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatRoomViewHolder, position: Int) {
        holder.bind(chatRooms[position])
    }

    override fun getItemCount(): Int = chatRooms.size

    inner class ChatRoomViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivPhoto: ImageView = itemView.findViewById(R.id.ivChatPhoto)
        private val tvName: TextView = itemView.findViewById(R.id.tvChatName)
        private val tvLastMessage: TextView = itemView.findViewById(R.id.tvLastMessage)
        private val tvTime: TextView = itemView.findViewById(R.id.tvChatTime)
        private val tvUnreadCount: TextView = itemView.findViewById(R.id.tvUnreadCount)
        private val viewUnreadBadge: View = itemView.findViewById(R.id.viewUnreadBadge)

        fun bind(chatRoom: ChatRoom) {
            // Determine if current user is patient or doctor
            val isPatient = currentUserId == chatRoom.patientId
            
            // Set name and photo based on user type
            if (isPatient) {
                tvName.text = chatRoom.doctorName
                if (chatRoom.doctorPhotoUrl.isNotEmpty()) {
                    Glide.with(itemView.context)
                        .load(chatRoom.doctorPhotoUrl)
                        .placeholder(R.drawable.ic_person)
                        .error(R.drawable.ic_person)
                        .circleCrop()
                        .into(ivPhoto)
                } else {
                    ivPhoto.setImageResource(R.drawable.ic_person)
                }
            } else {
                tvName.text = chatRoom.patientName
                if (chatRoom.patientPhotoUrl.isNotEmpty()) {
                    Glide.with(itemView.context)
                        .load(chatRoom.patientPhotoUrl)
                        .placeholder(R.drawable.ic_person)
                        .error(R.drawable.ic_person)
                        .circleCrop()
                        .into(ivPhoto)
                } else {
                    ivPhoto.setImageResource(R.drawable.ic_person)
                }
            }

            // Last message
            tvLastMessage.text = chatRoom.lastMessage.ifEmpty { "Nenhuma mensagem ainda" }

            // Time
            chatRoom.lastMessageTimestamp?.let { timestamp ->
                val date = timestamp.toDate()
                tvTime.text = formatTime(date)
            } ?: run {
                tvTime.text = ""
            }

            // Unread count
            if (chatRoom.unreadCount > 0) {
                viewUnreadBadge.visibility = View.VISIBLE
                tvUnreadCount.text = if (chatRoom.unreadCount > 99) "99+" else chatRoom.unreadCount.toString()
            } else {
                viewUnreadBadge.visibility = View.GONE
            }

            itemView.setOnClickListener {
                onChatRoomClick(chatRoom)
            }
        }

        private fun formatTime(date: Date): String {
            val now = Date()
            val diff = now.time - date.time
            val seconds = diff / 1000
            val minutes = seconds / 60
            val hours = minutes / 60
            val days = hours / 24

            return when {
                days > 7 -> SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(date)
                days > 0 -> "${days.toInt()}d atrás"
                hours > 0 -> "${hours.toInt()}h atrás"
                minutes > 0 -> "${minutes.toInt()}min atrás"
                else -> "Agora"
            }
        }
    }
}




