package com.otiniel.copy

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Deterministic local command layer used when remote AI is disabled or unavailable.
 * It does not perform general language generation or access network services.
 */
object AssistantEngine {
    private val locale = Locale("pt", "BR")

    fun reply(input: String, now: LocalDateTime = LocalDateTime.now()): String {
        val normalized = input.trim().lowercase(locale)
        if (normalized.isEmpty()) return "Escreva uma mensagem para começar."

        return when {
            normalized in listOf("oi", "olá", "ola", "bom dia", "boa tarde", "boa noite") ->
                "Olá. Sou a interface local do Copy. O núcleo inteligente ainda está em integração."

            normalized.contains("hora") ->
                "Agora são " + now.format(DateTimeFormatter.ofPattern("HH:mm", locale)) + "."

            normalized.contains("data") || normalized.contains("que dia") ->
                "Hoje é " + now.format(DateTimeFormatter.ofPattern("dd/MM/yyyy", locale)) + "."

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
