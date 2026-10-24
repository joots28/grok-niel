package com.otiniel.copy.data

data class ConversationExportRecord(
    val session: ConversationSession,
    val messages: List<ConversationMessage>,
)

/**
 * Creates a portable JSON snapshot of local conversations. The JSON is intentionally plain text;
 * the destination is chosen through Android's document picker and is not encrypted by this formatter.
 */
object ConversationExportFormatter {
    fun toJson(records: List<ConversationExportRecord>, exportedAt: Long): String = buildString {
        append("{\n  \"schemaVersion\": 1,\n  \"exportedAt\": ")
        append(exportedAt)
        append(",\n  \"conversations\": [")
        if (records.isEmpty()) {
            append("]\n}\n")
            return@buildString
        }

        records.forEachIndexed { index, record ->
            append(if (index == 0) "\n" else ",\n")
            append("    {\"id\": ")
            append(record.session.id)
            append(", \"title\": ")
            appendJsonString(record.session.title)
            append(", \"updatedAt\": ")
            append(record.session.updatedAt)
            append(", \"messageCount\": ")
            append(record.messages.size)
            append(", \"messages\": [")
            if (record.messages.isNotEmpty()) {
                record.messages.forEachIndexed { messageIndex, message ->
                    append(if (messageIndex == 0) "\n" else ",\n")
                    append("      {\"id\": ")
                    append(message.id)
                    append(", \"role\": ")
                    appendJsonString(message.role)
                    append(", \"content\": ")
                    appendJsonString(message.content)
                    append(", \"createdAt\": ")
                    append(message.createdAt)
                    append('}')
                }
                append("\n    ")
            }
            append("]}")
        }
        append("\n  ]\n}\n")
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
