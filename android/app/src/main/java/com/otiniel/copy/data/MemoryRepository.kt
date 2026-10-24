package com.otiniel.copy.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper

/**
 * App-private memory storage. Title/detail fields are AES-GCM encrypted.
 * IDs/timestamps remain ordinary SQLite metadata; this is not whole-file encryption.
 */
class MemoryRepository(context: Context, databaseName: String = DATABASE_NAME) {
    private val helper = MemoryDatabase(context.applicationContext, databaseName)

    fun getAll(): List<MemoryItem> =
        helper.readableDatabase.query(
            TABLE_MEMORIES,
            arrayOf(COLUMN_ID, COLUMN_TITLE, COLUMN_DETAIL),
            null, null, null, null,
            "$COLUMN_UPDATED_AT DESC, $COLUMN_ID DESC",
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(
                        MemoryItem(
                            id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                            title = LocalFieldCipher.decrypt(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE)), FIELD_TITLE),
                            detail = LocalFieldCipher.decrypt(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DETAIL)), FIELD_DETAIL),
                        ),
                    )
                }
            }
        }

    fun save(id: Long?, title: String, detail: String) {
        val cleanTitle = title.trim()
        val cleanDetail = detail.trim()
        require(cleanTitle.isNotEmpty()) { "A memory title is required." }
        require(cleanDetail.isNotEmpty()) { "Memory content is required." }
        require(cleanTitle.length <= MAX_TITLE_LENGTH) { "Memory title is too long." }
        require(cleanDetail.length <= MAX_DETAIL_LENGTH) { "Memory content is too long." }

        val now = System.currentTimeMillis()
        val values = ContentValues().apply {
            put(COLUMN_TITLE, LocalFieldCipher.encrypt(cleanTitle, FIELD_TITLE))
            put(COLUMN_DETAIL, LocalFieldCipher.encrypt(cleanDetail, FIELD_DETAIL))
            put(COLUMN_UPDATED_AT, now)
            if (id == null) put(COLUMN_CREATED_AT, now)
        }
        val db = helper.writableDatabase
        if (id == null) {
            db.insertOrThrow(TABLE_MEMORIES, null, values)
        } else {
            val changed = db.update(TABLE_MEMORIES, values, "$COLUMN_ID = ?", arrayOf(id.toString()))
            check(changed == 1) { "The memory to update no longer exists." }
        }
    }

    fun delete(id: Long) {
        val deleted = helper.writableDatabase.delete(
            TABLE_MEMORIES, "$COLUMN_ID = ?", arrayOf(id.toString()),
        )
        check(deleted == 1) { "The memory to delete no longer exists." }
    }

    fun deleteAll(): Int = helper.writableDatabase.delete(TABLE_MEMORIES, null, null)

    fun close() = helper.close()

    private class MemoryDatabase(context: Context, name: String) :
        SQLiteOpenHelper(context, name, null, DATABASE_VERSION) {
        private var needsVacuumAfterUpgrade = false

        override fun onConfigure(db: SQLiteDatabase) {
            super.onConfigure(db)
            db.rawQuery("PRAGMA secure_delete = ON", null).use { cursor -> cursor.moveToFirst() }
        }

        override fun onCreate(db: SQLiteDatabase) {
            createMemoryTable(db, TABLE_MEMORIES)
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            if (oldVersion == 1 && newVersion == DATABASE_VERSION) {
                migrateLegacyMemories(db)
                needsVacuumAfterUpgrade = true
            } else {
                error("No migration path is defined from database version $oldVersion to $newVersion.")
            }
        }

        override fun onOpen(db: SQLiteDatabase) {
            super.onOpen(db)
            if (needsVacuumAfterUpgrade) {
                try {
                    db.execSQL("VACUUM")
                } catch (_: SQLiteException) {
                    // Keep the migrated values available when free storage is limited.
                } finally {
                    needsVacuumAfterUpgrade = false
                }
            }
        }

        private fun migrateLegacyMemories(db: SQLiteDatabase) {
            val expectedRows = countRows(db, TABLE_MEMORIES)
            createMemoryTable(db, TEMP_TABLE)
            db.query(
                TABLE_MEMORIES,
                arrayOf(COLUMN_ID, COLUMN_TITLE, COLUMN_DETAIL, COLUMN_CREATED_AT, COLUMN_UPDATED_AT),
                null, null, null, null, "$COLUMN_ID ASC",
            ).use { cursor ->
                while (cursor.moveToNext()) {
                    val values = ContentValues().apply {
                        put(COLUMN_ID, cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID)))
                        put(
                            COLUMN_TITLE,
                            LocalFieldCipher.encrypt(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE)), FIELD_TITLE),
                        )
                        put(
                            COLUMN_DETAIL,
                            LocalFieldCipher.encrypt(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DETAIL)), FIELD_DETAIL),
                        )
                        put(COLUMN_CREATED_AT, cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_CREATED_AT)))
                        put(COLUMN_UPDATED_AT, cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_UPDATED_AT)))
                    }
                    db.insertOrThrow(TEMP_TABLE, null, values)
                }
            }
            check(countRows(db, TEMP_TABLE) == expectedRows) {
                "Memory migration verification failed; SQLite keeps the original table inside the upgrade transaction."
            }
            db.execSQL("DROP TABLE $TABLE_MEMORIES")
            db.execSQL("ALTER TABLE $TEMP_TABLE RENAME TO $TABLE_MEMORIES")
        }
    }

    private companion object {
        const val DATABASE_NAME = "copy-local.db"
        const val DATABASE_VERSION = 2
        const val TABLE_MEMORIES = "memories"
        const val TEMP_TABLE = "memories_secure_v2"
        const val COLUMN_ID = "id"
        const val COLUMN_TITLE = "title"
        const val COLUMN_DETAIL = "detail"
        const val COLUMN_CREATED_AT = "created_at"
        const val COLUMN_UPDATED_AT = "updated_at"
        const val MAX_TITLE_LENGTH = MemoryInputValidator.MAX_TITLE_LENGTH
        const val MAX_DETAIL_LENGTH = MemoryInputValidator.MAX_DETAIL_LENGTH
        const val FIELD_TITLE = "memory.title"
        const val FIELD_DETAIL = "memory.detail"
    }
}

private fun createMemoryTable(db: SQLiteDatabase, tableName: String) {
    db.execSQL(
        """
        CREATE TABLE $tableName (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            title TEXT NOT NULL CHECK(length(title) BETWEEN 1 AND 700),
            detail TEXT NOT NULL CHECK(length(detail) BETWEEN 1 AND 17000),
            created_at INTEGER NOT NULL,
            updated_at INTEGER NOT NULL
        )
        """.trimIndent(),
    )
}

private fun countRows(db: SQLiteDatabase, tableName: String): Long =
    db.rawQuery("SELECT COUNT(*) FROM $tableName", null).use { cursor ->
        check(cursor.moveToFirst())
        cursor.getLong(0)
    }
