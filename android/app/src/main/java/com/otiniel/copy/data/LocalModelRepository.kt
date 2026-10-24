package com.otiniel.copy.data

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

data class LocalModelStatus(
    val installed: Boolean,
    val sizeBytes: Long = 0L,
    val message: String? = null,
)

/**
 * Manages a user-selected LiteRT-LM model in private app storage.
 * The app never downloads a model automatically and does not need Internet permission.
 */
class LocalModelRepository(private val context: Context) {
    private val modelDirectory = File(context.filesDir, MODEL_DIRECTORY)
    private val modelFile = File(modelDirectory, MODEL_FILENAME)

    fun status(): LocalModelStatus {
        if (!modelFile.isFile) return LocalModelStatus(installed = false)
        val size = modelFile.length()
        if (size !in MIN_MODEL_BYTES..MAX_MODEL_BYTES || !hasValidMagic(modelFile)) {
            return LocalModelStatus(
                installed = false,
                sizeBytes = size,
                message = "O arquivo local não parece ser um modelo LiteRT-LM válido. Importe novamente.",
            )
        }
        return LocalModelStatus(installed = true, sizeBytes = size)
    }

    fun installedModelPath(): String? = status().takeIf { it.installed }?.let { modelFile.absolutePath }

    @Throws(IOException::class, IllegalArgumentException::class)
    fun importModel(uri: Uri): LocalModelStatus {
        if (!modelDirectory.exists() && !modelDirectory.mkdirs()) {
            throw IOException("Não foi possível criar o diretório privado do modelo.")
        }

        val temporaryFile = File(modelDirectory, "$MODEL_FILENAME.part")
        try {
            val input = context.contentResolver.openInputStream(uri)
                ?: throw IOException("O Android não conseguiu abrir o arquivo selecionado.")
            var totalBytes = 0L
            input.use { source ->
                FileOutputStream(temporaryFile).use { destination ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        val read = source.read(buffer)
                        if (read < 0) break
                        totalBytes += read
                        require(totalBytes <= MAX_MODEL_BYTES) {
                            "O modelo excede o limite de " + (MAX_MODEL_BYTES / (1024 * 1024)) + " MB."
                        }
                        destination.write(buffer, 0, read)
                    }
                    destination.fd.sync()
                }
            }

            require(totalBytes in MIN_MODEL_BYTES..MAX_MODEL_BYTES) {
                "O arquivo está vazio, incompleto ou tem tamanho incompatível com este importador."
            }
            require(hasValidMagic(temporaryFile)) {
                "O arquivo não tem a assinatura LITERTLM esperada. Selecione um arquivo .litertlm compatível."
            }

            if (modelFile.exists() && !modelFile.delete()) {
                throw IOException("Não foi possível substituir o modelo anterior.")
            }
            if (!temporaryFile.renameTo(modelFile)) {
                throw IOException("Não foi possível finalizar a cópia do modelo.")
            }
            return status()
        } catch (error: Exception) {
            temporaryFile.delete()
            throw error
        }
    }

    fun removeModel(): Boolean {
        if (!modelFile.exists()) return true
        return modelFile.delete()
    }

    private fun hasValidMagic(file: File): Boolean {
        if (!file.isFile || file.length() < MODEL_MAGIC.size) return false
        return try {
            FileInputStream(file).use { input ->
                val actual = ByteArray(MODEL_MAGIC.size)
                var offset = 0
                while (offset < actual.size) {
                    val read = input.read(actual, offset, actual.size - offset)
                    if (read < 0) return false
                    offset += read
                }
                actual.contentEquals(MODEL_MAGIC)
            }
        } catch (_: IOException) {
            false
        }
    }

    companion object {
        const val MODEL_FILENAME = "copy-assistant.litertlm"
        private const val MODEL_DIRECTORY = "models"
        private const val MIN_MODEL_BYTES = 1_000_000L
        private const val MAX_MODEL_BYTES = 1_500L * 1024L * 1024L
        private val MODEL_MAGIC = "LITERTLM".toByteArray(Charsets.US_ASCII)

        // Open model is free to download; model licensing is Apache-2.0.
        const val MODEL_DOWNLOAD_URL =
            "https://huggingface.co/litert-community/Qwen3-0.6B/resolve/main/Qwen3-0.6B_dynamic_wi4b32_afp32.litertlm"
        const val MODEL_LICENSE_URL = "https://huggingface.co/litert-community/Qwen3-0.6B"
        const val MODEL_SIZE_LABEL = "aproximadamente 329 MB"
    }
}
