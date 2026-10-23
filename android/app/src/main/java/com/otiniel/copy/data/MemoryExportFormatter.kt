package com.otiniel.copy.data

/**
 * Small dependency-free JSON serializer for memory export.
 * Strings are escaped according to JSON string rules; stored content is not modified.
 */
object MemoryExportFormatter {
    fun toJson(memories: List<MemoryItem>, exportedAt: Long): String = buildString {
        append("{\n  \"schemaVersion\": 1,\n  \"exportedAt\": ")
        append(exportedAt)
        append(",\n  \"memories\": [")
        memories.forEachIndexed { index, memory ->
            append(if (index == 0) "\n" else ",\n")
            append("    {\"id\": ")
            append(memory.id)
            append(", \"title\": ")
            appendJsonString(memory.title)
            append(", \"detail\": ")
            appendJsonString(memory.detail)
            append('}')
        }
        if (memories.isNotEmpty()) append('\n')
        append("  ]\n}\n")
    }

    private fun StringBuilder.appendJsonString(value: String) {
        append('"')
        value.forEach { character ->
            when (character) {
                '"' -> append("\\\"")
                '\\' -> append("\\\\")
                '\b' -> append("\\b")
                '\u000C' -> append("\\f")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> if (character.code < 0x20) {
                    append("\\u")
                    append(character.code.toString(16).padStart(4, '0'))
                } else {
                    append(character)
                }
            }
        }
        append('"')
    }
}
