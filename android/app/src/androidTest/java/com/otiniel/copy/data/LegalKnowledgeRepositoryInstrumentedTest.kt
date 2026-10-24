package com.otiniel.copy.data

import android.content.Context
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
class LegalKnowledgeRepositoryInstrumentedTest {
    private lateinit var context: Context
    private lateinit var databaseName: String
    private lateinit var repository: LegalKnowledgeRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        databaseName = "copy-legal-test-${System.nanoTime()}.db"
        repository = LegalKnowledgeRepository(context, databaseName)
    }

    @After
    fun tearDown() {
        repository.close()
        context.deleteDatabase(databaseName)
    }

    @Test
    fun importsMarkdownAndSearchesLocallyWithSourceAndUnverifiedStatus() {
        val markdown = """
            # Lei de teste

            ## Art. 1º
            Toda pessoa tem direito ao exemplo descrito neste documento.

            ## Art. 2º
            Este trecho trata de um assunto completamente diferente.
        """.trimIndent()

        val imported = repository.importMarkdown(
            sourceName = "Corpus de teste",
            sourceUrl = "https://example.org/lei.md",
            licenseNotice = "Conteúdo fictício usado em teste",
            markdown = markdown,
            importedAt = 1234L,
        )

        assertFalse(imported.alreadyImported)
        assertTrue(imported.passageCount >= 2)
        assertEquals(1, repository.importedDocumentCount())

        val results = repository.search("direito exemplo")
        assertEquals(1, results.size)
        assertEquals("Lei de teste", results.single().title)
        assertEquals("https://example.org/lei.md", results.single().sourceUrl)
        assertEquals("Corpus de teste", results.single().sourceName)
        assertEquals(1234L, results.single().importedAt)
        assertEquals("unverified", results.single().verificationStatus)
        assertTrue(results.single().excerpt.contains("direito"))
    }

    @Test
    fun repeatedImportOfSameContentDoesNotCreateDuplicates() {
        val markdown = "# Documento\n\nArt. 1º\nConteúdo único de teste."
        val first = repository.importMarkdown(
            "Fonte", "https://example.org/doc.md", "Licença de teste", markdown,
        )
        val second = repository.importMarkdown(
            "Outra fonte", "https://example.org/outro.md", "Licença de teste", markdown,
        )

        assertFalse(first.alreadyImported)
        assertTrue(second.alreadyImported)
        assertEquals(first.documentId, second.documentId)
        assertEquals(first.contentHash, second.contentHash)
        assertEquals(1, repository.importedDocumentCount())
    }

    @Test
    fun rejectsUnsafeSourceUrlsAndOversizedDocuments() {
        assertRejected {
            repository.importMarkdown("Fonte", "http://example.org/doc.md", "", "# Conteúdo")
        }
        assertRejected {
            repository.importMarkdown("Fonte", "file:///tmp/doc.md", "", "# Conteúdo")
        }
        assertRejected {
            repository.importMarkdown("Fonte", "https://example.org/doc.md", "", "x".repeat(2_000_001))
        }
        assertEquals(0, repository.importedDocumentCount())
    }

    @Test
    fun emptyOrPunctuationOnlySearchReturnsNoResults() {
        repository.importMarkdown(
            "Fonte", "https://example.org/doc.md", "Licença", "# Documento\n\nConteúdo de pesquisa.",
        )
        assertTrue(repository.search("").isEmpty())
        assertTrue(repository.search("!!! ...").isEmpty())
    }

    @Test
    fun searchIsAccentInsensitive() {
        repository.importMarkdown(
            "Fonte", "https://example.org/doc.md", "Licença", "# Documento\n\nA legislação contém informação pública.",
        )
        assertEquals(1, repository.search("legislacao informacao").size)
    }

    private fun assertRejected(block: () -> Unit) {
        var rejected = false
        try {
            block()
        } catch (_: IllegalArgumentException) {
            rejected = true
        }
        assertTrue("Expected invalid import to be rejected", rejected)
    }
}
