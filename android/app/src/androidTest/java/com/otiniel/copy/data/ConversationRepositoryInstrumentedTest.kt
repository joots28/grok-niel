package com.otiniel.copy.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase as PlainSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ConversationRepositoryInstrumentedTest {
    private lateinit var context: Context
    private lateinit var databaseName: String
    private lateinit var repository: ConversationRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        databaseName = "copy-conversations-test-\${System.nanoTime()}.db"
        repository = ConversationRepository(context, databaseName)
    }

    @After
    fun tearDown() {
        repository.close()
        context.deleteDatabase(databaseName)
    }

    @Test
    fun restoresMessagesAndDeletesSessionWithItsMessages() {
        val sessionId = repository.createSession("Plano da semana")
        repository.addMessage(sessionId, "assistant", "Resposta local de demonstração")
        val expectedMessages = repository.getMessages(sessionId)

        repository.close()
        repository = ConversationRepository(context, databaseName)

        val session = repository.getSessions().single()
        assertEquals(sessionId, session.id)
        assertEquals("Plano da semana", session.title)
        assertEquals(2, session.messageCount)
        assertEquals("Resposta local de demonstração", session.preview)
        assertEquals(expectedMessages.map { it.content }, repository.getMessages(sessionId).map { it.content })

        repository.deleteSession(sessionId)
        assertTrue(repository.getSessions().isEmpty())
        assertTrue(repository.getMessages(sessionId).isEmpty())
    }

    @Test
    fun encryptsNewConversationContentInSqlite() {
        val sessionId = repository.createSession("Conteúdo privado")
        repository.close()

        val db = PlainSQLiteDatabase.openDatabase(
            context.getDatabasePath(databaseName).absolutePath,
            null,
            PlainSQLiteDatabase.OPEN_READONLY,
        )
        try {
            val title = db.query(
                "conversation_sessions", arrayOf("title"), "id = ?", arrayOf(sessionId.toString()),
                null, null, null,
            ).use { cursor -> cursor.moveToFirst(); cursor.getString(0) }
            val message = db.query(
                "conversation_messages", arrayOf("content"), "session_id = ?", arrayOf(sessionId.toString()),
                null, null, "id ASC",
            ).use { cursor -> cursor.moveToFirst(); cursor.getString(0) }

            assertTrue(title.startsWith(LocalFieldCipher.PREFIX))
            assertTrue(message.startsWith(LocalFieldCipher.PREFIX))
            assertFalse(title.contains("Conteúdo privado"))
            assertFalse(message.contains("Conteúdo privado"))
        } finally {
            db.close()
        }
    }

    @Test
    fun migratesLegacyPlaintextDatabaseAndPreservesContent() {
        val legacyFile = context.getDatabasePath(databaseName)
        val legacy = PlainSQLiteDatabase.openOrCreateDatabase(legacyFile, null)
        legacy.execSQL(
            """
            CREATE TABLE conversation_sessions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                title TEXT NOT NULL CHECK(length(trim(title)) BETWEEN 1 AND 120),
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        legacy.execSQL(
            """
            CREATE TABLE conversation_messages (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                session_id INTEGER NOT NULL,
                role TEXT NOT NULL CHECK(role IN ('user', 'assistant')),
                content TEXT NOT NULL CHECK(length(trim(content)) BETWEEN 1 AND 12000),
                created_at INTEGER NOT NULL,
                FOREIGN KEY(session_id) REFERENCES conversation_sessions(id) ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        legacy.execSQL("INSERT INTO conversation_sessions (id, title, created_at, updated_at) VALUES (7, 'Importada', 100, 200)")
        legacy.execSQL("INSERT INTO conversation_messages (id, session_id, role, content, created_at) VALUES (11, 7, 'user', 'Mensagem antiga', 150)")
        legacy.version = 1
        legacy.close()

        assertEquals("Importada", repository.getSessions().single().title)
        assertEquals("Mensagem antiga", repository.getMessages(7L).single().content)
        repository.close()

        val db = PlainSQLiteDatabase.openDatabase(legacyFile.absolutePath, null, PlainSQLiteDatabase.OPEN_READONLY)
        try {
            val title = db.query("conversation_sessions", arrayOf("title"), "id = ?", arrayOf("7"), null, null, null)
                .use { cursor -> cursor.moveToFirst(); cursor.getString(0) }
            val message = db.query("conversation_messages", arrayOf("content"), "id = ?", arrayOf("11"), null, null, null)
                .use { cursor -> cursor.moveToFirst(); cursor.getString(0) }
            assertTrue(title.startsWith(LocalFieldCipher.PREFIX))
            assertTrue(message.startsWith(LocalFieldCipher.PREFIX))
            assertFalse(title.contains("Importada"))
            assertFalse(message.contains("Mensagem antiga"))
            assertEquals("Importada", LocalFieldCipher.decrypt(title, "conversation.title"))
            assertEquals("Mensagem antiga", LocalFieldCipher.decrypt(message, "conversation.content"))
        } finally {
            db.close()
        }
    }

    @Test
    fun createsEmptySessionWhenNoInitialPromptIsProvided() {
        val sessionId = repository.createSession()
        val session = repository.getSessions().single()
        assertEquals(sessionId, session.id)
        assertEquals("Nova conversa", session.title)
        assertEquals(0, session.messageCount)
        assertTrue(repository.getMessages(sessionId).isEmpty())
    }
}
