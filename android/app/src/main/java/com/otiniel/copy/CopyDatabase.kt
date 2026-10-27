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

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        // Ask SQLite to overwrite deleted cell content rather than leave it in free pages.
        db.execSQL("PRAGMA secure_delete = ON")
    }

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
        if (oldVersion == 2) migrateMemoriesToEncryptedStorage(db, existingContentIsEncrypted = false)
        if (oldVersion == 3) migrateMemoriesToEncryptedStorage(db, existingContentIsEncrypted = true)
    }

    private fun createMemoriesTable(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS personal_memories (
                _id INTEGER PRIMARY KEY AUTOINCREMENT,
                content TEXT NOT NULL CHECK(length(content) BETWEEN 1 AND 3072),
                created_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }

    /** Rebuilds the table while preserving IDs and timestamps; version 2 stored plaintext. */
    private fun migrateMemoriesToEncryptedStorage(db: SQLiteDatabase, existingContentIsEncrypted: Boolean) {
        db.execSQL("ALTER TABLE personal_memories RENAME TO personal_memories_v2")
        createMemoriesTable(db)

        val oldMemories = mutableListOf<PersonalMemory>()
        db.query(
            "personal_memories_v2",
            arrayOf(COLUMN_ID, COLUMN_CONTENT, COLUMN_CREATED_AT),
            null, null, null, null, "$COLUMN_ID ASC"
        ).use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(COLUMN_ID)
            val contentIndex = cursor.getColumnIndexOrThrow(COLUMN_CONTENT)
            val timeIndex = cursor.getColumnIndexOrThrow(COLUMN_CREATED_AT)
            while (cursor.moveToNext()) {
                oldMemories += PersonalMemory(
                    id = cursor.getLong(idIndex),
                    content = cursor.getString(contentIndex),
                    createdAt = cursor.getLong(timeIndex)
                )
            }
        }

        oldMemories.forEach { memory ->
            val values = ContentValues().apply {
                put(COLUMN_ID, memory.id)
                put(
                    COLUMN_CONTENT,
                    if (existingContentIsEncrypted) memory.content else LocalMemoryCrypto.encrypt(memory.content)
                )
                put(COLUMN_CREATED_AT, memory.createdAt)
            }
            db.insertOrThrow(TABLE_MEMORIES, null, values)
        }
        db.execSQL("DROP TABLE personal_memories_v2")
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
                    content = LocalMemoryCrypto.decrypt(cursor.getString(contentIndex)),
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
        val encryptedContent = LocalMemoryCrypto.encrypt(normalized)
        require(encryptedContent.length <= MAX_ENCRYPTED_MEMORY_LENGTH) {
            "A memória cifrada excedeu o limite interno permitido."
        }
        val values = ContentValues().apply {
            put(COLUMN_CONTENT, encryptedContent)
            put(COLUMN_CREATED_AT, createdAt)
        }
        val id = writableDatabase.insertOrThrow(TABLE_MEMORIES, null, values)
        return PersonalMemory(id = id, content = normalized, createdAt = createdAt)
    }

    fun deleteMemory(id: Long) {
        writableDatabase.delete(TABLE_MEMORIES, "$COLUMN_ID = ?", arrayOf(id.toString()))
    }

    fun clearMemories() {
        writableDatabase.delete(TABLE_MEMORIES, null, null)
    }

    companion object {
        private const val DATABASE_NAME = "copy_local.db"
        private const val DATABASE_VERSION = 4
        private const val TABLE_MESSAGES = "messages"
        private const val TABLE_MEMORIES = "personal_memories"
        private const val COLUMN_ID = "_id"
        private const val COLUMN_ROLE = "role"
        private const val COLUMN_CONTENT = "content"
        private const val COLUMN_CREATED_AT = "created_at"
        private const val MAX_MEMORY_LENGTH = 500
        private const val MAX_ENCRYPTED_MEMORY_LENGTH = 3072
    }
}
