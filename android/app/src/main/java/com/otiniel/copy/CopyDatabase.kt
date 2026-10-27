package com.otiniel.copy

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class ChatMessage(
    val id: Long = 0L,
    val role: String,
    val content: String,
    val createdAt: Long
)

data class PersonalMemory(
    val id: Long = 0L,
    val content: String,
    val createdAt: Long
)

class CopyDatabase(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE messages (
                _id INTEGER PRIMARY KEY AUTOINCREMENT,
                role TEXT NOT NULL CHECK(role IN ('user', 'assistant')),
                content TEXT NOT NULL,
                created_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
        createMemoriesTable(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) createMemoriesTable(db)
    }

    private fun createMemoriesTable(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS personal_memories (
                _id INTEGER PRIMARY KEY AUTOINCREMENT,
                content TEXT NOT NULL CHECK(length(trim(content)) BETWEEN 1 AND 500),
                created_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }

    fun getMessages(): List<ChatMessage> {
        val result = mutableListOf<ChatMessage>()
        readableDatabase.query(
            TABLE_MESSAGES,
            arrayOf(COLUMN_ID, COLUMN_ROLE, COLUMN_CONTENT, COLUMN_CREATED_AT),
            null, null, null, null, "$COLUMN_ID ASC"
        ).use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(COLUMN_ID)
            val roleIndex = cursor.getColumnIndexOrThrow(COLUMN_ROLE)
            val contentIndex = cursor.getColumnIndexOrThrow(COLUMN_CONTENT)
            val timeIndex = cursor.getColumnIndexOrThrow(COLUMN_CREATED_AT)
            while (cursor.moveToNext()) {
                result += ChatMessage(
                    id = cursor.getLong(idIndex),
                    role = cursor.getString(roleIndex),
                    content = cursor.getString(contentIndex),
                    createdAt = cursor.getLong(timeIndex)
                )
            }
        }
        return result
    }

    fun insertMessage(message: ChatMessage): ChatMessage {
        val values = ContentValues().apply {
            put(COLUMN_ROLE, message.role)
            put(COLUMN_CONTENT, message.content)
            put(COLUMN_CREATED_AT, message.createdAt)
        }
        val id = writableDatabase.insertOrThrow(TABLE_MESSAGES, null, values)
        return message.copy(id = id)
    }

    fun clearMessages() {
        writableDatabase.delete(TABLE_MESSAGES, null, null)
    }

    fun getMemories(): List<PersonalMemory> {
        val result = mutableListOf<PersonalMemory>()
        readableDatabase.query(
            TABLE_MEMORIES,
            arrayOf(COLUMN_ID, COLUMN_CONTENT, COLUMN_CREATED_AT),
            null, null, null, null, "$COLUMN_ID ASC"
        ).use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(COLUMN_ID)
            val contentIndex = cursor.getColumnIndexOrThrow(COLUMN_CONTENT)
            val timeIndex = cursor.getColumnIndexOrThrow(COLUMN_CREATED_AT)
            while (cursor.moveToNext()) {
                result += PersonalMemory(
                    id = cursor.getLong(idIndex),
                    content = cursor.getString(contentIndex),
                    createdAt = cursor.getLong(timeIndex)
                )
            }
        }
        return result
    }

    fun insertMemory(content: String, createdAt: Long = System.currentTimeMillis()): PersonalMemory {
        val normalized = content.trim()
        require(normalized.isNotEmpty()) { "A memória não pode ficar vazia." }
        require(normalized.length <= MAX_MEMORY_LENGTH) { "A memória deve ter até $MAX_MEMORY_LENGTH caracteres." }
        val values = ContentValues().apply {
            put(COLUMN_CONTENT, normalized)
            put(COLUMN_CREATED_AT, createdAt)
        }
        val id = writableDatabase.insertOrThrow(TABLE_MEMORIES, null, values)
        return PersonalMemory(id = id, content = normalized, createdAt = createdAt)
    }

    fun deleteMemory(id: Long) {
        writableDatabase.delete(TABLE_MEMORIES, "$COLUMN_ID = ?", arrayOf(id.toString()))
    }

    companion object {
        private const val DATABASE_NAME = "copy_local.db"
        private const val DATABASE_VERSION = 2
        private const val TABLE_MESSAGES = "messages"
        private const val TABLE_MEMORIES = "personal_memories"
        private const val COLUMN_ID = "_id"
        private const val COLUMN_ROLE = "role"
        private const val COLUMN_CONTENT = "content"
        private const val COLUMN_CREATED_AT = "created_at"
        private const val MAX_MEMORY_LENGTH = 500
    }
}
