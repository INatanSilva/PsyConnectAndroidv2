package com.example.psyconnectandroid

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class ChatActivity : AppCompatActivity() {
    
    private lateinit var recyclerViewMessages: RecyclerView
    private lateinit var etMessage: EditText
    private lateinit var ivSend: ImageView
    private lateinit var ivBack: ImageView
    private lateinit var tvChatName: TextView
    private lateinit var ivChatPhoto: ImageView
    
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    
    private val messages = mutableListOf<Message>()
    private var chatRoomId: String? = null
    private var patientId: String? = null
    private var doctorId: String? = null
    private var patientName: String? = null
    private var doctorName: String? = null
    private var patientPhotoUrl: String? = null
    private var doctorPhotoUrl: String? = null
    private var currentUserId: String? = null
    private var userType: UserType? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)
        
        initializeViews()
        setupClickListeners()
        loadIntentData()
        loadUserData()
    }
    
    override fun onResume() {
        super.onResume()
        if (chatRoomId != null && patientId != null && doctorId != null) {
            loadMessages()
            markMessagesAsRead()
        }
    }
    
    private fun initializeViews() {
        recyclerViewMessages = findViewById(R.id.recyclerViewMessages)
        etMessage = findViewById(R.id.etMessage)
        ivSend = findViewById(R.id.ivSend)
        ivBack = findViewById(R.id.ivBack)
        tvChatName = findViewById(R.id.tvChatName)
        ivChatPhoto = findViewById(R.id.ivChatPhoto)
        
        recyclerViewMessages.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
    }
    
    private fun setupClickListeners() {
        ivBack.setOnClickListener {
            finish()
        }
        
        ivSend.setOnClickListener {
            sendMessage()
        }
        
        etMessage.setOnEditorActionListener { _, _, _ ->
            sendMessage()
            true
        }
    }
    
    private fun loadIntentData() {
        chatRoomId = intent.getStringExtra("CHAT_ROOM_ID")
        patientId = intent.getStringExtra("PATIENT_ID")
        doctorId = intent.getStringExtra("DOCTOR_ID")
        patientName = intent.getStringExtra("PATIENT_NAME")
        doctorName = intent.getStringExtra("DOCTOR_NAME")
        patientPhotoUrl = intent.getStringExtra("PATIENT_PHOTO_URL")
        doctorPhotoUrl = intent.getStringExtra("DOCTOR_PHOTO_URL")
        
        // Se não tiver chatRoomId mas tiver patientId e doctorId, criar um ordenado
        if (chatRoomId.isNullOrEmpty() && !patientId.isNullOrEmpty() && !doctorId.isNullOrEmpty()) {
            chatRoomId = ChatHelper.generateChatId(patientId!!, doctorId!!)
        }
        
        // Set chat header
        val currentUser = auth.currentUser
        currentUserId = currentUser?.uid
        
        if (currentUserId == patientId) {
            // Current user is patient, show doctor info
            tvChatName.text = doctorName ?: "Doutor"
            if (!doctorPhotoUrl.isNullOrEmpty()) {
                Glide.with(this)
                    .load(doctorPhotoUrl)
                    .placeholder(R.drawable.ic_person)
                    .error(R.drawable.ic_person)
                    .circleCrop()
                    .into(ivChatPhoto)
            }
        } else {
            // Current user is doctor, show patient info
            tvChatName.text = patientName ?: "Paciente"
            if (!patientPhotoUrl.isNullOrEmpty()) {
                Glide.with(this)
                    .load(patientPhotoUrl)
                    .placeholder(R.drawable.ic_person)
                    .error(R.drawable.ic_person)
                    .circleCrop()
                    .into(ivChatPhoto)
            }
        }
        
        // Carregar mensagens após carregar os dados do intent
        if (!chatRoomId.isNullOrEmpty() && !patientId.isNullOrEmpty() && !doctorId.isNullOrEmpty()) {
            loadMessages()
        }
    }
    
    private fun loadUserData() {
        val currentUser = auth.currentUser ?: return
        val userId = currentUser.uid
        
        // Determine user type
        firestore.collection("pacientes").document(userId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    userType = UserType.PATIENT
                } else {
                    firestore.collection("doutores").document(userId).get()
                        .addOnSuccessListener { doc ->
                            if (doc.exists()) {
                                userType = UserType.PSYCHOLOGIST
                            }
                        }
                }
            }
    }
    
    private fun loadMessages() {
        val chatRoomId = this.chatRoomId
        val patientId = this.patientId
        val doctorId = this.doctorId
        
        if (chatRoomId == null || patientId == null || doctorId == null) {
            android.util.Log.e("ChatActivity", "Missing required data: chatRoomId=$chatRoomId, patientId=$patientId, doctorId=$doctorId")
            return
        }
        
        // Verificar se o chat existe, se não, criar
        firestore.collection("chats")
            .document(chatRoomId)
            .get()
            .addOnSuccessListener { document ->
                if (!document.exists()) {
                    // Criar o chat se não existir
                    createChatRoomIfNeeded()
                } else {
                    // Chat existe, carregar mensagens
                    loadMessagesFromFirestore(chatRoomId)
                }
            }
            .addOnFailureListener { e ->
                android.util.Log.e("ChatActivity", "Error checking chat room", e)
                // Tentar carregar mensagens mesmo assim
                loadMessagesFromFirestore(chatRoomId)
            }
    }
    
    private fun createChatRoomIfNeeded() {
        val patientId = this.patientId ?: return
        val doctorId = this.doctorId ?: return
        val patientName = this.patientName ?: "Paciente"
        val doctorName = this.doctorName ?: "Doutor"
        val patientPhotoUrl = this.patientPhotoUrl ?: ""
        val doctorPhotoUrl = this.doctorPhotoUrl ?: ""
        
        // Usar o chatRoomId do intent se existir, senão criar um novo
        val chatRoomId = this.chatRoomId ?: "${patientId}_${doctorId}"
        
        val chatRoom = ChatRoom(
            id = chatRoomId,
            patientId = patientId,
            doctorId = doctorId,
            patientName = patientName,
            doctorName = doctorName,
            patientPhotoUrl = patientPhotoUrl,
            doctorPhotoUrl = doctorPhotoUrl,
            lastMessage = "",
            lastMessageTimestamp = null,
            unreadCount = 0
        )
        
        firestore.collection("chats")
            .document(chatRoomId)
            .set(ChatRoom.toMap(chatRoom))
            .addOnSuccessListener {
                this.chatRoomId = chatRoomId
                loadMessagesFromFirestore(chatRoomId)
            }
            .addOnFailureListener { e ->
                android.util.Log.e("ChatActivity", "Error creating chat room", e)
                // Mesmo com erro, tentar carregar mensagens
                loadMessagesFromFirestore(chatRoomId)
            }
    }
    
    private fun loadMessagesFromFirestore(chatRoomId: String) {
        firestore.collection("chats")
            .document(chatRoomId)
            .collection("messages")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    android.util.Log.e("ChatActivity", "Error loading messages", e)
                    // Se o erro for por falta de índice, tentar sem orderBy
                    if (e.message?.contains("index") == true) {
                        firestore.collection("chats")
                            .document(chatRoomId)
                            .collection("messages")
                            .addSnapshotListener { snap, err ->
                                if (err == null) {
                                    messages.clear()
                                    snap?.documents?.forEach { document ->
                                        try {
                                            val message = Message.fromMap(document.data ?: emptyMap(), document.id)
                                            messages.add(message)
                                        } catch (ex: Exception) {
                                            android.util.Log.e("ChatActivity", "Error parsing message", ex)
                                        }
                                    }
                                    messages.sortBy { it.createdAt.toDate().time }
                                    updateMessagesUI()
                                }
                            }
                    }
                    return@addSnapshotListener
                }
                
                messages.clear()
                snapshot?.documents?.forEach { document ->
                    try {
                        val message = Message.fromMap(document.data ?: emptyMap(), document.id)
                        messages.add(message)
                    } catch (ex: Exception) {
                        android.util.Log.e("ChatActivity", "Error parsing message", ex)
                    }
                }
                
                updateMessagesUI()
            }
    }
    
    private fun updateMessagesUI() {
        recyclerViewMessages.adapter = MessageAdapter(messages, currentUserId ?: "")
        recyclerViewMessages.scrollToPosition(messages.size - 1)
    }
    
    private fun sendMessage() {
        val text = etMessage.text.toString().trim()
        if (text.isEmpty()) return
        
        val chatRoomId = this.chatRoomId
        val currentUserId = this.currentUserId
        
        if (chatRoomId == null || currentUserId == null) {
            return
        }
        
        val message = Message(
            senderId = currentUserId,
            text = text,
            createdAt = Timestamp.now()
        )
        
        // Add message to Firestore
        firestore.collection("chats")
            .document(chatRoomId)
            .collection("messages")
            .add(Message.toMap(message))
            .addOnSuccessListener { documentRef ->
                // Update chat room with last message (compatível com iOS e Android)
                val chatRoomRef = firestore.collection("chats").document(chatRoomId)
                val now = Timestamp.now()
                chatRoomRef.update(
                    mapOf(
                        "lastMessage" to text,
                        "lastMessageAt" to now,  // Formato iOS
                        "lastMessageTimestamp" to now,  // Formato Android
                        "unreadCount" to com.google.firebase.firestore.FieldValue.increment(1)
                    )
                )
                
                etMessage.setText("")
            }
            .addOnFailureListener { e ->
                android.util.Log.e("ChatActivity", "Error sending message", e)
            }
    }
    
    private fun markMessagesAsRead() {
        // iOS não usa isRead/unread tracking nas mensagens individuais
        // Apenas resetar o unreadCount do chat se necessário
        val chatRoomId = this.chatRoomId
        if (chatRoomId == null) return
        
        firestore.collection("chats")
            .document(chatRoomId)
            .update("unreadCount", 0)
            .addOnFailureListener { e ->
                android.util.Log.e("ChatActivity", "Error resetting unread count", e)
            }
    }
}

