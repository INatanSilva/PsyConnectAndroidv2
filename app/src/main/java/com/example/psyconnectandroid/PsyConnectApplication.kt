package com.example.psyconnectandroid

import android.app.Application
import com.google.firebase.FirebaseApp

/**
 * Classe Application principal do app
 * Inicializa componentes globais como cache e Firebase
 */
class PsyConnectApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Inicializar Firebase
        FirebaseApp.initializeApp(this)
        
        // Inicializar Cache Manager
        CacheManager.init(this)
        
        // Limpar itens expirados do cache na inicialização
        CacheManager.clearExpired()
        
        android.util.Log.d("PsyConnectApp", "✅ Application initialized with caching enabled")
    }
}

