package com.example.psyconnectandroid

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {
    
    private lateinit var fullNameEditText: EditText
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var confirmPasswordEditText: EditText
    private lateinit var patientOption: LinearLayout
    private lateinit var psychologistOption: LinearLayout
    private lateinit var createAccountButton: Button
    private lateinit var backButton: ImageButton
    
    private var selectedUserType: UserType = UserType.PATIENT
    
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)
        
        initializeViews()
        setupClickListeners()
        setInitialUserType()
    }
    
    private fun initializeViews() {
        fullNameEditText = findViewById(R.id.etFullName)
        emailEditText = findViewById(R.id.etEmail)
        passwordEditText = findViewById(R.id.etPassword)
        confirmPasswordEditText = findViewById(R.id.etConfirmPassword)
        patientOption = findViewById(R.id.llPatientOption)
        psychologistOption = findViewById(R.id.llPsychologistOption)
        createAccountButton = findViewById(R.id.btnCreateAccount)
        backButton = findViewById(R.id.btnBack)
    }
    
    private fun setupClickListeners() {
        patientOption.setOnClickListener {
            selectUserType(UserType.PATIENT)
        }
        
        psychologistOption.setOnClickListener {
            selectUserType(UserType.PSYCHOLOGIST)
        }
        
        createAccountButton.setOnClickListener {
            registerUser()
        }
        
        backButton.setOnClickListener {
            finish()
        }
    }
    
    private fun setInitialUserType() {
        selectUserType(UserType.PATIENT)
    }
    
    private fun selectUserType(type: UserType) {
        selectedUserType = type
        
        when (type) {
            UserType.PATIENT -> {
                patientOption.background = getDrawable(R.drawable.bg_segmented_selected)
                psychologistOption.background = getDrawable(R.drawable.bg_segmented_unselected)
            }
            UserType.PSYCHOLOGIST -> {
                patientOption.background = getDrawable(R.drawable.bg_segmented_unselected)
                psychologistOption.background = getDrawable(R.drawable.bg_segmented_selected)
            }
        }
    }
    
    private fun registerUser() {
        val fullName = fullNameEditText.text.toString().trim()
        val email = emailEditText.text.toString().trim()
        val password = passwordEditText.text.toString()
        val confirmPassword = confirmPasswordEditText.text.toString()
        
        // Validation
        if (fullName.isEmpty()) {
            Toast.makeText(this, "Por favor, insira seu nome completo", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (email.isEmpty()) {
            Toast.makeText(this, "Por favor, insira seu email", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (password.isEmpty()) {
            Toast.makeText(this, "Por favor, insira uma senha", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (password.length < 6) {
            Toast.makeText(this, "A senha deve ter pelo menos 6 caracteres", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (password != confirmPassword) {
            Toast.makeText(this, "As senhas não coincidem", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Create Firebase user
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Save user data to Firestore
                    android.util.Log.d("RegisterActivity", "Selected user type: $selectedUserType")
                    
                    val user = User(
                        fullName = fullName,
                        email = email,
                        userType = selectedUserType
                    )
                    
                    android.util.Log.d("RegisterActivity", "User object created: ${user.userType}")
                    android.util.Log.d("RegisterActivity", "User toMap: ${user.toMap()}")
                    
                    firestore.collection("users")
                        .document(auth.currentUser?.uid ?: "")
                        .set(user.toMap())
                        .addOnSuccessListener {
                            android.util.Log.d("RegisterActivity", "User data saved successfully to Firestore")
                            Toast.makeText(this, "Conta criada com sucesso!", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                        .addOnFailureListener { e ->
                            android.util.Log.e("RegisterActivity", "Error saving user data", e)
                            Toast.makeText(this, "Erro ao salvar dados: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Toast.makeText(this, "Erro ao criar conta: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }
}

