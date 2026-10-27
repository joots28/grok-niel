package com.otiniel.copy

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Deterministic local command layer. When the native library is packaged,
 * Rust classifies the command; Kotlin still formats locale/time-dependent text.
 * The Kotlin classifier is a safe fallback for builds without the native library.
 */
object AssistantEngine {
    private val locale = Locale("pt", "BR")

    fun reply(input: String, now: LocalDateTime = LocalDateTime.now()): String {
        val normalized = input.trim().lowercase(locale)
        val command = CopyRustBridge.classify(input) ?: classifyInKotlin(normalized)

        return when (command) {
            "empty" -> "Escreva uma mensagem para começar."
            "greeting" ->
                "Olá. Sou a interface local do Copy. O núcleo inteligente ainda está em integração."
            "time" ->
                "Agora são " + now.format(DateTimeFormatter.ofPattern("HH:mm", locale)) + "."
            "date" ->
                "Hoje é " + now.format(DateTimeFormatter.ofPattern("dd/MM/yyyy", locale)) + "."
            "help" ->
                "Comandos locais disponíveis: perguntar a hora, perguntar a data e pedir ajuda. " +
                    "Perguntas gerais funcionarão quando o provedor remoto estiver ativo."
            else ->
                "Recebi sua mensagem e ela foi salva somente neste dispositivo. " +
                    "O núcleo de IA ainda não está conectado, então não vou inventar uma resposta " +
                    "nem enviar seu texto a um serviço externo."
        }
    }

    private fun classifyInKotlin(normalized: String): String {
        return when {
            normalized.isEmpty() -> "empty"
            normalized in listOf("oi", "olá", "ola", "bom dia", "boa tarde", "boa noite") -> "greeting"
            normalized.contains("hora") -> "time"
            normalized.contains("data") || normalized.contains("que dia") -> "date"
            normalized == "ajuda" || normalized == "comandos" -> "help"
            else -> "unknown"
        }
    }
}
