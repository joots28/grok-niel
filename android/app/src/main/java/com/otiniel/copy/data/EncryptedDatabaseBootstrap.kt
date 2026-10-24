package com.otiniel.copy.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase as PlainSQLiteDatabase
import java.io.File
import java.io.IOException
import net.zetetic.database.sqlcipher.SQLiteDatabase as CipherSQLiteDatabase

internal data class PreparedEncryptedDatabase(
    val databaseName: String,
    val passphrase: ByteArray,
)

/**
 * Creates encrypted databases in a staging file, verifies them, then atomically promotes them.
 * A legacy plaintext database is deleted only after the encrypted copy has been reopened and
 * validated. Any failure before that point leaves the legacy file untouched.
 */
internal object EncryptedDatabaseBootstrap {
    @Volatile
    private var nativeLibraryLoaded = false

    fun encryptedNameFor(legacyName: String): String =
        legacyName.removeSuffix(".db") + ".encrypted.db"

    fun stagingNameFor(legacyName: String): String =
        legacyName.removeSuffix(".db") + ".encryption-staging.db"

    fun prepare(
        context: Context,
        legacyDatabaseName: String,
        databaseId: String,
        schemaVersion: Int,
        createSchema: (CipherSQLiteDatabase) -> Unit,
        migrateLegacy: (PlainSQLiteDatabase, CipherSQLiteDatabase) -> Unit,
        validateSchema: (CipherSQLiteDatabase) -> Unit,
    ): PreparedEncryptedDatabase {
        ensureNativeLibraryLoaded()

        val encryptedName = encryptedNameFor(legacyDatabaseName)
        val stagingName = stagingNameFor(legacyDatabaseName)
        val encryptedFile = context.getDatabasePath(encryptedName)
        val stagingFile = context.getDatabasePath(stagingName)
        encryptedFile.parentFile?.mkdirs()

        val encryptedStorageExists = encryptedFile.exists() || stagingFile.exists()
        val passphrase = DatabasePassphraseStore.getOrCreate(
            context = context,
            databaseId = databaseId,
            encryptedDatabaseExists = encryptedStorageExists,
        )

        try {
            ensureEncryptedDatabase(
                context = context,
                legacyDatabaseName = legacyDatabaseName,
                encryptedName = encryptedName,
                stagingName = stagingName,
                schemaVersion = schemaVersion,
                passphrase = passphrase,
                createSchema = createSchema,
                migrateLegacy = migrateLegacy,
                validateSchema = validateSchema,
            )
            return PreparedEncryptedDatabase(encryptedName, passphrase)
        } catch (error: Throwable) {
            passphrase.fill(0)
            throw error
        }
    }

    private fun ensureNativeLibraryLoaded() {
        if (nativeLibraryLoaded) return
        synchronized(this) {
            if (!nativeLibraryLoaded) {
                System.loadLibrary("sqlcipher")
                nativeLibraryLoaded = true
            }
        }
    }

