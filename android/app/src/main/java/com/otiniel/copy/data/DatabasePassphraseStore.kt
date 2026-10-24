package com.otiniel.copy.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Stores a random SQLCipher passphrase wrapped by a key that never leaves Android Keystore.
 * The wrapped envelope is placed in noBackupFilesDir so key material is not expected to be
 * restored independently by Android backup.
 */
internal object DatabasePassphraseStore {
    private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val ENVELOPE_MAGIC = 0x4350594B
    private const val ENVELOPE_VERSION = 1
    private const val PASSPHRASE_BYTES = 32
    private const val GCM_TAG_BITS = 128

    fun getOrCreate(context: Context, databaseId: String, encryptedDatabaseExists: Boolean): ByteArray {
        val token = digestToken(databaseId)
        val alias = "copy.db.wrap." + token
        val envelope = File(context.noBackupFilesDir, "copy-db-key-" + token + ".bin")
        val keyStore = loadKeyStore()

        if (envelope.exists()) {
            val wrappingKey = keyStore.getKey(alias, null) as? SecretKey
                ?: throw IllegalStateException(
                    "The local database key is missing from Android Keystore. Existing data was preserved.",
                )
            return readPassphrase(envelope, databaseId, wrappingKey)
        }

        if (encryptedDatabaseExists) {
            throw IllegalStateException(
                "The encrypted database exists but its protected key envelope is missing. Existing files were preserved.",
            )
        }

        // No encrypted database has been committed yet, so a leftover alias can safely be replaced.
        if (keyStore.containsAlias(alias)) keyStore.deleteEntry(alias)
        val wrappingKey = createWrappingKey(alias)
        val passphrase = ByteArray(PASSPHRASE_BYTES)
        SecureRandom().nextBytes(passphrase)

        try {
            writePassphrase(envelope, databaseId, wrappingKey, passphrase)
            return passphrase
        } catch (error: Throwable) {
            passphrase.fill(0)
            runCatching { keyStore.deleteEntry(alias) }
            throw error
        }
    }

    private fun loadKeyStore(): KeyStore =
        KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }

    private fun createWrappingKey(alias: String): SecretKey {
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
        generator.init(
            KeyGenParameterSpec.Builder(
                alias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(true)
                .build(),
        )
        return generator.generateKey()
    }

    private fun writePassphrase(
        file: File,
        databaseId: String,
        wrappingKey: SecretKey,
        passphrase: ByteArray,
    ) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, wrappingKey)
        cipher.updateAAD(databaseId.toByteArray(StandardCharsets.UTF_8))
        val encrypted = cipher.doFinal(passphrase)
        val iv = cipher.iv
        val atomicFile = AtomicFile(file)
        var stream: FileOutputStream? = null

        try {
            val outputStream = atomicFile.startWrite()
            stream = outputStream
            val output = DataOutputStream(outputStream)
            output.writeInt(ENVELOPE_MAGIC)
            output.writeInt(ENVELOPE_VERSION)
            output.writeInt(iv.size)
            output.write(iv)
            output.writeInt(encrypted.size)
            output.write(encrypted)
            output.flush()
            atomicFile.finishWrite(outputStream)
            stream = null
        } catch (error: Throwable) {
            stream?.let { atomicFile.failWrite(it) }
            throw error
        } finally {
            encrypted.fill(0)
            iv.fill(0)
        }
    }

    private fun readPassphrase(file: File, databaseId: String, wrappingKey: SecretKey): ByteArray {
        val (iv, encrypted) = DataInputStream(file.inputStream().buffered()).use { input ->
            check(input.readInt() == ENVELOPE_MAGIC) { "Stored database key envelope has an invalid header." }
            check(input.readInt() == ENVELOPE_VERSION) { "Stored database key envelope version is unsupported." }
            val ivLength = input.readInt()
            check(ivLength in 12..16) { "Stored database key envelope has an invalid IV." }
            val nonce = ByteArray(ivLength)
            input.readFully(nonce)
            val encryptedLength = input.readInt()
            check(encryptedLength in (PASSPHRASE_BYTES + 16)..256) {
                "Stored database key envelope has an invalid payload length."
            }
            val ciphertext = ByteArray(encryptedLength)
            input.readFully(ciphertext)
            check(input.read() == -1) { "Stored database key envelope contains trailing data." }
            nonce to ciphertext
        }

        try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, wrappingKey, GCMParameterSpec(GCM_TAG_BITS, iv))
            cipher.updateAAD(databaseId.toByteArray(StandardCharsets.UTF_8))
            val passphrase = cipher.doFinal(encrypted)
            check(passphrase.size == PASSPHRASE_BYTES) {
                passphrase.fill(0)
                "Stored database passphrase has an invalid length."
            }
            return passphrase
        } catch (error: Throwable) {
            throw IllegalStateException(
                "The protected local database key could not be unlocked. Existing database files were preserved.",
                error,
            )
        } finally {
            iv.fill(0)
            encrypted.fill(0)
        }
    }

    private fun digestToken(value: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(StandardCharsets.UTF_8))
            .take(16)
            .joinToString("") { byte ->
                (byte.toInt() and 0xff).toString(16).padStart(2, '0')
            }

    internal fun deleteForTesting(context: Context, databaseId: String) {
        val token = digestToken(databaseId)
        File(context.noBackupFilesDir, "copy-db-key-" + token + ".bin").delete()
        val keyStore = loadKeyStore()
        val alias = "copy.db.wrap." + token
        if (keyStore.containsAlias(alias)) keyStore.deleteEntry(alias)
    }
}
