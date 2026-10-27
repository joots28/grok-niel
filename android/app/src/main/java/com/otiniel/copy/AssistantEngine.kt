package com.otiniel.copy

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Deterministic local response layer. The native Rust library handles the local
 * response when packaged; Kotlin keeps a matching fallback for development builds.
 */
object AssistantEngine {
    private val locale = Locale("pt", "BR")
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", locale)
    private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", locale)

    fun reply(input: String, now: LocalDateTime = LocalDateTime.now()): String {
        val rustResponse = CopyRustBridge.reply(
            input = input,
            currentTime = now.format(timeFormatter),
            currentDate = now.format(dateFormatter)
        )
        return rustResponse ?: fallbackReply(input, now)
    }

    private fun fallbackReply(input: String, now: LocalDateTime): String {
        val normalized = input.trim().lowercase(locale)
        if (normalized.isEmpty()) return "Escreva uma mensagem para começar."

        return when {
            normalized in listOf("oi", "olá", "ola", "bom dia", "boa tarde", "boa noite") ->
                "Olá. Sou a interface local do Copy. O núcleo inteligente ainda está em integração."
            normalized.contains("hora") ->
                "Agora são " + now.format(timeFormatter) + "."
            normalized.contains("data") || normalized.contains("que dia") ->
                "Hoje é " + now.format(dateFormatter) + "."
            normalized == "ajuda" || normalized == "comandos" ->
                "Comandos locais disponíveis: perguntar a hora, perguntar a data e pedir ajuda. " +
                    "Perguntas gerais funcionarão quando o provedor remoto estiver ativo."
            else ->
                "Recebi sua mensagem e ela foi salva somente neste dispositivo. " +
                    "O núcleo de IA ainda não está conectado, então não vou inventar uma resposta " +
                    "nem enviar seu texto a um serviço externo."
        }
    }
}