    private fun ensureEncryptedDatabase(
        context: Context,
        legacyDatabaseName: String,
        encryptedName: String,
        stagingName: String,
        schemaVersion: Int,
        passphrase: ByteArray,
        createSchema: (CipherSQLiteDatabase) -> Unit,
        migrateLegacy: (PlainSQLiteDatabase, CipherSQLiteDatabase) -> Unit,
        validateSchema: (CipherSQLiteDatabase) -> Unit,
    ) {
        val encryptedFile = context.getDatabasePath(encryptedName)
        val legacyFile = context.getDatabasePath(legacyDatabaseName)
        val stagingFile = context.getDatabasePath(stagingName)

        if (encryptedFile.exists()) {
            verifyEncryptedFile(encryptedFile, passphrase, schemaVersion, validateSchema)
            if (stagingFile.exists()) context.deleteDatabase(stagingName)
            removeLegacyPlaintextIfPresent(context, legacyDatabaseName, legacyFile)
            return
        }

        if (stagingFile.exists()) {
            if (legacyFile.exists()) {
                // A failed migration can be repeated from the untouched plaintext source.
                if (!context.deleteDatabase(stagingName)) {
                    throw IOException("Could not remove an incomplete encrypted staging database.")
                }
            } else {
                // A crash during first-run initialization can leave an empty staged database.
                val stageIsValid = try {
                    verifyEncryptedFile(stagingFile, passphrase, schemaVersion, validateSchema)
                    true
                } catch (_: Exception) {
                    false
                }
                if (stageIsValid) {
                    promoteStagingFile(stagingFile, encryptedFile)
                    verifyEncryptedFile(encryptedFile, passphrase, schemaVersion, validateSchema)
                    return
                }
                if (!context.deleteDatabase(stagingName)) {
                    throw IOException("Could not remove an invalid encrypted staging database.")
                }
            }
        }

        val source = if (legacyFile.exists()) {
            PlainSQLiteDatabase.openDatabase(
                legacyFile.absolutePath,
                null,
                PlainSQLiteDatabase.OPEN_READONLY,
            )
        } else {
            null
        }

        try {
            if (source != null && source.version != schemaVersion) {
                throw IllegalStateException(
                    "The existing local database has an unsupported version; it was left untouched.",
                )
            }

            val target = CipherSQLiteDatabase.openOrCreateDatabase(
                stagingFile,
                passphrase,
                null,
                null,
                null,
            )
            try {
                target.setForeignKeyConstraintsEnabled(true)
                createSchema(target)
                if (source != null) migrateLegacy(source, target)
                target.version = schemaVersion
                validateSchema(target)
            } finally {
                target.close()
            }
        } catch (error: Throwable) {
            if (stagingFile.exists() && legacyFile.exists()) {
                context.deleteDatabase(stagingName)
            }
            throw error
        } finally {
            source?.close()
        }

        verifyEncryptedFile(stagingFile, passphrase, schemaVersion, validateSchema)
        promoteStagingFile(stagingFile, encryptedFile)
        verifyEncryptedFile(encryptedFile, passphrase, schemaVersion, validateSchema)
        removeLegacyPlaintextIfPresent(context, legacyDatabaseName, legacyFile)
    }

    private fun promoteStagingFile(stagingFile: File, encryptedFile: File) {
        if (encryptedFile.exists()) {
            throw IOException("An encrypted database already exists; refusing to overwrite it.")
        }
        if (!stagingFile.renameTo(encryptedFile)) {
            throw IOException("Could not atomically promote the encrypted database.")
        }
    }

    private fun verifyEncryptedFile(
        file: File,
        passphrase: ByteArray,
        schemaVersion: Int,
        validateSchema: (CipherSQLiteDatabase) -> Unit,
    ) {
        val database = CipherSQLiteDatabase.openDatabase(
            file.absolutePath,
            passphrase,
            null,
            CipherSQLiteDatabase.OPEN_READWRITE,
            null,
            null,
        )
        try {
            check(database.version == schemaVersion) {
                "Encrypted database schema version does not match this application."
            }
            validateSchema(database)
        } finally {
            database.close()
        }
    }

    private fun removeLegacyPlaintextIfPresent(
        context: Context,
        legacyDatabaseName: String,
        legacyFile: File,
    ) {
        if (legacyFile.exists() && !context.deleteDatabase(legacyDatabaseName)) {
            throw IOException(
                "The encrypted data was verified, but the old plaintext database could not yet be removed.",
            )
        }
    }

    internal fun deleteForTesting(context: Context, legacyDatabaseName: String, databaseId: String) {
        context.deleteDatabase(legacyDatabaseName)
        context.deleteDatabase(encryptedNameFor(legacyDatabaseName))
        context.deleteDatabase(stagingNameFor(legacyDatabaseName))
        DatabasePassphraseStore.deleteForTesting(context, databaseId)
    }
}
