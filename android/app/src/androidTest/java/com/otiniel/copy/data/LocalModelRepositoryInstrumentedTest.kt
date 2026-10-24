package com.otiniel.copy.data

import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import java.io.FileOutputStream
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LocalModelRepositoryInstrumentedTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Test
    fun importsModelWithExpectedMagicIntoPrivateStorage() {
        val source = File.createTempFile("copy-model-test", ".litertlm", context.cacheDir)
        val repository = LocalModelRepository(context)
        try {
            FileOutputStream(source).use { output ->
                output.write("LITERTLM".toByteArray(Charsets.US_ASCII))
                output.write(ByteArray(1_000_000))
            }

            val status = repository.importModel(Uri.fromFile(source))

            assertTrue(status.installed)
            assertTrue(status.sizeBytes > 1_000_000L)
            assertTrue(File(repository.installedModelPath()!!).isFile)
        } finally {
            repository.removeModel()
            source.delete()
        }
    }

    @Test
    fun rejectsFileWithoutLitertLmMagic() {
        val source = File.createTempFile("copy-invalid-model", ".bin", context.cacheDir)
        val repository = LocalModelRepository(context)
        try {
            FileOutputStream(source).use { output -> output.write(ByteArray(1_000_008)) }

            var rejected = false
            try {
                repository.importModel(Uri.fromFile(source))
            } catch (_: IllegalArgumentException) {
                rejected = true
            }

            assertTrue(rejected)
            assertFalse(repository.status().installed)
        } finally {
            repository.removeModel()
            source.delete()
        }
    }
}
