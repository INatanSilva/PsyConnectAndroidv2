package com.example.psyconnectandroid

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.SurfaceView
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import io.agora.rtc2.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class CallActivity : AppCompatActivity() {
    
    private lateinit var tvCallerName: TextView
    private lateinit var tvCallStatus: TextView
    private lateinit var tvCallDuration: TextView
    private lateinit var svRemoteVideo: SurfaceView
    private lateinit var svLocalVideo: SurfaceView
    private lateinit var btnMute: ImageButton
    private lateinit var btnVideo: ImageButton
    private lateinit var btnSpeaker: ImageButton
    private lateinit var btnSwitchCamera: ImageButton
    private lateinit var btnEndCall: Button
    
    private var callId: String? = null
    private var isInitiator: Boolean = false
    private var callerId: String? = null
    private var patientName: String? = null
    
    private lateinit var agoraService: AgoraService
    private var isMuted = false
    private var isVideoEnabled = true
    private var isSpeakerEnabled = false
    
    private val handler = Handler(Looper.getMainLooper())
    private var callStartTime: Long = 0
    private val durationRunnable = object : Runnable {
        override fun run() {
            updateCallDuration()
            handler.postDelayed(this, 1000)
        }
    }
    
    private val requiredPermissions = arrayOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.CAMERA
    )
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_call)
        
        callId = intent.getStringExtra("CALL_ID")
        isInitiator = intent.getBooleanExtra("IS_INITIATOR", false)
        callerId = intent.getStringExtra("CALLER_ID")
        patientName = intent.getStringExtra("PATIENT_NAME")
        
        if (callId == null) {
            finish()
            return
        }
        
        agoraService = AgoraService(this)
        
        initializeViews()
        setupListeners()
        
        if (checkPermissions()) {
            startCall()
        } else {
            requestPermissions()
        }
    }
    
    private fun initializeViews() {
        tvCallerName = findViewById(R.id.tvCallerName)
        tvCallStatus = findViewById(R.id.tvCallStatus)
        tvCallDuration = findViewById(R.id.tvCallDuration)
        svRemoteVideo = findViewById(R.id.svRemoteVideo)
        svLocalVideo = findViewById(R.id.svLocalVideo)
        btnMute = findViewById(R.id.btnMute)
        btnVideo = findViewById(R.id.btnVideo)
        btnSpeaker = findViewById(R.id.btnSpeaker)
        btnSwitchCamera = findViewById(R.id.btnSwitchCamera)
        btnEndCall = findViewById(R.id.btnEndCall)
        
        tvCallerName.text = patientName ?: "Chamada de vídeo"
        tvCallStatus.text = if (isInitiator) "Conectando..." else "Chamada em andamento"
    }
    
    private fun setupListeners() {
        btnMute.setOnClickListener {
            toggleMute()
        }
        
        btnVideo.setOnClickListener {
            toggleVideo()
        }
        
        btnSpeaker.setOnClickListener {
            toggleSpeaker()
        }
        
        btnSwitchCamera.setOnClickListener {
            agoraService.switchCamera()
        }
        
        btnEndCall.setOnClickListener {
            endCall()
        }
        
        // Callbacks do Agora
        agoraService.onUserJoined = { uid ->
            runOnUiThread {
                tvCallStatus.text = "Conectado"
                agoraService.setupRemoteVideo(svRemoteVideo, uid)
            }
        }
        
        agoraService.onUserOffline = { uid ->
            runOnUiThread {
                tvCallStatus.text = "Usuário desconectado"
            }
        }
        
        agoraService.onJoinChannelSuccess = { channel, uid, elapsed ->
            runOnUiThread {
                tvCallStatus.text = "Conectado"
                callStartTime = System.currentTimeMillis()
                handler.post(durationRunnable)
            }
        }
        
        agoraService.onLeaveChannel = {
            runOnUiThread {
                finish()
            }
        }
    }
    
    private fun checkPermissions(): Boolean {
        return requiredPermissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    private fun requestPermissions() {
        ActivityCompat.requestPermissions(this, requiredPermissions, REQUEST_CODE_PERMISSIONS)
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                startCall()
            } else {
                android.widget.Toast.makeText(
                    this,
                    "Permissões necessárias para fazer chamadas",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }
    
    private fun startCall() {
        // Inicializar Agora Engine
        if (!agoraService.setupAgoraEngine()) {
            android.widget.Toast.makeText(
                this,
                "Erro ao inicializar serviço de vídeo",
                android.widget.Toast.LENGTH_SHORT
            ).show()
            finish()
            return
        }
        
        // Configurar vídeo local
        agoraService.setupLocalVideo(svLocalVideo)
        
        // Entrar no canal
        CoroutineScope(Dispatchers.Main).launch {
            val channelName = callId ?: return@launch
            val uid = (1000..99999).random()
            
            android.util.Log.d("CallActivity", "Tentando entrar no canal: $channelName com UID: $uid")
            
            // Verificar servidor de tokens
            val serverHealthy = TokenService.checkServerHealth()
            
            val token = if (serverHealthy) {
                TokenService.generateRTCToken(
                    channelName = channelName,
                    uid = uid.toString(),
                    role = "publisher",
                    expireTime = 3600
                )
            } else {
                null
            }
            
            android.util.Log.d("CallActivity", "Token gerado: ${token != null}")
            
            val success = agoraService.joinChannel(channelName, uid, token)
            if (!success) {
                android.util.Log.e("CallActivity", "Falha ao entrar no canal")
                runOnUiThread {
                    android.widget.Toast.makeText(
                        this@CallActivity,
                        "Erro ao conectar na chamada",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
    
    private fun toggleMute() {
        isMuted = !isMuted
        agoraService.muteAudio(isMuted)
        btnMute.setImageResource(
            if (isMuted) R.drawable.ic_mic_off else R.drawable.ic_mic
        )
    }
    
    private fun toggleVideo() {
        isVideoEnabled = !isVideoEnabled
        agoraService.muteVideo(!isVideoEnabled)
        btnVideo.setImageResource(
            if (isVideoEnabled) R.drawable.ic_videocam else R.drawable.ic_videocam_off
        )
        svLocalVideo.visibility = if (isVideoEnabled) View.VISIBLE else View.GONE
    }
    
    private fun toggleSpeaker() {
        isSpeakerEnabled = !isSpeakerEnabled
        // TODO: Implementar controle de speaker
        btnSpeaker.setImageResource(
            if (isSpeakerEnabled) R.drawable.ic_volume_up else R.drawable.ic_volume_down
        )
    }
    
    private fun updateCallDuration() {
        if (callStartTime > 0) {
            val elapsed = System.currentTimeMillis() - callStartTime
            val seconds = (elapsed / 1000).toInt()
            val hours = seconds / 3600
            val minutes = (seconds % 3600) / 60
            val secs = seconds % 60
            
            tvCallDuration.text = String.format("%02d:%02d:%02d", hours, minutes, secs)
        }
    }
    
    private fun endCall() {
        callId?.let { id ->
            CallService.endCall(
                callId = id,
                onSuccess = {
                    agoraService.leaveChannel()
                    finish()
                },
                onError = { e ->
                    android.util.Log.e("CallActivity", "Erro ao finalizar chamada", e)
                    agoraService.leaveChannel()
                    finish()
                }
            )
        } ?: run {
            agoraService.leaveChannel()
            finish()
        }
    }
    
    override fun onBackPressed() {
        // Não permitir voltar, apenas finalizar chamada
        endCall()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(durationRunnable)
        agoraService.release()
    }
    
    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 1001
    }
}

