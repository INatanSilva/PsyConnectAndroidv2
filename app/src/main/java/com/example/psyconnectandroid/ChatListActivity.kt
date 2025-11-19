package com.example.psyconnectandroid

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ChatListActivity : AppCompatActivity() {
    
    private lateinit var recyclerViewChatRooms: RecyclerView
    private lateinit var tvEmptyState: TextView
    private lateinit var ivBack: ImageView
    
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    
    private val chatRooms = mutableListOf<ChatRoom>()
    private var currentUserId: String? = null
    private var userType: UserType? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_list)
        
        initializeViews()
        setupClickListeners()
        loadUserData()
    }
    
    override fun onResume() {
        super.onResume()
        loadChatRooms()
    }
    
    private fun initializeViews() {
        recyclerViewChatRooms = findViewById(R.id.recyclerViewChatRooms)
        tvEmptyState = findViewById(R.id.tvEmptyState)
        ivBack = findViewById(R.id.ivBack)
        
        recyclerViewChatRooms.layoutManager = LinearLayoutManager(this)
    }
    
    private fun setupClickListeners() {
        ivBack.setOnClickListener {
            finish()
        }
    }
    
    private fun loadUserData() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            return
        }
        
        currentUserId = currentUser.uid
        determineUserType(currentUser.uid)
    }
    
    private fun determineUserType(userId: String) {
        // Check if user is patient
        firestore.collection("pacientes").document(userId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    userType = UserType.PATIENT
                    loadChatRooms()
                } else {
                    // Check if user is doctor
                    firestore.collection("doutores").document(userId).get()
                        .addOnSuccessListener { doc ->
                            if (doc.exists()) {
                                userType = UserType.PSYCHOLOGIST
                                loadChatRooms()
                            }
                        }
                }
            }
    }
    
    private fun loadChatRooms() {
        val userId = currentUserId ?: return
        val userType = this.userType ?: return
        
        android.util.Log.d("ChatListActivity", "Loading chat rooms for userId: $userId, userType: $userType")
        
        chatRooms.clear()
        
        val collectionRef = firestore.collection("chats")
        
        // Tentar duas queries: uma com participants.patientId/doctorId (formato iOS) 
        // e outra com patientId/doctorId direto (formato Android)
        val fieldPath = if (userType == UserType.PATIENT) {
            "participants.patientId"
        } else {
            "participants.doctorId"
        }
        
        // Primeira tentativa: buscar usando participants (formato iOS)
        collectionRef.whereEqualTo(fieldPath, userId)
            .get()
            .addOnSuccessListener { documents ->
                android.util.Log.d("ChatListActivity", "Found ${documents.size()} chats using participants field")
                
                if (documents.isEmpty) {
                    // Se não encontrou nada, tentar com campos diretos (formato Android)
                    val directField = if (userType == UserType.PATIENT) "patientId" else "doctorId"
                    collectionRef.whereEqualTo(directField, userId)
                        .get()
                        .addOnSuccessListener { docs ->
                            android.util.Log.d("ChatListActivity", "Found ${docs.size()} chats using direct field")
                            processChatDocuments(docs)
                        }
                        .addOnFailureListener { e ->
                            android.util.Log.e("ChatListActivity", "Error loading chat rooms (direct field)", e)
                            chatRooms.clear()
                            updateUI()
                        }
                } else {
                    processChatDocuments(documents)
                }
            }
            .addOnFailureListener { e ->
                android.util.Log.e("ChatListActivity", "Error loading chat rooms (participants)", e)
                // Fallback: tentar com campo direto
                val directField = if (userType == UserType.PATIENT) "patientId" else "doctorId"
                collectionRef.whereEqualTo(directField, userId)
                    .get()
                    .addOnSuccessListener { docs ->
                        android.util.Log.d("ChatListActivity", "Found ${docs.size()} chats using direct field (fallback)")
                        processChatDocuments(docs)
                    }
                    .addOnFailureListener { ex ->
                        android.util.Log.e("ChatListActivity", "Error loading chat rooms (fallback)", ex)
                        chatRooms.clear()
                        updateUI()
                    }
            }
    }
    
    private fun processChatDocuments(documents: com.google.firebase.firestore.QuerySnapshot) {
        chatRooms.clear()
        
        for (document in documents) {
            try {
                android.util.Log.d("ChatListActivity", "Processing chat document: ${document.id}")
                android.util.Log.d("ChatListActivity", "Chat data: ${document.data}")
                
                val chatRoom = ChatRoom.fromMap(document.data, document.id)
                chatRooms.add(chatRoom)
                
                android.util.Log.d("ChatListActivity", "Successfully parsed chat: patientId=${chatRoom.patientId}, doctorId=${chatRoom.doctorId}")
            } catch (e: Exception) {
                android.util.Log.e("ChatListActivity", "Error parsing chat room ${document.id}", e)
                e.printStackTrace()
            }
        }
        
        // Sort by last message timestamp
        chatRooms.sortByDescending { it.lastMessageTimestamp?.toDate()?.time ?: 0L }
        
        android.util.Log.d("ChatListActivity", "Total chats loaded: ${chatRooms.size}")
        
        updateUI()
    }
    
    private fun updateUI() {
        if (chatRooms.isEmpty()) {
            recyclerViewChatRooms.visibility = View.GONE
            tvEmptyState.visibility = View.VISIBLE
        } else {
            recyclerViewChatRooms.visibility = View.VISIBLE
            tvEmptyState.visibility = View.GONE
            
            recyclerViewChatRooms.adapter = ChatRoomAdapter(chatRooms, currentUserId ?: "") { chatRoom ->
                val intent = Intent(this, ChatActivity::class.java)
                intent.putExtra("CHAT_ROOM_ID", chatRoom.id)
                intent.putExtra("PATIENT_ID", chatRoom.patientId)
                intent.putExtra("DOCTOR_ID", chatRoom.doctorId)
                intent.putExtra("PATIENT_NAME", chatRoom.patientName)
                intent.putExtra("DOCTOR_NAME", chatRoom.doctorName)
                intent.putExtra("PATIENT_PHOTO_URL", chatRoom.patientPhotoUrl)
                intent.putExtra("DOCTOR_PHOTO_URL", chatRoom.doctorPhotoUrl)
                startActivity(intent)
            }
        }
    }
}
