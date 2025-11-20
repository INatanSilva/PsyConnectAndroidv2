package com.example.psyconnectandroid

import android.content.Context
import android.util.Log
import android.view.SurfaceView
import io.agora.rtc2.*
import io.agora.rtc2.video.VideoCanvas
import io.agora.rtc2.video.VideoEncoderConfiguration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Serviço para gerenciar conexão com Agora.io
 */
class AgoraService(context: Context) {
    
    private val TAG = "AgoraService"
    private val APP_ID = "90d3896855554717b5d27b219eae0b01"
    
    // Armazenar applicationContext para evitar problemas com ciclo de vida
    private val appContext: Context = context.applicationContext
    
    private var agoraEngine: RtcEngine? = null
    private var isConnected = false
    private var remoteUid: Int = 0
    private var currentChannel: String? = null
    
    var onUserJoined: ((Int) -> Unit)? = null
    var onUserOffline: ((Int) -> Unit)? = null
    var onConnectionStateChanged: ((Int, Int) -> Unit)? = null
    var onJoinChannelSuccess: ((String, Int, Int) -> Unit)? = null
    var onLeaveChannel: (() -> Unit)? = null
    
    private val eventHandler = object : IRtcEngineEventHandler() {
        override fun onJoinChannelSuccess(channel: String, uid: Int, elapsed: Int) {
            Log.d(TAG, "✅ Entrou no canal: $channel com UID: $uid")
            isConnected = true
            currentChannel = channel
            onJoinChannelSuccess?.invoke(channel, uid, elapsed)
        }
        
        override fun onUserJoined(uid: Int, elapsed: Int) {
            Log.d(TAG, "👥 Usuário remoto entrou: $uid")
            remoteUid = uid
            onUserJoined?.invoke(uid)
        }
        
        override fun onUserOffline(uid: Int, reason: Int) {
            Log.d(TAG, "👤 Usuário remoto saiu: $uid, reason: $reason")
            if (uid == remoteUid) {
                remoteUid = 0
            }
            onUserOffline?.invoke(uid)
        }
        
        override fun onConnectionStateChanged(state: Int, reason: Int) {
            Log.d(TAG, "Connection state changed: $state, reason: $reason")
            onConnectionStateChanged?.invoke(state, reason)
        }
        
        override fun onLeaveChannel(stats: RtcStats) {
            Log.d(TAG, "Saiu do canal")
            isConnected = false
            currentChannel = null
            remoteUid = 0
            onLeaveChannel?.invoke()
        }
    }
    
    /**
     * Inicializa o Agora Engine
     */
    fun setupAgoraEngine(): Boolean {
        return try {
            // IMPORTANTE: O Agora SDK só permite uma instância do engine por aplicação
            // Se já existe uma instância, precisamos destruí-la primeiro
            if (agoraEngine != null) {
                Log.w(TAG, "Engine já existe, destruindo antes de criar novo")
                try {
                    agoraEngine?.leaveChannel()
                    agoraEngine = null
                    RtcEngine.destroy()
                } catch (e: Exception) {
                    Log.w(TAG, "Erro ao destruir engine existente", e)
                }
            }
            
            val config = RtcEngineConfig().apply {
                // Usar applicationContext armazenado para evitar problemas com ciclo de vida da Activity
                mContext = appContext
                mAppId = APP_ID
                mEventHandler = eventHandler
            }
            
            Log.d(TAG, "Criando Agora Engine com APP_ID: $APP_ID")
            agoraEngine = RtcEngine.create(config)
            
            if (agoraEngine == null) {
                Log.e(TAG, "Falha ao criar Agora Engine - RtcEngine.create retornou null")
                Log.e(TAG, "Possíveis causas:")
                Log.e(TAG, "  1. APP_ID inválido ou incorreto")
                Log.e(TAG, "  2. Já existe uma instância do engine não destruída")
                Log.e(TAG, "  3. Permissões não concedidas (CAMERA, RECORD_AUDIO)")
                Log.e(TAG, "  4. Problema com o contexto da aplicação")
                return false
            }
            
            // Habilitar vídeo e áudio
            agoraEngine?.enableVideo()
            agoraEngine?.enableAudio()
            
            // Configurar perfil de áudio
            agoraEngine?.setAudioProfile(
                Constants.AUDIO_PROFILE_MUSIC_HIGH_QUALITY,
                Constants.AUDIO_SCENARIO_CHATROOM
            )
            
            // Configurar vídeo
            val videoConfig = VideoEncoderConfiguration().apply {
                dimensions = VideoEncoderConfiguration.VD_640x480
                frameRate = 15  // 15 FPS
                bitrate = VideoEncoderConfiguration.STANDARD_BITRATE
                orientationMode = VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_ADAPTIVE
                mirrorMode = VideoEncoderConfiguration.MIRROR_MODE_TYPE.MIRROR_MODE_AUTO
            }
            agoraEngine?.setVideoEncoderConfiguration(videoConfig)
            
            Log.d(TAG, "✅ Agora Engine inicializado com sucesso")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao inicializar Agora Engine", e)
            e.printStackTrace()
            agoraEngine = null
            false
        }
    }
    
