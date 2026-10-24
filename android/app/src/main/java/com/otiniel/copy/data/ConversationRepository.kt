package com.otiniel.copy.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper

/**
 * Local-only conversation storage. Title/message content uses AES-GCM.
 * IDs, roles, timestamps and relations remain ordinary SQLite metadata; this is not whole-file encryption.
 */
class ConversationRepository(context: Context, databaseName: String = DATABASE_NAME) {
    private val helper = ConversationDatabase(context.applicationContext, databaseName)

    fun createSession(initialPrompt: String = ""): Long {
        val cleanPrompt = initialPrompt.trim()
        require(cleanPrompt.length <= MAX_MESSAGE_LENGTH) { "Message is too long." }
        val now = System.currentTimeMillis()
        val title = cleanPrompt.lineSequence().firstOrNull().orEmpty().take(MAX_TITLE_LENGTH).ifBlank { DEFAULT_TITLE }
        val values = ContentValues().apply {
            put(COLUMN_TITLE, LocalFieldCipher.encrypt(title, FIELD_TITLE))
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
                put(COLUMN_CONTENT, LocalFieldCipher.encrypt(cleanContent, FIELD_CONTENT))
                put(COLUMN_CREATED_AT, now)
            }
            val messageId = db.insertOrThrow(TABLE_MESSAGES, null, values)
            db.update(TABLE_SESSIONS, ContentValues().apply { put(COLUMN_UPDATED_AT, now) }, "$COLUMN_ID = ?", arrayOf(sessionId.toString()))
            if (role == ROLE_USER && storedTitle(db, sessionId) == DEFAULT_TITLE) {
                val title = cleanContent.lineSequence().first().take(MAX_TITLE_LENGTH)
                db.update(
                    TABLE_SESSIONS,
                    ContentValues().apply { put(COLUMN_TITLE, LocalFieldCipher.encrypt(title, FIELD_TITLE)) },
                    "$COLUMN_ID = ?",
                    arrayOf(sessionId.toString()),
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
                            title = LocalFieldCipher.decrypt(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE)), FIELD_TITLE),
                            preview = LocalFieldCipher.decrypt(cursor.getString(cursor.getColumnIndexOrThrow("preview")), FIELD_CONTENT),
                            updatedAt = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_UPDATED_AT)),
                            messageCount = cursor.getInt(cursor.getColumnIndexOrThrow("message_count")),
                        ),
                    )
                }
            }
        }
    }

    fun getMessages(sessionId: Long): List<ConversationMessage> =
        helper.readableDatabase.query(
            TABLE_MESSAGES,
            arrayOf(COLUMN_ID, COLUMN_SESSION_ID, COLUMN_ROLE, COLUMN_CONTENT, COLUMN_CREATED_AT),
            "$COLUMN_SESSION_ID = ?",
            arrayOf(sessionId.toString()),
            null, null,
            "$COLUMN_CREATED_AT ASC, $COLUMN_ID ASC",
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(
                        ConversationMessage(
                            id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                            sessionId = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_SESSION_ID)),
                            role = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ROLE)),
                            content = LocalFieldCipher.decrypt(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CONTENT)), FIELD_CONTENT),
                            createdAt = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_CREATED_AT)),
                        ),
                    )
                }
            }
        }

    fun deleteSession(sessionId: Long) {
        val deleted = helper.writableDatabase.delete(TABLE_SESSIONS, "$COLUMN_ID = ?", arrayOf(sessionId.toString()))
        check(deleted == 1) { "Conversation no longer exists." }
    }

    /**
     * Deletes every conversation in one SQLite transaction. Foreign-key cascade removes messages.
     * Returns the number of sessions removed so callers can report the real result.
     */
    fun deleteAllSessions(): Int {
        val db = helper.writableDatabase
        db.beginTransaction()
        try {
            val deleted = db.delete(TABLE_SESSIONS, null, null)
            db.setTransactionSuccessful()
            return deleted
        } finally {
            db.endTransaction()
        }
    }

    fun close() = helper.close()

    private fun storedTitle(db: SQLiteDatabase, sessionId: Long): String =
        db.query(TABLE_SESSIONS, arrayOf(COLUMN_TITLE), "$COLUMN_ID = ?", arrayOf(sessionId.toString()),
            null, null, null, "1").use { cursor ->
            check(cursor.moveToFirst()) { "Conversation no longer exists." }
            LocalFieldCipher.decrypt(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE)), FIELD_TITLE)
        }

    private fun sessionExists(db: SQLiteDatabase, sessionId: Long): Boolean =
        db.query(TABLE_SESSIONS, arrayOf(COLUMN_ID), "$COLUMN_ID = ?", arrayOf(sessionId.toString()),
            null, null, null, "1").use { it.moveToFirst() }

    private class ConversationDatabase(context: Context, name: String) :
        SQLiteOpenHelper(context, name, null, DATABASE_VERSION) {
        private var needsVacuumAfterUpgrade = false

        override fun onConfigure(db: SQLiteDatabase) {
            super.onConfigure(db)
            db.setForeignKeyConstraintsEnabled(true)
            db.rawQuery("PRAGMA secure_delete = ON", null).use { cursor -> cursor.moveToFirst() }
        }

        override fun onCreate(db: SQLiteDatabase) {
            createConversationTables(db, TABLE_SESSIONS, TABLE_MESSAGES)
            createConversationIndex(db)
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            if (oldVersion == 1 && newVersion == DATABASE_VERSION) {
                migrateLegacyConversations(db)
                needsVacuumAfterUpgrade = true
            } else {
                error("No migration path is defined from database version $oldVersion to $newVersion.")
            }
        }

        override fun onOpen(db: SQLiteDatabase) {
            super.onOpen(db)
            if (needsVacuumAfterUpgrade) {
                try {
                    db.execSQL("VACUUM")
                } catch (_: SQLiteException) {
                    // Keep verified data accessible if free storage is limited.
                } finally {
                    needsVacuumAfterUpgrade = false
                }
            }
        }

        private fun migrateLegacyConversations(db: SQLiteDatabase) {
            val oldSessions = countRows(db, TABLE_SESSIONS)
            val oldMessages = countRows(db, TABLE_MESSAGES)
            createConversationTables(db, TEMP_SESSIONS, TEMP_MESSAGES)

            db.query(TABLE_SESSIONS, arrayOf(COLUMN_ID, COLUMN_TITLE, COLUMN_CREATED_AT, COLUMN_UPDATED_AT),
                null, null, null, null, "$COLUMN_ID ASC").use { cursor ->
                while (cursor.moveToNext()) {
                    val values = ContentValues().apply {
                        put(COLUMN_ID, cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID)))
                        put(COLUMN_TITLE, LocalFieldCipher.encrypt(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE)), FIELD_TITLE))
                        put(COLUMN_CREATED_AT, cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_CREATED_AT)))
                        put(COLUMN_UPDATED_AT, cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_UPDATED_AT)))
                    }
                    db.insertOrThrow(TEMP_SESSIONS, null, values)
                }
            }
            db.query(TABLE_MESSAGES, arrayOf(COLUMN_ID, COLUMN_SESSION_ID, COLUMN_ROLE, COLUMN_CONTENT, COLUMN_CREATED_AT),
                null, null, null, null, "$COLUMN_ID ASC").use { cursor ->
                while (cursor.moveToNext()) {
                    val values = ContentValues().apply {
                        put(COLUMN_ID, cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID)))
                        put(COLUMN_SESSION_ID, cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_SESSION_ID)))
                        put(COLUMN_ROLE, cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ROLE)))
                        put(COLUMN_CONTENT, LocalFieldCipher.encrypt(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CONTENT)), FIELD_CONTENT))
                        put(COLUMN_CREATED_AT, cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_CREATED_AT)))
                    }
                    db.insertOrThrow(TEMP_MESSAGES, null, values)
                }
            }
            check(countRows(db, TEMP_SESSIONS) == oldSessions && countRows(db, TEMP_MESSAGES) == oldMessages) {
                "Conversation migration verification failed; original tables remain within the upgrade transaction."
            }
            db.execSQL("DROP TABLE $TABLE_MESSAGES")
            db.execSQL("DROP TABLE $TABLE_SESSIONS")
            db.execSQL("ALTER TABLE $TEMP_SESSIONS RENAME TO $TABLE_SESSIONS")
            db.execSQL("ALTER TABLE $TEMP_MESSAGES RENAME TO $TABLE_MESSAGES")
            createConversationIndex(db)
        }
    }

    private companion object {
        const val DATABASE_NAME = "copy-conversations.db"
        const val DATABASE_VERSION = 2
        const val TABLE_SESSIONS = "conversation_sessions"
        const val TABLE_MESSAGES = "conversation_messages"
        const val TEMP_SESSIONS = "conversation_sessions_secure_v2"
        const val TEMP_MESSAGES = "conversation_messages_secure_v2"
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
        const val FIELD_TITLE = "conversation.title"
        const val FIELD_CONTENT = "conversation.content"
    }
}

private fun createConversationTables(db: SQLiteDatabase, sessionsTable: String, messagesTable: String) {
    db.execSQL("""
        CREATE TABLE $sessionsTable (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            title TEXT NOT NULL CHECK(length(title) BETWEEN 1 AND 700),
            created_at INTEGER NOT NULL,
            updated_at INTEGER NOT NULL
        )
    """.trimIndent())
    db.execSQL("""
        CREATE TABLE $messagesTable (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            session_id INTEGER NOT NULL,
            role TEXT NOT NULL CHECK(role IN ('user', 'assistant')),
            content TEXT NOT NULL CHECK(length(content) BETWEEN 1 AND 66000),
            created_at INTEGER NOT NULL,
            FOREIGN KEY(session_id) REFERENCES $sessionsTable(id) ON DELETE CASCADE
        )
    """.trimIndent())
}

private fun createConversationIndex(db: SQLiteDatabase) {
    db.execSQL("CREATE INDEX index_messages_session_created ON conversation_messages(session_id, created_at, id)")
}

private fun countRows(db: SQLiteDatabase, tableName: String): Long =
    db.rawQuery("SELECT COUNT(*) FROM $tableName", null).use { cursor ->
        check(cursor.moveToFirst())
        cursor.getLong(0)
    }
