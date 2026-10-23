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
class MemoryRepositoryInstrumentedTest {
    private lateinit var context: Context
    private lateinit var databaseName: String
    private lateinit var repository: MemoryRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        databaseName = "copy-memory-test-${System.nanoTime()}.db"
        repository = MemoryRepository(context, databaseName)
    }

    @After
    fun tearDown() {
        repository.close()
        context.deleteDatabase(databaseName)
    }

    @Test
    fun savesReopensEditsAndDeletesUserMemory() {
        assertTrue(repository.getAll().isEmpty())

        repository.save(id = null, title = "  Rotina  ", detail = "  Revisar tarefas  ")
        val created = repository.getAll().single()
        assertEquals("Rotina", created.title)
        assertEquals("Revisar tarefas", created.detail)

        repository.save(
            id = created.id,
            title = "Rotina da semana",
            detail = "Revisar tarefas e compromissos",
        )
        repository.close()
        repository = MemoryRepository(context, databaseName)

        val restored = repository.getAll().single()
        assertEquals(created.id, restored.id)
        assertEquals("Rotina da semana", restored.title)
        assertEquals("Revisar tarefas e compromissos", restored.detail)

        repository.delete(restored.id)
        assertTrue(repository.getAll().isEmpty())
    }
}
