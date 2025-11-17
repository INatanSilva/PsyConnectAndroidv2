package com.example.psyconnectandroid

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
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
            // Animação de clique no botão
            it.animate()
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(100)
                .withEndAction {
                    it.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(100)
                        .start()
                }
                .start()
            
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
        
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Por favor, preencha todos os campos.", Toast.LENGTH_SHORT).show()
            // Shake animation para erro
            loginButton.animate()
                .translationX(-10f)
                .setDuration(50)
                .withEndAction {
                    loginButton.animate().translationX(10f).setDuration(50)
                        .withEndAction {
                            loginButton.animate().translationX(-10f).setDuration(50)
                                .withEndAction {
                                    loginButton.animate().translationX(10f).setDuration(50)
                                        .withEndAction {
                                            loginButton.animate().translationX(0f).setDuration(50).start()
                                        }.start()
                                }.start()
                        }.start()
                }.start()
            return
        }
        
        // Desabilitar botão e mostrar loading
        loginButton.isEnabled = false
        val originalText = loginButton.text
        loginButton.text = "Entrando..."
        
        // Animação de loading (pulse)
        loginButton.animate()
            .alpha(0.7f)
            .setDuration(500)
            .withEndAction {
                loginButton.animate()
                    .alpha(1f)
                    .setDuration(500)
                    .start()
            }
            .start()
        
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                loginButton.isEnabled = true
                loginButton.text = originalText
                loginButton.alpha = 1f
                
                if (task.isSuccessful) {
                    // Animação de sucesso
                    loginButton.animate()
                        .scaleX(1.1f)
                        .scaleY(1.1f)
                        .alpha(0f)
                        .setDuration(300)
                        .withEndAction {
                            navigateToUserHome()
                        }
                        .start()
                } else {
                    // Animação de erro (shake)
                    Toast.makeText(this, "Erro no login: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    loginButton.animate()
                        .translationX(-20f)
                        .setDuration(100)
                        .withEndAction {
                            loginButton.animate().translationX(20f).setDuration(100)
                                .withEndAction {
                                    loginButton.animate().translationX(0f).setDuration(100).start()
                                }.start()
                        }.start()
                }
            }
    }
    
    private fun navigateToUserHome() {
        val userId = auth.currentUser?.uid ?: return

        determineUserType(
            userId = userId,
            onSuccess = { userType ->
                navigateToUserScreen(userType)
            },
            onFailure = {
                Toast.makeText(this, "Erro ao buscar dados do usuário.", Toast.LENGTH_SHORT).show()
                navigateToDefault()
            }
        )
    }

    private fun determineUserType(
        userId: String,
        onSuccess: (UserType) -> Unit,
        onFailure: () -> Unit
    ) {
        checkUserInCollection("pacientes", userId) { isPatient ->
            if (isPatient) {
                onSuccess(UserType.PATIENT)
            } else {
                checkUserInCollection("doutores", userId) { isDoctor ->
                    if (isDoctor) {
                        onSuccess(UserType.PSYCHOLOGIST)
                    } else {
                        // Fallback to legacy "users" collection if new structure not found
                        firestore.collection("users").document(userId).get()
                            .addOnSuccessListener { document ->
                                if (document.exists()) {
                                    val userTypeString = document.getString("userType")
                                    val userType = try {
                                        UserType.valueOf(userTypeString ?: "PATIENT")
                                    } catch (e: IllegalArgumentException) {
                                        UserType.PATIENT
                                    }
                                    onSuccess(userType)
                                } else {
                                    checkLegacyCollections(userId, onSuccess, onFailure)
                                }
                            }
                            .addOnFailureListener {
                                checkLegacyCollections(userId, onSuccess, onFailure)
                            }
                    }
                }
            }
        }
    }
    
    private fun checkUserInCollection(
        collection: String,
        userId: String,
        onComplete: (Boolean) -> Unit
    ) {
        val collectionRef = firestore.collection(collection)
        collectionRef.document(userId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    onComplete(true)
                } else {
                    // Try to locate the user by a potential UID field (authUid or userId)
                    findUserByField(
                        collectionRef = collectionRef,
                        userId = userId,
                        fields = listOf("authUid", "userId", "uid"),
                        onComplete = { found -> onComplete(found) }
                    )
                }
            }
            .addOnFailureListener {
                onComplete(false)
            }
    }

    private fun findUserByField(
        collectionRef: com.google.firebase.firestore.CollectionReference,
        userId: String,
        fields: List<String>,
        onComplete: (Boolean) -> Unit,
        currentIndex: Int = 0
    ) {
        if (currentIndex >= fields.size) {
            onComplete(false)
            return
        }

        val field = fields[currentIndex]
        collectionRef.whereEqualTo(field, userId).limit(1).get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    onComplete(true)
                } else {
                    findUserByField(
                        collectionRef = collectionRef,
                        userId = userId,
                        fields = fields,
                        onComplete = onComplete,
                        currentIndex = currentIndex + 1
                    )
                }
            }
            .addOnFailureListener {
                findUserByField(
                    collectionRef = collectionRef,
                    userId = userId,
                    fields = fields,
                    onComplete = onComplete,
                    currentIndex = currentIndex + 1
                )
            }
    }

    private fun checkLegacyCollections(
        userId: String,
        onSuccess: (UserType) -> Unit,
        onFailure: () -> Unit
    ) {
        firestore.collection("doutores").document(userId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    onSuccess(UserType.PSYCHOLOGIST)
                } else {
                    firestore.collection("pacientes").document(userId).get()
                        .addOnSuccessListener { patientDocument ->
                            if (patientDocument.exists()) {
                                onSuccess(UserType.PATIENT)
                            } else {
                                onFailure()
                            }
                        }
                        .addOnFailureListener {
                            onFailure()
                        }
                }
            }
            .addOnFailureListener {
                onFailure()
            }
    }
    
    private fun navigateToUserScreen(userType: UserType) {
        val intent = when (userType) {
            UserType.PATIENT -> Intent(this, PatientActivity::class.java)
            UserType.PSYCHOLOGIST -> Intent(this, DoctorDashboardActivity::class.java)
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
