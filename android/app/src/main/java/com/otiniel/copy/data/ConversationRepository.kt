package com.otiniel.copy.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * Local-only conversation storage. It never sends messages to a remote service.
 * The database is app-private but does not have database-level encryption.
 */
class ConversationRepository(context: Context, databaseName: String = DATABASE_NAME) {
    private val helper = ConversationDatabase(context.applicationContext, databaseName)

    fun createSession(initialPrompt: String = ""): Long {
        val now = System.currentTimeMillis()
        val cleanPrompt = initialPrompt.trim()
        require(cleanPrompt.length <= MAX_MESSAGE_LENGTH) { "Message is too long." }
        val title = cleanPrompt.lineSequence().firstOrNull().orEmpty()
            .take(MAX_TITLE_LENGTH)
            .ifBlank { DEFAULT_TITLE }
        val values = ContentValues().apply {
            put(COLUMN_TITLE, title)
            put(COLUMN_CREATED_AT, now)
            put(COLUMN_UPDATED_AT, now)
        }
        val id = helper.writableDatabase.insertOrThrow(TABLE_SESSIONS, null, values)
        if (cleanPrompt.isNotEmpty()) addMessage(id, ROLE_USER, cleanPrompt)
        return id
    }

    fun addMessage(sessionId: Long, role: String, content: String): ConversationMessage {
        require(role == ROLE_USER || role == ROLE_ASSISTANT) { "Unsupported message role." }
        val cleanContent = content.trim()
        require(cleanContent.isNotEmpty()) { "Message cannot be empty." }
        require(cleanContent.length <= MAX_MESSAGE_LENGTH) { "Message is too long." }

        val db = helper.writableDatabase
        val now = System.currentTimeMillis()
        db.beginTransaction()
        try {
            check(sessionExists(db, sessionId)) { "Conversation no longer exists." }
            val values = ContentValues().apply {
                put(COLUMN_SESSION_ID, sessionId)
                put(COLUMN_ROLE, role)
                put(COLUMN_CONTENT, cleanContent)
                put(COLUMN_CREATED_AT, now)
            }
            val messageId = db.insertOrThrow(TABLE_MESSAGES, null, values)
            val sessionValues = ContentValues().apply {
                put(COLUMN_UPDATED_AT, now)
            }
            db.update(TABLE_SESSIONS, sessionValues, "$COLUMN_ID = ?", arrayOf(sessionId.toString()))
            if (role == ROLE_USER) {
                val firstMessageTitle = ContentValues().apply {
                    put(COLUMN_TITLE, cleanContent.lineSequence().first().take(MAX_TITLE_LENGTH))
                }
                db.update(
                    TABLE_SESSIONS,
                    firstMessageTitle,
                    "$COLUMN_ID = ? AND $COLUMN_TITLE = ?",
                    arrayOf(sessionId.toString(), DEFAULT_TITLE),
                )
            }
            db.setTransactionSuccessful()
            return ConversationMessage(messageId, sessionId, role, cleanContent, now)
        } finally {
            db.endTransaction()
        }
    }

