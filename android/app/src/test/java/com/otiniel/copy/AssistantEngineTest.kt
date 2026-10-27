package com.otiniel.copy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class AssistantEngineTest {
    private val fixedTime = LocalDateTime.of(2026, 10, 27, 14, 5, 0)

    @Test
    fun returnsLocalTimeWithoutNetworkAccess() {
        assertEquals("Agora são 14:05.", AssistantEngine.reply("Que horas são?", fixedTime))
    }

    @Test
    fun returnsLocalDateWithoutNetworkAccess() {
        assertEquals("Hoje é 27/10/2026.", AssistantEngine.reply("Qual a data de hoje?", fixedTime))
    }

    @Test
    fun emptyInputPromptsForMessage() {
        assertEquals("Escreva uma mensagem para começar.", AssistantEngine.reply("   ", fixedTime))
    }

    @Test
    fun unknownQuestionDoesNotPretendTheModelAnswered() {
        val response = AssistantEngine.reply("Explique computação quântica", fixedTime)
        assertTrue(response.contains("núcleo de IA ainda não está conectado"))
        assertTrue(response.contains("nem enviar seu texto a um serviço externo"))
    }

    @Test
    fun helpListsOnlyImplementedLocalCommands() {
        val response = AssistantEngine.reply("ajuda", fixedTime)
        assertTrue(response.contains("perguntar a hora"))
        assertTrue(response.contains("perguntar a data"))
        assertTrue(response.contains("pedir ajuda"))
        assertTrue(response.contains("provedor remoto"))
    }
}
