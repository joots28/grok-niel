package com.otiniel.copy

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProviderChatClientTest {
    @Test
    fun appliesConciseAndDetailedResponseStyles() {
        val concise = ProviderChatClient.buildSystemPrompt(emptyList(), "Conciso")
        val detailed = ProviderChatClient.buildSystemPrompt(emptyList(), "Detalhado")
        val balanced = ProviderChatClient.buildSystemPrompt(emptyList(), "Equilibrado")

        assertTrue(concise.contains("respostas curtas, diretas"))
        assertTrue(detailed.contains("apresente etapas"))
        assertTrue(balanced.contains("Equilibre clareza"))
    }

    @Test
    fun doesNotIncludeMemoryContextByDefault() {
        val prompt = ProviderChatClient.buildSystemPrompt(emptyList())

        assertFalse(prompt.contains("Memórias pessoais fornecidas pelo usuário"))
        assertTrue(prompt.contains("Você é COPY"))
    }

    @Test
    fun includesOnlyNonBlankUserProvidedMemories() {
        val prompt = ProviderChatClient.buildSystemPrompt(
            listOf("Prefiro respostas diretas.", "", "   ", "Gosto de exemplos práticos.")
        )

        assertTrue(prompt.contains("- Prefiro respostas diretas."))
        assertTrue(prompt.contains("- Gosto de exemplos práticos."))
        assertFalse(prompt.contains("-  \n"))
    }

    @Test
    fun capsMemoryContextToFourThousandCharacters() {
        val prompt = ProviderChatClient.buildSystemPrompt(listOf("x".repeat(5000)))
        val marker = "Memórias pessoais fornecidas pelo usuário"
        val index = prompt.indexOf(marker)

        assertTrue(index >= 0)
        val section = prompt.substring(index)
        val memoryStart = section.indexOf("\n- ")
        assertTrue(memoryStart >= 0)
        assertTrue(section.substring(memoryStart + 1).length <= 4002)
    }

    @Test
    fun capsMemoryContextToTwelveItems() {
        val items = (1..20).map { "memoria-$it" }
        val prompt = ProviderChatClient.buildSystemPrompt(items)
        val section = prompt.substring(prompt.indexOf("Memórias pessoais fornecidas pelo usuário"))

        for (index in 1..12) assertTrue(section.contains("memoria-$index"))
        assertFalse(section.contains("memoria-13"))
        assertFalse(section.contains("memoria-20"))
    }

    @Test
    fun truncatesAnIndividualMemoryWhenItExceedsRemainingContext() {
        val prompt = ProviderChatClient.buildSystemPrompt(listOf("a".repeat(10000), "segunda memória"))
        val marker = "Memórias pessoais fornecidas pelo usuário"
        val start = prompt.indexOf(marker)
        assertTrue(start >= 0)
        val context = prompt.substring(start)
        assertTrue(context.contains("a".repeat(1000)))
        assertFalse(context.contains("segunda memória"))
    }
}
