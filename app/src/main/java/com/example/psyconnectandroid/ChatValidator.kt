package com.example.psyconnectandroid

import android.util.Log

/**
 * Validador de mensagens de chat para bloquear:
 * - Números de telefone
 * - Links/URLs
 */
object ChatValidator {
    
    // Regex para detectar números de telefone (formato brasileiro)
    // Aceita: (11) 98765-4321, 11 98765-4321, 11987654321, +55 11 98765-4321, etc.
    private val PHONE_PATTERNS = listOf(
        Regex("""(\+?\d{1,3}[\s-]?)?(\(?\d{2,3}\)?[\s-]?)?\d{4,5}[\s-]?\d{4}"""), // Formato geral
        Regex("""\b\d{10,11}\b"""), // Apenas números (10-11 dígitos) com word boundaries
        Regex("""\(\d{2,3}\)[\s-]?\d{4,5}[\s-]?\d{4}"""), // (11) 98765-4321
        Regex("""\d{2,3}[\s.-]?\d{4,5}[\s.-]?\d{4}"""), // 11 98765-4321
        Regex("""\b(whatsapp|wpp|zap|telefone|celular|fone|contato|ligar|ligue|me chama|me liga)\b""", RegexOption.IGNORE_CASE) // Palavras relacionadas com word boundaries
    )
    
    // Regex para detectar URLs/links
    private val URL_PATTERNS = listOf(
        Regex("""https?://[^\s]+""", RegexOption.IGNORE_CASE), // http:// ou https://
        Regex("""www\.[^\s]+""", RegexOption.IGNORE_CASE), // www.exemplo.com
        Regex("""\b[a-zA-Z0-9-]+\.[a-zA-Z]{2,}(/[^\s]*)?""", RegexOption.IGNORE_CASE), // exemplo.com ou exemplo.com.br (com word boundary)
        Regex("""@[a-zA-Z0-9_]+"""), // @usuario (redes sociais)
        Regex("""\b(instagram|facebook|linkedin|twitter|x\.com|youtube|tiktok|telegram|discord|snapchat)\b""", RegexOption.IGNORE_CASE) // Nomes de redes sociais com word boundaries
    )
    
    /**
     * Valida se a mensagem contém conteúdo bloqueado
     * @return Pair<Boolean, String> - (isValid, errorMessage)
     */
    fun validateMessage(message: String): Pair<Boolean, String> {
        val trimmedMessage = message.trim()
        
        if (trimmedMessage.isEmpty()) {
            return Pair(false, "A mensagem não pode estar vazia")
        }
        
        // Verificar telefones
        for (pattern in PHONE_PATTERNS) {
            if (pattern.containsMatchIn(trimmedMessage)) {
                Log.d("ChatValidator", "Phone detected: ${pattern.find(trimmedMessage)?.value}")
                return Pair(false, "Não é permitido compartilhar números de telefone ou contatos no chat. Por favor, use apenas a plataforma para comunicação.")
            }
        }
        
        // Verificar links/URLs
        for (pattern in URL_PATTERNS) {
            if (pattern.containsMatchIn(trimmedMessage)) {
                Log.d("ChatValidator", "URL detected: ${pattern.find(trimmedMessage)?.value}")
                return Pair(false, "Não é permitido compartilhar links ou URLs no chat. Por favor, mantenha a conversa dentro da plataforma.")
            }
        }
        
        return Pair(true, "")
    }
    
    /**
     * Verifica se a mensagem contém apenas texto permitido
     */
    fun isMessageValid(message: String): Boolean {
        return validateMessage(message).first
    }
}

