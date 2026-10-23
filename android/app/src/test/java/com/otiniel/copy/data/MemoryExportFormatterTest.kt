package com.otiniel.copy.data

import org.junit.Assert.assertTrue
import org.junit.Test

class MemoryExportFormatterTest {
    @Test
    fun exportsSchemaAndEscapesContentAsJson() {
        val json = MemoryExportFormatter.toJson(
            listOf(MemoryItem(7L, "Rótulo \"premium\"", "linha 1\nlinha 2 \\ caminho")),
            exportedAt = 123456L,
        )

        assertTrue(json.contains("\"schemaVersion\": 1"))
        assertTrue(json.contains("\"exportedAt\": 123456"))
        assertTrue(json.contains("\"id\": 7"))
        assertTrue(json.contains("\"title\": \"Rótulo \\\"premium\\\"\""))
        assertTrue(json.contains("\"detail\": \"linha 1\\nlinha 2 \\\\ caminho\""))
    }

    @Test
    fun exportsAnEmptyMemoryList() {
        val json = MemoryExportFormatter.toJson(emptyList(), exportedAt = 1L)
        assertTrue(json.contains("\"memories\": []"))
    }
}
