package com.otiniel.copy.data

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Encrypts selected text fields with Android's built-in AES-GCM.
 * The key is generated and retained by Android Keystore, never written to a file or source.
 *
 * Protects field contents, not the SQLite file structure or non-text metadata.
 */
internal object LocalFieldCipher {
    const val PREFIX = "enc:v1:"
    private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
    private const val KEY_ALIAS = "copy.local.content.aesgcm.v1"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val IV_LENGTH_BYTES = 12
    private const val TAG_LENGTH_BITS = 128
    private const val AAD_PREFIX = "COPY-LOCAL-FIELD-v1:"
    private val keyLock = Any()

    @Volatile
    private var cachedKey: SecretKey? = null

    fun encrypt(plainText: String, field: String): String {
        require(field.isNotBlank()) { "A field identifier is required." }
        val plainBytes = plainText.toByteArray(StandardCharsets.UTF_8)
        var encryptedBytes: ByteArray? = null
        var iv: ByteArray? = null
        var payload: ByteArray? = null
        try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
            cipher.updateAAD((AAD_PREFIX + field).toByteArray(StandardCharsets.UTF_8))
            iv = cipher.iv
            require(iv.size == IV_LENGTH_BYTES) { "Unexpected AES-GCM IV length." }
            encryptedBytes = cipher.doFinal(plainBytes)
            payload = ByteArray(1 + iv.size + encryptedBytes.size)
            payload[0] = iv.size.toByte()
            System.arraycopy(iv, 0, payload, 1, iv.size)
            System.arraycopy(encryptedBytes, 0, payload, 1 + iv.size, encryptedBytes.size)
            return PREFIX + Base64.encodeToString(payload, Base64.NO_WRAP)
        } finally {
            plainBytes.fill(0)
            encryptedBytes?.fill(0)
            iv?.fill(0)
            payload?.fill(0)
        }
    }

    /**
     * Plain values remain readable for legacy compatibility; versioned migrations re-encrypt them.
     */
    fun decrypt(storedValue: String, field: String): String {
        if (!storedValue.startsWith(PREFIX)) return storedValue
        val payload = try {
            Base64.decode(storedValue.substring(PREFIX.length), Base64.NO_WRAP)
        } catch (error: IllegalArgumentException) {
            throw IllegalStateException("Stored encrypted content is malformed.", error)
        }
        var iv: ByteArray? = null
        var encryptedBytes: ByteArray? = null
        var plainBytes: ByteArray? = null
        try {
            require(payload.isNotEmpty()) { "Stored encrypted content is empty." }
            val ivLength = payload[0].toInt() and 0xff
            require(ivLength == IV_LENGTH_BYTES) { "Stored encrypted content has an invalid IV." }
            require(payload.size > 1 + ivLength + TAG_LENGTH_BITS / 8) {
                "Stored encrypted content is too short."
            }
            iv = payload.copyOfRange(1, 1 + ivLength)
            encryptedBytes = payload.copyOfRange(1 + ivLength, payload.size)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                getExistingKey() ?: throw IllegalStateException(
                    "The Android Keystore key is missing. Saved content was not overwritten.",
                ),
                GCMParameterSpec(TAG_LENGTH_BITS, iv),
            )
            cipher.updateAAD((AAD_PREFIX + field).toByteArray(StandardCharsets.UTF_8))
            plainBytes = cipher.doFinal(encryptedBytes)
            return String(plainBytes, StandardCharsets.UTF_8)
        } finally {
            payload.fill(0)
            iv?.fill(0)
            encryptedBytes?.fill(0)
            plainBytes?.fill(0)
        }
    }

    private fun getOrCreateKey(): SecretKey {
        getExistingKey()?.let { return it }
        synchronized(keyLock) {
            getExistingKey()?.let { return it }
            val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
            generator.init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .setRandomizedEncryptionRequired(true)
                    .build(),
            )
            return generator.generateKey().also { cachedKey = it }
        }
    }

    private fun getExistingKey(): SecretKey? {
        cachedKey?.let { return it }
        synchronized(keyLock) {
            cachedKey?.let { return it }
            val store = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
            if (!store.containsAlias(KEY_ALIAS)) return null
            val key = store.getKey(KEY_ALIAS, null) as? SecretKey
                ?: throw IllegalStateException("The Android Keystore entry for local content has an invalid type.")
            cachedKey = key
            return key
        }
    }
}
