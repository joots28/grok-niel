package com.otiniel.copy.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LuoPromptBuilderTest {
    @Test
    fun systemInstructionContainsTrustedRulesWithoutEmbeddingSavedMemories() {
        val prompt = LuoPromptBuilder.systemInstruction()

        assertTrue(prompt.contains("assistente pessoal do aplicativo Copy"))
        assertTrue(prompt.contains("dados do usuário"))
        assertFalse(prompt.contains("respostas objetivas"))
        assertTrue(prompt.contains("Não invente fatos pessoais"))
    }

    @Test
    fun savedMemoriesStayInBoundedUserTurnRatherThanSystemInstruction() {
        val memory = SavedMemoryContext.build(
            listOf(com.otiniel.copy.data.MemoryItem(1L, "Preferência", "Respostas objetivas")),
        )
        val systemPrompt = LuoPromptBuilder.systemInstruction()
        val userTurn = LuoPromptBuilder.boundUserTurn("O que você lembra?", memory)

        assertFalse(systemPrompt.contains("Respostas objetivas"))
        assertTrue(userTurn.contains("INÍCIO_DOS_DADOS_DE_MEMÓRIA"))
        assertTrue(userTurn.contains("Respostas objetivas"))
        assertTrue(userTurn.contains("Mensagem atual do usuário:"))
        assertTrue(userTurn.contains("O que você lembra?"))
    }

    @Test
    fun longUserPromptIsBoundedAndDisclosesTruncation() {
        val prompt = LuoPromptBuilder.boundUserPrompt("a".repeat(5000))

        assertTrue(prompt.length < 1400)
        assertTrue(prompt.contains("parte do final não foi incluída"))
    }

    @Test
    fun shortUserPromptIsPreserved() {
        val prompt = "  Olá, Luo.  "

        assertTrue(LuoPromptBuilder.boundUserPrompt(prompt) == "Olá, Luo.")
    }

    @Test
    fun historyMessageIsTrimmedToConfiguredLimit() {
        val bounded = LuoPromptBuilder.boundHistoryMessage("x".repeat(1000))

        assertTrue(bounded.length <= LuoPromptBuilder.MAX_HISTORY_MESSAGE_CHARS)
        assertFalse(bounded.isBlank())
    }
}
