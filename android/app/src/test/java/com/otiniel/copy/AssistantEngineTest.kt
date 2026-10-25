package com.otiniel.copy

import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class AssistantEngineTest {
    @Test
    fun helpListsSupportedLocalCommands() {
        val result = AssistantEngine.reply("ajuda")
        assertTrue(result.contains("hora"))
        assertTrue(result.contains("data"))
    }

    @Test
    fun timeCommandReturnsFormattedTime() {
        val result = AssistantEngine.reply(
            "Que horas são?",
            LocalDateTime.of(2026, 10, 25, 14, 7)
        )
        assertTrue(result.contains("14:07"))
    }

    @Test
    fun unsupportedPromptDoesNotPretendToBeAnAiAnswer() {
        val result = AssistantEngine.reply("Explique relatividade geral")
        assertTrue(result.contains("não está conectado"))
        assertTrue(result.contains("não vou inventar"))
    }
}
