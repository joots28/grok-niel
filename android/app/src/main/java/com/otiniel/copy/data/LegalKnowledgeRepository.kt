package com.otiniel.copy.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.net.URI
import java.security.MessageDigest
import java.util.Locale

/**
 * Local, opt-in Markdown knowledge store.
 *
 * Imported text is untrusted data: it is never executed or treated as system instructions.
 * The initial implementation uses parameterized LIKE queries as a portable lexical fallback.
 */
data class LegalSearchResult(
    val title: String,
    val locator: String?,
    val excerpt: String,
    val sourceName: String,
    val sourceUrl: String,
    val importedAt: Long,
    val contentHash: String,
    val verificationStatus: String,
)

data class LegalImportResult(
    val documentId: Long,
    val contentHash: String,
    val passageCount: Int,
    val alreadyImported: Boolean,
)

class LegalKnowledgeRepository(
    context: Context,
    databaseName: String = DATABASE_NAME,
) : AutoCloseable {
    private val helper = LegalDatabase(context.applicationContext, databaseName)

    /**
     * Imports one user-selected Markdown document. The caller must obtain the content through
     * Android's Storage Access Framework and pass the real source URL and attribution.
     */
    fun importMarkdown(
        sourceName: String,
        sourceUrl: String,
        licenseNotice: String,
        markdown: String,
        importedAt: Long = System.currentTimeMillis(),
    ): LegalImportResult {
        val cleanName = sourceName.trim()
        val cleanUrl = sourceUrl.trim()
        val cleanLicense = licenseNotice.trim()
        val cleanMarkdown = markdown.trim()

        require(cleanName.isNotEmpty() && cleanName.length <= MAX_SOURCE_NAME) {
            "O nome da fonte é obrigatório e deve ter até $MAX_SOURCE_NAME caracteres."
        }
        require(isSafeSourceUrl(cleanUrl)) { "A fonte deve usar uma URL HTTPS válida." }
        require(cleanLicense.length <= MAX_LICENSE_NOTICE) { "Aviso de licença muito longo." }
        require(cleanMarkdown.isNotEmpty()) { "O documento está vazio." }
        require(cleanMarkdown.length <= MAX_DOCUMENT_CHARS) {
            "O documento excede o limite de $MAX_DOCUMENT_CHARS caracteres."
        }

        val hash = sha256(cleanMarkdown)
        val passages = splitMarkdown(cleanMarkdown)
        require(passages.isNotEmpty()) { "Nenhum trecho pesquisável foi encontrado." }

        val db = helper.writableDatabase
        db.beginTransaction()
        try {
            val existingId = db.query(
                TABLE_DOCUMENTS,
                arrayOf(COLUMN_ID),
                "$COLUMN_HASH = ?",
                arrayOf(hash),
                null,
                null,
                null,
                "1",
            ).use { cursor -> if (cursor.moveToFirst()) cursor.getLong(0) else null }

            if (existingId != null) {
                db.setTransactionSuccessful()
                return LegalImportResult(existingId, hash, countPassages(db, existingId), true)
            }

            val sourceValues = ContentValues().apply {
                put(COLUMN_SOURCE_NAME, cleanName)
                put(COLUMN_SOURCE_URL, cleanUrl)
                put(COLUMN_LICENSE, cleanLicense)
                put(COLUMN_IMPORTED_AT, importedAt)
            }
            val sourceId = db.insertOrThrow(TABLE_SOURCES, null, sourceValues)
            val title = extractTitle(cleanMarkdown, cleanName)
            val documentValues = ContentValues().apply {
                put(COLUMN_SOURCE_ID, sourceId)
                put(COLUMN_TITLE, title)
                put(COLUMN_HASH, hash)
                put(COLUMN_IMPORTED_AT, importedAt)
                put(COLUMN_STATUS, STATUS_UNVERIFIED)
            }
            val documentId = db.insertOrThrow(TABLE_DOCUMENTS, null, documentValues)

            passages.forEach { passage ->
                val values = ContentValues().apply {
                    put(COLUMN_DOCUMENT_ID, documentId)
                    put(COLUMN_LOCATOR, passage.locator)
                    put(COLUMN_TEXT, passage.text)
                    put(COLUMN_NORMALIZED_TEXT, normalize(passage.text))
                }
                db.insertOrThrow(TABLE_PASSAGES, null, values)
            }
            db.setTransactionSuccessful()
            return LegalImportResult(documentId, hash, passages.size, false)
        } finally {
            db.endTransaction()
        }
    }

    /**
     * Searches only locally imported documents. The caller should display the source and the
     * "vigência não verificada" status; search relevance is not legal validation.
     */
    fun search(query: String, limit: Int = DEFAULT_LIMIT): List<LegalSearchResult> {
        require(query.length <= MAX_QUERY_CHARS) { "A consulta é muito longa." }
        val terms = tokenize(query)
        if (terms.isEmpty()) return emptyList()
        require(limit in 1..MAX_LIMIT) { "O limite deve ficar entre 1 e $MAX_LIMIT." }

        val clauses = terms.joinToString(" AND ") { "p.$COLUMN_NORMALIZED_TEXT LIKE ?" }
        val args = terms.map { "%${escapeLike(it)}%" }.toTypedArray()
        val sql = """
            SELECT d.$COLUMN_TITLE, p.$COLUMN_LOCATOR, p.$COLUMN_TEXT,
                   s.$COLUMN_SOURCE_NAME, s.$COLUMN_SOURCE_URL,
                   d.$COLUMN_IMPORTED_AT, d.$COLUMN_HASH, d.$COLUMN_STATUS
            FROM $TABLE_PASSAGES p
            JOIN $TABLE_DOCUMENTS d ON d.$COLUMN_ID = p.$COLUMN_DOCUMENT_ID
            JOIN $TABLE_SOURCES s ON s.$COLUMN_ID = d.$COLUMN_SOURCE_ID
            WHERE $clauses
            ORDER BY d.$COLUMN_IMPORTED_AT DESC, d.$COLUMN_ID DESC
            LIMIT ?
        """.trimIndent()
        val queryArgs = args + limit.toString()
        return helper.readableDatabase.rawQuery(sql, queryArgs).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(
                        LegalSearchResult(
                            title = cursor.getString(0),
                            locator = cursor.getString(1),
                            excerpt = cursor.getString(2),
                            sourceName = cursor.getString(3),
                            sourceUrl = cursor.getString(4),
                            importedAt = cursor.getLong(5),
                            contentHash = cursor.getString(6),
                            verificationStatus = cursor.getString(7),
                        ),
                    )
                }
            }
        }
    }

    fun importedDocumentCount(): Int =
        helper.readableDatabase.rawQuery("SELECT COUNT(*) FROM $TABLE_DOCUMENTS", null).use { cursor ->
            cursor.moveToFirst()
            cursor.getInt(0)
        }

    override fun close() = helper.close()

    private fun countPassages(db: SQLiteDatabase, documentId: Long): Int =
        db.rawQuery(
            "SELECT COUNT(*) FROM $TABLE_PASSAGES WHERE $COLUMN_DOCUMENT_ID = ?",
            arrayOf(documentId.toString()),
        ).use { cursor -> cursor.moveToFirst(); cursor.getInt(0) }

    private data class Passage(val locator: String?, val text: String)

    private fun splitMarkdown(markdown: String): List<Passage> {
        val result = mutableListOf<Passage>()
        var currentLocator: String? = null
        val current = StringBuilder()

        fun flush() {
            val text = current.toString().trim()
            if (text.isNotEmpty()) {
                text.chunked(MAX_PASSAGE_CHARS).forEachIndexed { index, chunk ->
                    result += Passage(
                        locator = if (index == 0) currentLocator else currentLocator?.let { "$it (continuação)" },
                        text = chunk,
                    )
                }
            }
            current.setLength(0)
        }

        markdown.lineSequence().forEach { line ->
            val heading = HEADING_REGEX.find(line)
            val article = ARTICLE_REGEX.find(line)
            if (heading != null || article != null) {
                flush()
                currentLocator = (article?.value ?: heading?.groupValues?.get(1))
                    ?.trim()
                    ?.take(MAX_LOCATOR_CHARS)
            }
            if (line.isBlank()) {
                flush()
            } else {
                if (current.isNotEmpty()) current.append('\n')
                current.append(line)
                if (current.length >= MAX_PASSAGE_CHARS) flush()
            }
        }
        flush()
        return result
    }

    private fun extractTitle(markdown: String, fallback: String): String =
        markdown.lineSequence()
            .map { it.trim() }
            .firstOrNull { it.startsWith("#") }
            ?.trimStart('#')
            ?.trim()
            ?.take(MAX_TITLE_CHARS)
            ?.takeIf { it.isNotEmpty() }
            ?: fallback.take(MAX_TITLE_CHARS)

    private fun isSafeSourceUrl(value: String): Boolean = try {
        val uri = URI(value)
        uri.scheme.equals("https", ignoreCase = true) &&
            !uri.host.isNullOrBlank() &&
            uri.userInfo == null &&
            value.length <= MAX_SOURCE_URL
    } catch (_: Exception) {
        false
    }

    private fun tokenize(value: String): List<String> =
        normalize(value).split(Regex("[^\\p{L}\\p{N}]+"))
            .filter { it.length >= 2 }
            .distinct()
            .take(MAX_QUERY_TERMS)

    private fun normalize(value: String): String =
        java.text.Normalizer.normalize(value.lowercase(Locale.ROOT), java.text.Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
            .trim()

    private fun escapeLike(value: String): String =
        value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")

    private fun sha256(value: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }

    private class LegalDatabase(context: Context, name: String) :
        SQLiteOpenHelper(context, name, null, DATABASE_VERSION) {
        override fun onConfigure(db: SQLiteDatabase) {
            super.onConfigure(db)
            db.setForeignKeyConstraintsEnabled(true)
        }

        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE $TABLE_SOURCES (
                    $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                    $COLUMN_SOURCE_NAME TEXT NOT NULL,
                    $COLUMN_SOURCE_URL TEXT NOT NULL,
                    $COLUMN_LICENSE TEXT NOT NULL,
                    $COLUMN_IMPORTED_AT INTEGER NOT NULL
                )
                """.trimIndent(),
            )
            db.execSQL(
                """
                CREATE TABLE $TABLE_DOCUMENTS (
                    $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                    $COLUMN_SOURCE_ID INTEGER NOT NULL REFERENCES $TABLE_SOURCES($COLUMN_ID) ON DELETE CASCADE,
                    $COLUMN_TITLE TEXT NOT NULL,
                    $COLUMN_HASH TEXT NOT NULL UNIQUE,
                    $COLUMN_IMPORTED_AT INTEGER NOT NULL,
                    $COLUMN_STATUS TEXT NOT NULL
                )
                """.trimIndent(),
            )
            db.execSQL(
                """
                CREATE TABLE $TABLE_PASSAGES (
                    $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                    $COLUMN_DOCUMENT_ID INTEGER NOT NULL REFERENCES $TABLE_DOCUMENTS($COLUMN_ID) ON DELETE CASCADE,
                    $COLUMN_LOCATOR TEXT,
                    $COLUMN_TEXT TEXT NOT NULL,
                    $COLUMN_NORMALIZED_TEXT TEXT NOT NULL
                )
                """.trimIndent(),
            )
            db.execSQL(
                "CREATE INDEX idx_legal_passages_document ON $TABLE_PASSAGES($COLUMN_DOCUMENT_ID)",
            )
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            error("No migration path is defined from legal database version $oldVersion to $newVersion.")
        }
    }

    companion object {
        const val DATABASE_NAME = "copy-legal-knowledge.db"
        private const val DATABASE_VERSION = 1
        private const val TABLE_SOURCES = "legal_sources"
        private const val TABLE_DOCUMENTS = "legal_documents"
        private const val TABLE_PASSAGES = "legal_passages"
        private const val COLUMN_ID = "id"
        private const val COLUMN_SOURCE_ID = "source_id"
        private const val COLUMN_DOCUMENT_ID = "document_id"
        private const val COLUMN_TITLE = "title"
        private const val COLUMN_HASH = "content_hash"
        private const val COLUMN_IMPORTED_AT = "imported_at"
        private const val COLUMN_STATUS = "verification_status"
        private const val COLUMN_LOCATOR = "locator"
        private const val COLUMN_TEXT = "text"
        private const val COLUMN_NORMALIZED_TEXT = "normalized_text"
        private const val COLUMN_SOURCE_NAME = "source_name"
        private const val COLUMN_SOURCE_URL = "source_url"
        private const val COLUMN_LICENSE = "license_notice"
        private const val MAX_SOURCE_NAME = 160
        private const val MAX_SOURCE_URL = 2048
        private const val MAX_LICENSE_NOTICE = 1000
        private const val MAX_TITLE_CHARS = 240
        private const val MAX_LOCATOR_CHARS = 120
        private const val MAX_DOCUMENT_CHARS = 2_000_000
        private const val MAX_PASSAGE_CHARS = 8_000
        private const val MAX_QUERY_CHARS = 240
        private const val MAX_QUERY_TERMS = 8
        private const val DEFAULT_LIMIT = 10
        private const val MAX_LIMIT = 50
        private const val STATUS_UNVERIFIED = "unverified"
        private val HEADING_REGEX = Regex("^\\s{0,3}#{1,6}\\s+(.+?)\\s*#*\\s*$")
        private val ARTICLE_REGEX = Regex("^\\s*(?:Art\\.?|Artigo)\\s+\\d+[A-Za-zº°.-]*.*$", RegexOption.IGNORE_CASE)
    }
}
