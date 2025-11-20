package com.example.psyconnectandroid

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class IncomingCallActivity : AppCompatActivity() {
    
    private lateinit var tvCallerName: TextView
    private lateinit var tvCallType: TextView
    private lateinit var btnAccept: Button
    private lateinit var btnReject: Button
    
    private var callId: String? = null
    private var callerId: String? = null
    private var patientName: String? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_incoming_call)
        
        callId = intent.getStringExtra("CALL_ID")
        callerId = intent.getStringExtra("CALLER_ID")
        patientName = intent.getStringExtra("PATIENT_NAME")
        
        if (callId == null) {
            finish()
            return
        }
        
        initializeViews()
        setupListeners()
        displayCallInfo()
    }
    
    private fun initializeViews() {
        tvCallerName = findViewById(R.id.tvCallerName)
        tvCallType = findViewById(R.id.tvCallType)
        btnAccept = findViewById(R.id.btnAccept)
        btnReject = findViewById(R.id.btnReject)
    }
    
    private fun setupListeners() {
        btnAccept.setOnClickListener {
            acceptCall()
        }
        
        btnReject.setOnClickListener {
            rejectCall()
        }
    }
    
    private fun displayCallInfo() {
        tvCallerName.text = patientName ?: "Chamada de vídeo"
        tvCallType.text = "Chamada de vídeo"
    }
    
    private fun acceptCall() {
        callId?.let { id ->
            CallService.answerCall(
                callId = id,
                onSuccess = {
                    // Abrir CallActivity
                    val intent = Intent(this, CallActivity::class.java).apply {
                        putExtra("CALL_ID", id)
                        putExtra("IS_INITIATOR", false)
                        putExtra("CALLER_ID", callerId)
                        putExtra("PATIENT_NAME", patientName)
                    }
                    startActivity(intent)
                    finish()
                },
                onError = { e ->
                    android.util.Log.e("IncomingCallActivity", "Erro ao aceitar chamada", e)
                    finish()
                }
            )
        }
    }
    
    private fun rejectCall() {
        callId?.let { id ->
            CallService.rejectCall(
                callId = id,
                onSuccess = {
                    finish()
                },
                onError = { e ->
                    android.util.Log.e("IncomingCallActivity", "Erro ao rejeitar chamada", e)
                    finish()
                }
            )
        }
    }
    
    override fun onBackPressed() {
        // Não permitir voltar, apenas rejeitar
        rejectCall()
    }
}

