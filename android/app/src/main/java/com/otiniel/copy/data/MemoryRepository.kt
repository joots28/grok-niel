package com.otiniel.copy.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase as PlainSQLiteDatabase
import net.zetetic.database.sqlcipher.SQLiteDatabase as CipherSQLiteDatabase
import net.zetetic.database.sqlcipher.SQLiteOpenHelper as CipherSQLiteOpenHelper

/**
 * Private, app-local storage for memories explicitly saved by the user.
 * The database is encrypted with SQLCipher and its random passphrase is wrapped by Android Keystore.
 */
class MemoryRepository(context: Context, databaseName: String = LEGACY_DATABASE_NAME) {
    private val appContext = context.applicationContext
    private val databaseId = "local:" + databaseName
    private val helperDelegate = lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        val prepared = EncryptedDatabaseBootstrap.prepare(
            context = appContext,
            legacyDatabaseName = databaseName,
            databaseId = databaseId,
            schemaVersion = DATABASE_VERSION,
            createSchema = ::createMemorySchema,
            migrateLegacy = ::migrateMemoryDatabase,
            validateSchema = ::validateMemorySchema,
        )
        MemoryDatabase(appContext, prepared.databaseName, prepared.passphrase).also {
            it.writableDatabase
        }
    }
    private val database: MemoryDatabase by helperDelegate

    fun getAll(): List<MemoryItem> {
        val columns = arrayOf(COLUMN_ID, COLUMN_TITLE, COLUMN_DETAIL)
        return database.readableDatabase.query(
            TABLE_MEMORIES,
            columns,
            null,
            null,
            null,
            null,
            "$COLUMN_UPDATED_AT DESC, $COLUMN_ID DESC",
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(
                        MemoryItem(
                            id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                            title = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE)),
                            detail = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DETAIL)),
                        ),
                    )
                }
            }
        }
    }

    fun save(id: Long?, title: String, detail: String) {
        val now = System.currentTimeMillis()
        val values = ContentValues().apply {
            put(COLUMN_TITLE, title.trim())
            put(COLUMN_DETAIL, detail.trim())
            put(COLUMN_UPDATED_AT, now)
            if (id == null) put(COLUMN_CREATED_AT, now)
        }

        val db = database.writableDatabase
        if (id == null) {
            db.insertOrThrow(TABLE_MEMORIES, null, values)
        } else {
            val changed = db.update(
                TABLE_MEMORIES,
                values,
                "$COLUMN_ID = ?",
                arrayOf(id.toString()),
            )
            check(changed == 1) { "The memory to update no longer exists." }
        }
    }

    fun delete(id: Long) {
        val deleted = database.writableDatabase.delete(
            TABLE_MEMORIES,
            "$COLUMN_ID = ?",
            arrayOf(id.toString()),
        )
        check(deleted == 1) { "The memory to delete no longer exists." }
    }

    fun deleteAll(): Int =
        database.writableDatabase.delete(TABLE_MEMORIES, null, null)

    fun close() {
        if (helperDelegate.isInitialized()) database.close()
    }

    private class MemoryDatabase(
        context: Context,
        name: String,
        passphrase: ByteArray,
    ) : CipherSQLiteOpenHelper(
        context,
        name,
        passphrase,
        null,
        DATABASE_VERSION,
        0,
        null,
        null,
        false,
    ) {
        override fun onCreate(db: CipherSQLiteDatabase) = createMemorySchema(db)

        override fun onUpgrade(db: CipherSQLiteDatabase, oldVersion: Int, newVersion: Int) {
            error("No migration path is defined from database version " + oldVersion + " to " + newVersion + ".")
        }
    }

    private companion object {
        const val LEGACY_DATABASE_NAME = "copy-local.db"
        const val DATABASE_VERSION = 1
        const val TABLE_MEMORIES = "memories"
        const val COLUMN_ID = "id"
        const val COLUMN_TITLE = "title"
        const val COLUMN_DETAIL = "detail"
        const val COLUMN_CREATED_AT = "created_at"
        const val COLUMN_UPDATED_AT = "updated_at"
    }
}

private fun createMemorySchema(db: CipherSQLiteDatabase) {
    db.execSQL(
        """
        CREATE TABLE memories (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            title TEXT NOT NULL
                CHECK(length(trim(title)) BETWEEN 1 AND 120),
            detail TEXT NOT NULL
                CHECK(length(trim(detail)) BETWEEN 1 AND 3000),
            created_at INTEGER NOT NULL,
            updated_at INTEGER NOT NULL
        )
        """.trimIndent(),
    )
}

private fun validateMemorySchema(db: CipherSQLiteDatabase) {
    val tableExists = db.rawQuery(
        "SELECT 1 FROM sqlite_master WHERE type='table' AND name=?",
        arrayOf("memories"),
    ).use { it.moveToFirst() }
    check(tableExists) { "The encrypted memory database is missing its expected table." }
}

private fun migrateMemoryDatabase(source: PlainSQLiteDatabase, target: CipherSQLiteDatabase) {
    val expectedRows = source.rawQuery("SELECT COUNT(*) FROM memories", null).use {
        check(it.moveToFirst())
        it.getLong(0)
    }

    target.beginTransaction()
    try {
        source.query(
            "memories",
            arrayOf("id", "title", "detail", "created_at", "updated_at"),
            null,
            null,
            null,
            null,
            "id ASC",
        ).use { cursor ->
            while (cursor.moveToNext()) {
                val values = ContentValues().apply {
                    put("id", cursor.getLong(cursor.getColumnIndexOrThrow("id")))
                    put("title", cursor.getString(cursor.getColumnIndexOrThrow("title")))
                    put("detail", cursor.getString(cursor.getColumnIndexOrThrow("detail")))
                    put("created_at", cursor.getLong(cursor.getColumnIndexOrThrow("created_at")))
                    put("updated_at", cursor.getLong(cursor.getColumnIndexOrThrow("updated_at")))
                }
                target.insertOrThrow("memories", null, values)
            }
        }
        target.setTransactionSuccessful()
    } finally {
        target.endTransaction()
    }

    val copiedRows = target.rawQuery("SELECT COUNT(*) FROM memories", null).use {
        check(it.moveToFirst())
        it.getLong(0)
    }
    check(copiedRows == expectedRows) {
        "Memory migration verification failed; the plaintext source was preserved."
    }
}
