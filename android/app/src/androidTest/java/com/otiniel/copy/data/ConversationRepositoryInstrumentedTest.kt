package com.otiniel.copy.data

import android.content.Context
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
        context.deleteDatabase(databaseName)
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
    fun createsEmptySessionWhenNoInitialPromptIsProvided() {
        val sessionId = repository.createSession()
        val session = repository.getSessions().single()

        assertEquals(sessionId, session.id)
        assertEquals("Nova conversa", session.title)
        assertEquals(0, session.messageCount)
        assertTrue(repository.getMessages(sessionId).isEmpty())
    }
}
