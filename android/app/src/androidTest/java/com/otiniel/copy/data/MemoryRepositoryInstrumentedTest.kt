package com.otiniel.copy.data

import android.content.ContentValues
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
class MemoryRepositoryInstrumentedTest {
    private lateinit var context: Context
    private lateinit var databaseName: String
    private lateinit var repository: MemoryRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        databaseName = "copy-memory-test-\${System.nanoTime()}.db"
        repository = MemoryRepository(context, databaseName)
    }

    @After
    fun tearDown() {
        repository.close()
        context.deleteDatabase(databaseName)
    }

    @Test
    fun createUpdateDeleteAndReopenMemoryDatabase() {
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
        assertEquals("Preferências atualizadas", updated.title)
        assertEquals("Respostas detalhadas", updated.detail)
        repository.delete(updated.id)
        assertTrue(repository.getAll().isEmpty())
    }

    @Test
    fun encryptsNewMemoryFieldsInSqlite() {
        repository.save(null, "Segredo", "Conteúdo privado")
        val id = repository.getAll().single().id
        repository.close()
        val db = PlainSQLiteDatabase.openDatabase(
            context.getDatabasePath(databaseName).absolutePath,
            null,
            PlainSQLiteDatabase.OPEN_READONLY,
        )
        try {
            val values = db.query(
                "memories", arrayOf("title", "detail"), "id = ?", arrayOf(id.toString()),
                null, null, null,
            ).use { cursor -> cursor.moveToFirst(); cursor.getString(0) to cursor.getString(1) }
            assertTrue(values.first.startsWith(LocalFieldCipher.PREFIX))
            assertTrue(values.second.startsWith(LocalFieldCipher.PREFIX))
            assertFalse(values.first.contains("Segredo"))
            assertFalse(values.second.contains("Conteúdo privado"))
        } finally {
            db.close()
        }
    }

    @Test
    fun migratesLegacyPlaintextMemoriesAndPreservesContent() {
        val legacyFile = context.getDatabasePath(databaseName)
        val legacy = PlainSQLiteDatabase.openOrCreateDatabase(legacyFile, null)
        legacy.execSQL(
            """
            CREATE TABLE memories (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                title TEXT NOT NULL CHECK(length(trim(title)) BETWEEN 1 AND 120),
                detail TEXT NOT NULL CHECK(length(trim(detail)) BETWEEN 1 AND 3000),
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        val values = ContentValues().apply {
            put("id", 4L)
            put("title", "Memória antiga")
            put("detail", "Conteúdo preservado")
            put("created_at", 100L)
            put("updated_at", 200L)
        }
        legacy.insertOrThrow("memories", null, values)
        legacy.version = 1
        legacy.close()

        val memory = repository.getAll().single()
        assertEquals("Memória antiga", memory.title)
        assertEquals("Conteúdo preservado", memory.detail)
        repository.close()
        val db = PlainSQLiteDatabase.openDatabase(legacyFile.absolutePath, null, PlainSQLiteDatabase.OPEN_READONLY)
        try {
            val valuesStored = db.query("memories", arrayOf("title", "detail"), "id = ?", arrayOf("4"), null, null, null)
                .use { cursor -> cursor.moveToFirst(); cursor.getString(0) to cursor.getString(1) }
            assertTrue(valuesStored.first.startsWith(LocalFieldCipher.PREFIX))
            assertTrue(valuesStored.second.startsWith(LocalFieldCipher.PREFIX))
            assertFalse(valuesStored.first.contains("Memória antiga"))
            assertFalse(valuesStored.second.contains("Conteúdo preservado"))
            assertEquals("Memória antiga", LocalFieldCipher.decrypt(valuesStored.first, "memory.title"))
            assertEquals("Conteúdo preservado", LocalFieldCipher.decrypt(valuesStored.second, "memory.detail"))
        } finally {
            db.close()
        }
    }

    @Test
    fun deleteAllRemovesEverySavedMemory() {
        repository.save(null, "Primeira", "Conteúdo 1")
        repository.save(null, "Segunda", "Conteúdo 2")
        assertEquals(2, repository.getAll().size)
        assertEquals(2, repository.deleteAll())
        assertTrue(repository.getAll().isEmpty())
        assertEquals(0, repository.deleteAll())
    }

    @Test
    fun repositoryRejectsBlankTitleAndContent() {
        var blankTitleRejected = false
        try { repository.save(null, "   ", "Conteúdo válido") } catch (_: IllegalArgumentException) { blankTitleRejected = true }
        assertTrue(blankTitleRejected)
        var blankContentRejected = false
        try { repository.save(null, "Título válido", "   ") } catch (_: IllegalArgumentException) { blankContentRejected = true }
        assertTrue(blankContentRejected)
        assertTrue(repository.getAll().isEmpty())
    }
}
