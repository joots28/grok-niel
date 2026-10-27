package com.otiniel.copy

import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CopyRustBridgeInstrumentedTest {
    @Test
    fun nativeLibraryLoadsAndReturnsDeviceLocalResponses() {
        assertTrue("The packaged Rust JNI library should load on Android.", CopyRustBridge.isAvailable())

        val fixedTime = LocalDateTime.of(2026, 10, 27, 14, 5, 0)
        assertEquals("Agora são 14:05.", AssistantEngine.reply("Que horas são?", fixedTime))
        assertEquals("Hoje é 27/10/2026.", AssistantEngine.reply("Qual é a data?", fixedTime))
    }

    @Test
    fun unknownPromptDoesNotInventAnOfflineAiAnswer() {
        assertTrue(
            AssistantEngine.reply("Explique computação quântica")
                .contains("núcleo de IA ainda não está conectado")
        )
    }
}
