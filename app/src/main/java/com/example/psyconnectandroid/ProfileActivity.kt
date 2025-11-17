package com.example.psyconnectandroid

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageView
    private lateinit var ivProfilePhoto: ImageView
    private lateinit var ivCameraIcon: ImageView
    private lateinit var tvProfileName: TextView
    private lateinit var tvProfileEmail: TextView
    private lateinit var btnEditProfile: android.view.View
    private lateinit var btnNotifications: android.view.View
    private lateinit var btnPrivacy: android.view.View
    private lateinit var btnLogout: android.view.View

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        initializeViews()
        setupListeners()
        loadUserProfile()
    }

    private fun initializeViews() {
        btnBack = findViewById(R.id.btnBack)
        ivProfilePhoto = findViewById(R.id.ivProfilePhoto)
        ivCameraIcon = findViewById(R.id.ivCameraIcon)
        tvProfileName = findViewById(R.id.tvProfileName)
        tvProfileEmail = findViewById(R.id.tvProfileEmail)
        btnEditProfile = findViewById(R.id.btnEditProfile)
        btnNotifications = findViewById(R.id.btnNotifications)
        btnPrivacy = findViewById(R.id.btnPrivacy)
        btnLogout = findViewById(R.id.btnLogout)
    }

    private fun setupListeners() {
        btnBack.setOnClickListener {
            finish()
        }

        ivCameraIcon.setOnClickListener {
            // TODO: Implementar seleção de foto
            android.widget.Toast.makeText(this, "Funcionalidade de editar foto em breve", android.widget.Toast.LENGTH_SHORT).show()
        }

        btnEditProfile.setOnClickListener {
            // TODO: Implementar edição de perfil
            android.widget.Toast.makeText(this, "Funcionalidade de editar perfil em breve", android.widget.Toast.LENGTH_SHORT).show()
        }

        btnNotifications.setOnClickListener {
            // TODO: Implementar configurações de notificações
            android.widget.Toast.makeText(this, "Configurações de notificações em breve", android.widget.Toast.LENGTH_SHORT).show()
        }

        btnPrivacy.setOnClickListener {
            // TODO: Implementar configurações de privacidade
            android.widget.Toast.makeText(this, "Configurações de privacidade em breve", android.widget.Toast.LENGTH_SHORT).show()
        }

        btnLogout.setOnClickListener {
            logout()
        }
    }

    private fun loadUserProfile() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            tvProfileEmail.text = currentUser.email
            
            // Tentar carregar do cache primeiro
            val cachedPhoto = PhotoCache.get(currentUser.uid)
            if (cachedPhoto != null && cachedPhoto.isNotEmpty()) {
                loadProfilePhoto(cachedPhoto)
            }

            // Buscar dados do Firestore
            firestore.collection("pacientes").document(currentUser.uid).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val name = document.getString("name") 
                            ?: document.getString("fullName")
                            ?: document.getString("displayName")
                            ?: currentUser.displayName
                            ?: "Usuário"
                        
                        tvProfileName.text = name

                        val photoUrl = document.getString("photoUrl")
                            ?: document.getString("profileImageUrl")
                            ?: document.getString("profileImageURL")
                            ?: ""

                        if (photoUrl.isNotEmpty()) {
                            PhotoCache.put(currentUser.uid, photoUrl)
                            loadProfilePhoto(photoUrl)
                        } else {
                            loadProfilePhoto(null)
                        }
                    } else {
                        tvProfileName.text = currentUser.displayName ?: "Usuário"
                    }
                }
                .addOnFailureListener {
                    tvProfileName.text = currentUser.displayName ?: "Usuário"
                }
        }
    }

    private fun loadProfilePhoto(photoUrl: String?) {
        if (photoUrl != null && photoUrl.isNotEmpty()) {
            Glide.with(this)
                .load(photoUrl)
                .placeholder(R.drawable.ic_person)
                .error(R.drawable.ic_person)
                .circleCrop()
                .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.ALL)
                .into(ivProfilePhoto)
        } else {
            Glide.with(this)
                .load(R.drawable.ic_person)
                .circleCrop()
                .into(ivProfilePhoto)
        }
    }

    private fun logout() {
        // Limpar caches
        PhotoCache.clear()
        DoctorCache.clear()
        PatientCache.clear()
        CacheManager.clear()
        
        auth.signOut()

        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}

