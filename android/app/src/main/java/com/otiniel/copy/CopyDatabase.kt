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
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Add explicit migrations before incrementing DATABASE_VERSION.
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

    companion object {
        private const val DATABASE_NAME = "copy_local.db"
        private const val DATABASE_VERSION = 1
        private const val TABLE_MESSAGES = "messages"
        private const val COLUMN_ID = "_id"
        private const val COLUMN_ROLE = "role"
        private const val COLUMN_CONTENT = "content"
        private const val COLUMN_CREATED_AT = "created_at"
    }
}
