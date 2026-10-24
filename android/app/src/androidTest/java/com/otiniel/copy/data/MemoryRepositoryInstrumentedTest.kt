package com.otiniel.copy.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase as PlainSQLiteDatabase
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
        EncryptedDatabaseBootstrap.deleteForTesting(context, databaseName, "local:" + databaseName)
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
    fun migratesLegacyPlaintextMemoriesAndRemovesSourceOnlyAfterVerification() {
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

        val restored = repository.getAll().single()
        assertEquals(4L, restored.id)
        assertEquals("Memória antiga", restored.title)
        assertEquals("Conteúdo preservado", restored.detail)
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
    fun deleteAllRemovesEverySavedMemory() {
        repository.save(null, "Primeira", "Conteúdo 1")
        repository.save(null, "Segunda", "Conteúdo 2")
        assertEquals(2, repository.getAll().size)

        assertEquals(2, repository.deleteAll())
        assertTrue(repository.getAll().isEmpty())
        assertEquals(0, repository.deleteAll())
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