    /**
     * Entra no canal
     */
    suspend fun joinChannel(
        channelName: String,
        uid: Int,
        token: String? = null
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                if (agoraEngine == null) {
                    Log.e(TAG, "Agora Engine não está inicializado")
                    return@withContext false
                }
                
                val option = ChannelMediaOptions().apply {
                    clientRoleType = Constants.CLIENT_ROLE_BROADCASTER
                }
                
                val code = if (token != null && token.isNotEmpty()) {
                    agoraEngine?.joinChannel(token, channelName, uid, option) ?: -1
                } else {
                    // Fallback: entrar sem token (modo desenvolvimento)
                    Log.w(TAG, "Entrando no canal sem token (modo desenvolvimento)")
                    agoraEngine?.joinChannel(null, channelName, uid, option) ?: -1
                }
                
                if (code == 0) {
                    Log.d(TAG, "Tentando entrar no canal: $channelName com UID: $uid")
                    true
                } else {
                    Log.e(TAG, "Erro ao entrar no canal. Código: $code")
                    false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao entrar no canal", e)
                false
            }
        }
    }
    
    /**
     * Configura vídeo local (selfie)
     */
    fun setupLocalVideo(view: SurfaceView) {
        agoraEngine?.let { engine ->
            val canvas = VideoCanvas(view, Constants.RENDER_MODE_HIDDEN, 0)
            engine.setupLocalVideo(canvas)
            engine.startPreview()
            Log.d(TAG, "Vídeo local configurado")
        }
    }
    
    /**
     * Configura vídeo remoto
     */
    fun setupRemoteVideo(view: SurfaceView, uid: Int) {
        agoraEngine?.let { engine ->
            val canvas = VideoCanvas(view, Constants.RENDER_MODE_HIDDEN, uid)
            engine.setupRemoteVideo(canvas)
            Log.d(TAG, "Vídeo remoto configurado para UID: $uid")
        }
    }
    
    /**
     * Mutar áudio
     */
    fun muteAudio(muted: Boolean): Boolean {
        return agoraEngine?.muteLocalAudioStream(muted) == 0
    }
    
    /**
     * Mutar vídeo
     */
    fun muteVideo(muted: Boolean): Boolean {
        return agoraEngine?.muteLocalVideoStream(muted) == 0
    }
    
    /**
     * Alternar câmera (frente/trás)
     */
    fun switchCamera(): Boolean {
        return agoraEngine?.switchCamera() == 0
    }
    
    /**
     * Finalizar chamada e sair do canal
     */
    fun leaveChannel() {
        agoraEngine?.let { engine ->
            engine.stopPreview()
            engine.leaveChannel()
            Log.d(TAG, "Saiu do canal")
        }
    }
    
    /**
     * Libera recursos
     */
    fun release() {
        try {
            leaveChannel()
            if (agoraEngine != null) {
                agoraEngine = null
                RtcEngine.destroy()
                Log.d(TAG, "Agora Engine destruído")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao liberar Agora Engine", e)
        } finally {
            isConnected = false
            remoteUid = 0
            currentChannel = null
            Log.d(TAG, "Agora Engine liberado")
        }
    }
    
    fun isConnected(): Boolean = isConnected
    fun getRemoteUid(): Int = remoteUid
    fun getCurrentChannel(): String? = currentChannel
}

