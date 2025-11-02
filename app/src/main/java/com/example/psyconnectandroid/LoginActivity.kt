package com.example.psyconnectandroid

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {
    
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: Button
    private lateinit var createAccountTextView: TextView
    
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        
        initializeViews()
        setupClickListeners()
    }
    
    private fun initializeViews() {
        emailEditText = findViewById(R.id.etEmail)
        passwordEditText = findViewById(R.id.etPassword)
        loginButton = findViewById(R.id.btnLogin)
        createAccountTextView = findViewById(R.id.tvCreateAccount)
    }
    
    private fun setupClickListeners() {
        loginButton.setOnClickListener {
            loginUser()
        }
        
        createAccountTextView.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }
    
    private fun loginUser() {
        val email = emailEditText.text.toString().trim()
        val password = passwordEditText.text.toString()
        
        // Validation
        if (email.isEmpty()) {
            Toast.makeText(this, "Por favor, insira seu email", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (password.isEmpty()) {
            Toast.makeText(this, "Por favor, insira sua senha", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Login with Firebase
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Login realizado com sucesso!", Toast.LENGTH_SHORT).show()
                    navigateToUserHome()
                } else {
                    Toast.makeText(this, "Erro no login: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }
    
    private fun navigateToUserHome() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // First, try to get user data from "users" collection
            firestore.collection("users")
                .document(currentUser.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val userData = document.data ?: emptyMap()
                        val userTypeString = userData["userType"] as? String ?: "PATIENT"
                        
                        // Debug log
                        android.util.Log.d("LoginActivity", "User data from Firestore: $userData")
                        android.util.Log.d("LoginActivity", "User type string: $userTypeString")
                        
                        val userType = try {
                            UserType.valueOf(userTypeString)
                        } catch (e: Exception) {
                            android.util.Log.e("LoginActivity", "Error parsing userType: $userTypeString", e)
                            UserType.PATIENT
                        }
                        
                        android.util.Log.d("LoginActivity", "Navigating to: $userType")
                        
                        // Navigate based on user type
                        navigateToUserScreen(userType)
                    } else {
                        // Document not found in "users", try checking "doutores" and "pacientes" collections
                        android.util.Log.w("LoginActivity", "Document does not exist in 'users', checking other collections")
                        checkLegacyCollections(currentUser.uid)
                    }
                }
                .addOnFailureListener { e ->
                    // Error reading user data, default to Patient
                    android.util.Log.e("LoginActivity", "Error reading user data", e)
                    navigateToDefault()
                }
        }
    }
    
    private fun checkLegacyCollections(userId: String) {
        // Check if user exists in "doutores" collection
        firestore.collection("doutores")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    android.util.Log.d("LoginActivity", "Found user in 'doutores' collection, navigating to DoctorActivity")
                    navigateToUserScreen(UserType.PSYCHOLOGIST)
                } else {
                    // If not in doutores, check pacientes
                    firestore.collection("pacientes")
                        .document(userId)
                        .get()
                        .addOnSuccessListener { pacDoc ->
                            if (pacDoc.exists()) {
                                android.util.Log.d("LoginActivity", "Found user in 'pacientes' collection, navigating to PatientActivity")
                                navigateToUserScreen(UserType.PATIENT)
                            } else {
                                android.util.Log.w("LoginActivity", "User not found in any collection, defaulting to Patient")
                                navigateToDefault()
                            }
                        }
                }
            }
    }
    
    private fun navigateToUserScreen(userType: UserType) {
        val intent = when (userType) {
            UserType.PATIENT -> Intent(this, PatientActivity::class.java)
            UserType.PSYCHOLOGIST -> Intent(this, DoctorActivity::class.java)
        }
        
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
    
    private fun navigateToDefault() {
        val intent = Intent(this, PatientActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}

