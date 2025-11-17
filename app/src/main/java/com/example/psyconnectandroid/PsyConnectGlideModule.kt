package com.example.psyconnectandroid

import android.content.Context
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.load.engine.cache.InternalCacheDiskCacheFactory
import com.bumptech.glide.load.engine.cache.LruResourceCache
import com.bumptech.glide.module.AppGlideModule
import com.bumptech.glide.request.RequestOptions

/**
 * Configuração customizada do Glide para cache otimizado de imagens
 */
@GlideModule
class PsyConnectGlideModule : AppGlideModule() {
    
    override fun applyOptions(context: Context, builder: GlideBuilder) {
        // Cache em memória: 50 MB
        val memoryCacheSizeBytes = 1024 * 1024 * 50L
        builder.setMemoryCache(LruResourceCache(memoryCacheSizeBytes))
        
        // Cache em disco: 250 MB
        val diskCacheSizeBytes = 1024 * 1024 * 250L
        builder.setDiskCache(InternalCacheDiskCacheFactory(context, diskCacheSizeBytes))
        
        // Opções padrão
        builder.setDefaultRequestOptions(
            RequestOptions()
                .placeholder(R.drawable.ic_person)
                .error(R.drawable.ic_person)
        )
    }
    
    override fun isManifestParsingEnabled(): Boolean {
        return false
    }
}

