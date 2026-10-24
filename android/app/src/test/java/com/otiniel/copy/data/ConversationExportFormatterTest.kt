package com.otiniel.copy.data

import org.junit.Assert.assertTrue
import org.junit.Test

class ConversationExportFormatterTest {
    @Test
    fun exportsConversationContentsAndEscapesJsonText() {
        val json = ConversationExportFormatter.toJson(
            listOf(
                ConversationExportRecord(
                    session = ConversationSession(
                        id = 4L,
                        title = "Título \"privado\"",
                        preview = "Prévia",
                        updatedAt = 456L,
                        messageCount = 2,
                    ),
                    messages = listOf(
                        ConversationMessage(
                            id = 11L,
                            sessionId = 4L,
                            role = "user",
                            content = "linha 1\nlinha 2\\caminho",
                            createdAt = 123L,
                        ),
                        ConversationMessage(
                            id = 12L,
                            sessionId = 4L,
                            role = "assistant",
                            content = "Resposta",
                            createdAt = 124L,
                        ),
                    ),
                ),
            ),
            exportedAt = 999L,
        )

        assertTrue(json.contains("\"schemaVersion\": 1"))
        assertTrue(json.contains("\"exportedAt\": 999"))
        assertTrue(json.contains("\"id\": 4"))
        assertTrue(json.contains("\"title\": \"Título \\\"privado\\\"\""))
        assertTrue(json.contains("\"content\": \"linha 1\\nlinha 2\\\\caminho\""))
        assertTrue(json.contains("\"role\": \"assistant\""))
        assertTrue(json.contains("\"createdAt\": 124"))
    }

    @Test
    fun exportsAnEmptyConversationList() {
        val json = ConversationExportFormatter.toJson(emptyList(), exportedAt = 1L)
        assertTrue(json.contains("\"conversations\": []"))
        assertTrue(json.endsWith("\"conversations\": []\n}\n"))
    }
}
