package com.otiniel.copy.data

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
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
    fun createUpdateAndDeletePersistAcrossDatabaseReopen() {
        repository.save(null, "Preferências", "Respostas diretas")
        val created = repository.getAll().single()
        assertEquals("Preferências", created.title)
        assertEquals("Respostas diretas", created.detail)

        repository.close()
        repository = MemoryRepository(context, databaseName)

        val restored = repository.getAll().single()
        assertEquals(created.id, restored.id)
        assertEquals("Preferências", restored.title)
        assertEquals("Respostas diretas", restored.detail)

        repository.save(restored.id, "Preferências atualizadas", "Respostas detalhadas")
        repository.close()
        repository = MemoryRepository(context, databaseName)

        val updated = repository.getAll().single()
        assertEquals(restored.id, updated.id)
        assertEquals("Preferências atualizadas", updated.title)
        assertEquals("Respostas detalhadas", updated.detail)

        repository.delete(updated.id)
        assertTrue(repository.getAll().isEmpty())
    }

    @Test
    fun databaseRejectsBlankTitleAndContent() {
        var blankTitleRejected = false
        try {
            repository.save(null, "   ", "Conteúdo válido")
        } catch (_: SQLiteConstraintException) {
            blankTitleRejected = true
        }
        assertTrue("SQLite should reject a blank title", blankTitleRejected)

        var blankContentRejected = false
        try {
            repository.save(null, "Título válido", "   ")
        } catch (_: SQLiteConstraintException) {
            blankContentRejected = true
        }
        assertTrue("SQLite should reject blank content", blankContentRejected)
        assertTrue(repository.getAll().isEmpty())
    }
}
