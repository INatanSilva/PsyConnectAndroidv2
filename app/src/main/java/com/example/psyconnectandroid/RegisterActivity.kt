package com.example.psyconnectandroid

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.Timestamp
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
    private lateinit var psychologistCardLayout: LinearLayout
    private lateinit var countrySpinner: Spinner
    private lateinit var cardNumberEditText: EditText

    private var selectedUserType: UserType = UserType.PATIENT

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        initializeViews()
        setupClickListeners()
        setInitialUserType()
        setupCountrySpinner()
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
        psychologistCardLayout = findViewById(R.id.llPsychologistCard)
        countrySpinner = findViewById(R.id.spinnerCountry)
        cardNumberEditText = findViewById(R.id.etCardNumber)
    }

    private fun setupCountrySpinner() {
        val countries = arrayOf("Portugal", "Brasil", "Angola")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, countries)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        countrySpinner.adapter = adapter
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
                psychologistCardLayout.visibility = View.GONE
            }
            UserType.PSYCHOLOGIST -> {
                patientOption.background = getDrawable(R.drawable.bg_segmented_unselected)
                psychologistOption.background = getDrawable(R.drawable.bg_segmented_selected)
                psychologistCardLayout.visibility = View.VISIBLE
            }
        }
    }

    private fun registerUser() {
        val fullName = fullNameEditText.text.toString().trim()
        val email = emailEditText.text.toString().trim()
        val password = passwordEditText.text.toString()
        val confirmPassword = confirmPasswordEditText.text.toString()
        val country = countrySpinner.selectedItem.toString()
        val cardNumber = cardNumberEditText.text.toString().trim()

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

        if (selectedUserType == UserType.PSYCHOLOGIST && cardNumber.isEmpty()) {
            Toast.makeText(this, "Por favor, insira o número da carteira profissional", Toast.LENGTH_SHORT).show()
            return
        }

        // Create Firebase user
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val userId = auth.currentUser?.uid
                    if (userId == null) {
                        Toast.makeText(this, "Erro ao recuperar usuário recém-criado.", Toast.LENGTH_SHORT).show()
                        return@addOnCompleteListener
                    }

                    val user = User(
                        fullName = fullName,
                        email = email,
                        userType = selectedUserType,
                        psychologistCardCountry = if (selectedUserType == UserType.PSYCHOLOGIST) country else null,
                        psychologistCardNumber = if (selectedUserType == UserType.PSYCHOLOGIST) cardNumber else null
                    )

                    saveUserProfile(userId, user)
                } else {
                    Toast.makeText(this, "Erro ao criar conta: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun saveUserProfile(userId: String, user: User) {
        val profileData = user.toMap().toMutableMap()
        profileData["name"] = user.fullName
        profileData["authUid"] = userId
        profileData["createdAt"] = Timestamp.now()

        val roleCollection = if (user.userType == UserType.PSYCHOLOGIST) "doutores" else "pacientes"

        val batch = firestore.batch()
        val mainDoc = firestore.collection("users").document(userId)
        val roleDoc = firestore.collection(roleCollection).document(userId)

        batch.set(mainDoc, profileData)
        batch.set(roleDoc, profileData)

        batch.commit()
            .addOnSuccessListener {
                android.util.Log.d("RegisterActivity", "User data saved successfully to Firestore collections")
                Toast.makeText(this, "Conta criada com sucesso!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                android.util.Log.e("RegisterActivity", "Error saving user data", e)
                Toast.makeText(this, "Erro ao salvar dados: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}