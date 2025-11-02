package com.example.psyconnectandroid

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class DoctorActivity : AppCompatActivity() {
    
    private lateinit var tvWelcome: TextView
    private lateinit var tvDoctorEmail: TextView
    private lateinit var btnLogout: Button
    
    private val auth = FirebaseAuth.getInstance()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_doctor)
        
        initializeViews()
        setupClickListeners()
        loadUserData()
    }
    
    private fun initializeViews() {
        tvWelcome = findViewById(R.id.tvWelcome)
        tvDoctorEmail = findViewById(R.id.tvDoctorEmail)
        btnLogout = findViewById(R.id.btnLogout)
    }
    
    private fun setupClickListeners() {
        btnLogout.setOnClickListener {
            logout()
        }
    }
    
    private fun loadUserData() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            tvWelcome.text = "Bem-vindo, Dr(a)!"
            tvDoctorEmail.text = currentUser.email ?: ""
        }
    }
    
    private fun logout() {
        auth.signOut()
        Toast.makeText(this, "Logout realizado", Toast.LENGTH_SHORT).show()
        
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}

