package com.otiniel.copy.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase as PlainSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
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
        databaseName = "copy-conversations-test-${System.nanoTime()}.db"
        repository = ConversationRepository(context, databaseName)
    }

    @After
    fun tearDown() {
        repository.close()
        EncryptedDatabaseBootstrap.deleteForTesting(context, databaseName, "local:" + databaseName)
    }

    @Test
    fun restoresMessagesAndDeletesSessionWithItsMessages() {
        val sessionId = repository.createSession("  Plano da semana  ")
        repository.addMessage(sessionId, "assistant", "Resposta local de demonstração")

        val firstMessages = repository.getMessages(sessionId)
        assertEquals(2, firstMessages.size)
        assertEquals("user", firstMessages[0].role)
        assertEquals("Plano da semana", firstMessages[0].content)
        assertEquals("assistant", firstMessages[1].role)

        repository.close()
        repository = ConversationRepository(context, databaseName)

        val session = repository.getSessions().single()
        assertEquals(sessionId, session.id)
        assertEquals("Plano da semana", session.title)
        assertEquals(2, session.messageCount)
        assertEquals("Resposta local de demonstração", session.preview)
        assertEquals(firstMessages.map { it.content }, repository.getMessages(sessionId).map { it.content })

        repository.deleteSession(sessionId)
        assertTrue(repository.getSessions().isEmpty())
        assertTrue(repository.getMessages(sessionId).isEmpty())
    }


    @Test
    fun migratesLegacyPlaintextDatabaseWithoutLosingSessionsOrMessages() {
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

        val session = repository.getSessions().single()
        assertEquals(7L, session.id)
        assertEquals("Importada", session.title)
        assertEquals(1, session.messageCount)
        assertEquals("Mensagem antiga", repository.getMessages(7L).single().content)
        assertTrue("The plaintext database should be deleted only after migration succeeds.", !legacyFile.exists())

        val encryptedFile = context.getDatabasePath(EncryptedDatabaseBootstrap.encryptedNameFor(databaseName))
        assertTrue(encryptedFile.exists())
        val header = ByteArray(16)
        encryptedFile.inputStream().use { input -> input.read(header) }
        assertTrue(
            "Encrypted SQLite must not retain the plaintext SQLite header.",
            !String(header, Charsets.US_ASCII).startsWith("SQLite format 3"),
        )
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
