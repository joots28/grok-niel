package com.otiniel.copy.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * Private, app-local storage for memories explicitly saved by the user.
 *
 * This database is not synchronized and is not encrypted at the SQLite layer.
 * Keep sensitive data out of logs and exports; add a reviewed encryption design
 * before treating this as encrypted-at-rest storage.
 */
class MemoryRepository(context: Context) {
    private val database = MemoryDatabase(context.applicationContext)

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

    fun close() {
        database.close()
    }

    private class MemoryDatabase(context: Context) : SQLiteOpenHelper(
        context,
        DATABASE_NAME,
        null,
        DATABASE_VERSION,
    ) {
        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE $TABLE_MEMORIES (
                    $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                    $COLUMN_TITLE TEXT NOT NULL
                        CHECK(length(trim($COLUMN_TITLE)) BETWEEN 1 AND $MAX_TITLE_LENGTH),
                    $COLUMN_DETAIL TEXT NOT NULL
                        CHECK(length(trim($COLUMN_DETAIL)) BETWEEN 1 AND $MAX_DETAIL_LENGTH),
                    $COLUMN_CREATED_AT INTEGER NOT NULL,
                    $COLUMN_UPDATED_AT INTEGER NOT NULL
                )
                """.trimIndent(),
            )
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            // Never drop user memories to make a schema change. Add explicit,
            // version-by-version migrations here before incrementing the version.
            error("No migration path is defined from database version $oldVersion to $newVersion.")
        }
    }

    private companion object {
        const val DATABASE_NAME = "copy-local.db"
        const val DATABASE_VERSION = 1
        const val TABLE_MEMORIES = "memories"
        const val COLUMN_ID = "id"
        const val COLUMN_TITLE = "title"
        const val COLUMN_DETAIL = "detail"
        const val COLUMN_CREATED_AT = "created_at"
        const val COLUMN_UPDATED_AT = "updated_at"
        const val MAX_TITLE_LENGTH = MemoryInputValidator.MAX_TITLE_LENGTH
        const val MAX_DETAIL_LENGTH = MemoryInputValidator.MAX_DETAIL_LENGTH
    }
}
