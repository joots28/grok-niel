package com.otiniel.copy

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.net.URI
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

data class ModelProviderConfig(
    val baseUrl: String,
    val model: String,
    val apiKey: String
)

data class StoredProviderSettings(
    val baseUrl: String,
    val model: String,
    val configured: Boolean
)

/**
 * Validates the user-selected OpenAI-compatible chat-completions endpoint.
 * HTTPS is required because the credential is sent as a bearer token.
 */
object ProviderConfigValidator {
    fun normalizeBaseUrl(raw: String): String {
        val value = raw.trim().trimEnd('/')
        require(value.isNotBlank()) { "Informe a URL base do provedor." }

        val uri = try {
            URI(value)
        } catch (_: Exception) {
            throw IllegalArgumentException("A URL do provedor é inválida.")
        }

        require(uri.scheme.equals("https", ignoreCase = true)) {
            "Por segurança, o provedor precisa usar HTTPS."
        }
        require(!uri.host.isNullOrBlank()) { "A URL precisa conter um domínio válido." }
        require(uri.rawUserInfo == null) { "Não coloque usuário ou senha dentro da URL." }
        require(uri.rawQuery == null && uri.rawFragment == null) {
            "A URL base não pode conter parâmetros ou fragmentos."
        }

        return URI(
            "https",
            null,
            uri.host,
            uri.port,
            uri.path.trimEnd('/').ifBlank { "" },
            null,
            null
        ).toASCIIString().trimEnd('/')
    }

    fun normalizeModel(raw: String): String {
        val model = raw.trim()
        require(model.isNotBlank()) { "Informe o identificador do modelo." }
        require(model.length <= 128 && model.matches(Regex("[A-Za-z0-9._:/-]+"))) {
            "O identificador do modelo contém caracteres inválidos."
        }
        return model
    }

    fun completionUrl(baseUrl: String): URLParts {
        val normalized = normalizeBaseUrl(baseUrl)
        val uri = URI(normalized)
        val path = uri.path.orEmpty().trimEnd('/')
        val completionPath = when {
            path.endsWith("/chat/completions") -> path
            path.matches(Regex(".*/v[0-9]+")) -> "$path/chat/completions"
            path.isBlank() -> "/v1/chat/completions"
            else -> "$path/v1/chat/completions"
        }
        return URLParts(URI("https", null, uri.host, uri.port, completionPath, null, null).toASCIIString())
    }
}

data class URLParts(val value: String)

/**
 * Stores provider metadata in app-private preferences and encrypts the API key
 * with an AES-GCM key kept by Android Keystore. The key is never written in
 * plaintext to SharedPreferences or to the repository.
 */
class ProviderConfigStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    fun loadSettings(): StoredProviderSettings? {
        val baseUrl = preferences.getString(KEY_BASE_URL, null) ?: return null
        val model = preferences.getString(KEY_MODEL, null) ?: return null
        val configured = !preferences.getString(KEY_ENCRYPTED_API_KEY, null).isNullOrBlank()
        return StoredProviderSettings(baseUrl, model, configured)
    }

    fun hasConfig(): Boolean {
        val settings = loadSettings() ?: return false
        return settings.configured && settings.baseUrl.isNotBlank() && settings.model.isNotBlank()
    }

    fun isEnabled(): Boolean = preferences.getBoolean(KEY_ENABLED, false) && hasConfig()

    fun setEnabled(enabled: Boolean) {
        require(!enabled || hasConfig()) { "Salve um provedor válido antes de ativar a IA externa." }
        preferences.edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    fun save(baseUrl: String, model: String, apiKey: String) {
        val normalizedUrl = ProviderConfigValidator.normalizeBaseUrl(baseUrl)
        val normalizedModel = ProviderConfigValidator.normalizeModel(model)
        val candidateKey = if (apiKey.isBlank()) readSavedKey() else apiKey.trim()
        require(!candidateKey.isNullOrBlank()) {
            "Informe a chave de API. Deixe o campo vazio apenas para manter uma chave já salva."
        }
        require(candidateKey.length <= 4096 && candidateKey.none { it == '\n' || it == '\r' }) {
            "A chave de API tem formato inválido."
        }

        val encrypted = encrypt(candidateKey)
        preferences.edit()
            .putString(KEY_BASE_URL, normalizedUrl)
            .putString(KEY_MODEL, normalizedModel)
            .putString(KEY_ENCRYPTED_API_KEY, encrypted)
            .apply()
    }

    fun load(): ModelProviderConfig? {
        val settings = loadSettings() ?: return null
        val encryptedKey = preferences.getString(KEY_ENCRYPTED_API_KEY, null) ?: return null
        val key = decrypt(encryptedKey)
        if (key.isBlank()) return null
        return ModelProviderConfig(settings.baseUrl, settings.model, key)
    }

    fun clear() {
        preferences.edit().clear().apply()
        try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            keyStore.deleteEntry(KEY_ALIAS)
        } catch (_: Exception) {
            // Removing app-private metadata still prevents this app from using the saved credential.
        }
    }

    private fun readSavedKey(): String? {
        val encrypted = preferences.getString(KEY_ENCRYPTED_API_KEY, null) ?: return null
        return decrypt(encrypted)
    }

    private fun encrypt(plainText: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        val payload = cipher.iv + encrypted
        return Base64.encodeToString(payload, Base64.NO_WRAP)
    }

    private fun decrypt(encoded: String): String {
        val payload = Base64.decode(encoded, Base64.NO_WRAP)
        require(payload.size > GCM_IV_BYTES) { "A chave salva não pôde ser lida. Configure o provedor novamente." }
        val iv = payload.copyOfRange(0, GCM_IV_BYTES)
        val ciphertext = payload.copyOfRange(GCM_IV_BYTES, payload.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(GCM_TAG_BITS, iv))
        return String(cipher.doFinal(ciphertext), Charsets.UTF_8)
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }

    private companion object {
        const val PREFERENCES = "copy_model_provider"
        const val KEY_BASE_URL = "base_url"
        const val KEY_MODEL = "model"
        const val KEY_ENCRYPTED_API_KEY = "encrypted_api_key"
        const val KEY_ENABLED = "enabled"
        const val KEY_ALIAS = "copy_provider_api_key_v1"
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val GCM_IV_BYTES = 12
        const val GCM_TAG_BITS = 128
    }
}