    fun getSessions(): List<ConversationSession> {
        val sql = """
            SELECT s.$COLUMN_ID, s.$COLUMN_TITLE, s.$COLUMN_UPDATED_AT,
                   COUNT(m.$COLUMN_ID) AS message_count,
                   COALESCE((
                       SELECT m2.$COLUMN_CONTENT FROM $TABLE_MESSAGES m2
                       WHERE m2.$COLUMN_SESSION_ID = s.$COLUMN_ID
                       ORDER BY m2.$COLUMN_CREATED_AT DESC, m2.$COLUMN_ID DESC LIMIT 1
                   ), '') AS preview
            FROM $TABLE_SESSIONS s
            LEFT JOIN $TABLE_MESSAGES m ON m.$COLUMN_SESSION_ID = s.$COLUMN_ID
            GROUP BY s.$COLUMN_ID
            ORDER BY s.$COLUMN_UPDATED_AT DESC, s.$COLUMN_ID DESC
        """.trimIndent()
        return helper.readableDatabase.rawQuery(sql, null).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(
                        ConversationSession(
                            id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                            title = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE)),
                            preview = cursor.getString(cursor.getColumnIndexOrThrow("preview")),
                            updatedAt = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_UPDATED_AT)),
                            messageCount = cursor.getInt(cursor.getColumnIndexOrThrow("message_count")),
                        ),
                    )
                }
            }
        }
    }

    fun getMessages(sessionId: Long): List<ConversationMessage> {
        return helper.readableDatabase.query(
            TABLE_MESSAGES,
            arrayOf(COLUMN_ID, COLUMN_SESSION_ID, COLUMN_ROLE, COLUMN_CONTENT, COLUMN_CREATED_AT),
            "$COLUMN_SESSION_ID = ?",
            arrayOf(sessionId.toString()),
            null,
            null,
            "$COLUMN_CREATED_AT ASC, $COLUMN_ID ASC",
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(
                        ConversationMessage(
                            id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                            sessionId = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_SESSION_ID)),
                            role = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ROLE)),
                            content = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CONTENT)),
                            createdAt = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_CREATED_AT)),
                        ),
                    )
                }
            }
        }
    }

    fun deleteSession(sessionId: Long) {
        val deleted = helper.writableDatabase.delete(
            TABLE_SESSIONS,
            "$COLUMN_ID = ?",
            arrayOf(sessionId.toString()),
        )
        check(deleted == 1) { "Conversation no longer exists." }
        // Messages are removed by the foreign-key cascade.
    }

    fun close() = helper.close()

    private fun sessionExists(db: SQLiteDatabase, sessionId: Long): Boolean =
        db.query(
            TABLE_SESSIONS,
            arrayOf(COLUMN_ID),
            "$COLUMN_ID = ?",
            arrayOf(sessionId.toString()),
            null,
            null,
            null,
            "1",
        ).use { it.moveToFirst() }

    private class ConversationDatabase(context: Context, name: String) : SQLiteOpenHelper(
        context,
        name,
        null,
        DATABASE_VERSION,
    ) {
        override fun onConfigure(db: SQLiteDatabase) {
            super.onConfigure(db)
            db.setForeignKeyConstraintsEnabled(true)
        }

        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE $TABLE_SESSIONS (
                    $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                    $COLUMN_TITLE TEXT NOT NULL CHECK(length(trim($COLUMN_TITLE)) BETWEEN 1 AND $MAX_TITLE_LENGTH),
                    $COLUMN_CREATED_AT INTEGER NOT NULL,
                    $COLUMN_UPDATED_AT INTEGER NOT NULL
                )
                """.trimIndent(),
            )
            db.execSQL(
                """
                CREATE TABLE $TABLE_MESSAGES (
                    $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                    $COLUMN_SESSION_ID INTEGER NOT NULL,
                    $COLUMN_ROLE TEXT NOT NULL CHECK($COLUMN_ROLE IN ('$ROLE_USER', '$ROLE_ASSISTANT')),
                    $COLUMN_CONTENT TEXT NOT NULL CHECK(length(trim($COLUMN_CONTENT)) BETWEEN 1 AND $MAX_MESSAGE_LENGTH),
                    $COLUMN_CREATED_AT INTEGER NOT NULL,
                    FOREIGN KEY($COLUMN_SESSION_ID) REFERENCES $TABLE_SESSIONS($COLUMN_ID) ON DELETE CASCADE
                )
                """.trimIndent(),
            )
            db.execSQL("CREATE INDEX index_messages_session_created ON $TABLE_MESSAGES($COLUMN_SESSION_ID, $COLUMN_CREATED_AT, $COLUMN_ID)")
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            error("No migration path is defined from database version $oldVersion to $newVersion.")
        }
    }

    private companion object {
        const val DATABASE_NAME = "copy-conversations.db"
        const val DATABASE_VERSION = 1
        const val TABLE_SESSIONS = "conversation_sessions"
        const val TABLE_MESSAGES = "conversation_messages"
        const val COLUMN_ID = "id"
        const val COLUMN_SESSION_ID = "session_id"
        const val COLUMN_TITLE = "title"
        const val COLUMN_ROLE = "role"
        const val COLUMN_CONTENT = "content"
        const val COLUMN_CREATED_AT = "created_at"
        const val COLUMN_UPDATED_AT = "updated_at"
        const val ROLE_USER = "user"
        const val ROLE_ASSISTANT = "assistant"
        const val DEFAULT_TITLE = "Nova conversa"
        const val MAX_TITLE_LENGTH = 120
        const val MAX_MESSAGE_LENGTH = 12000
    }
}
