package com.otiniel.copy.ui

import android.speech.SpeechRecognizer
import org.junit.Assert.assertEquals
import org.junit.Test

class OnDeviceVoiceInputTest {
    @Test
    fun noMatchShowsActionableMessage() {
        assertEquals(
            "Não consegui entender a fala. Tente novamente.",
            speechRecognitionErrorMessage(SpeechRecognizer.ERROR_NO_MATCH),
        )
    }

    @Test
    fun speechTimeoutExplainsWhatHappened() {
        assertEquals(
            "Nenhuma fala foi detectada.",
            speechRecognitionErrorMessage(SpeechRecognizer.ERROR_SPEECH_TIMEOUT),
        )
    }

    @Test
    fun missingPermissionIsNotReportedAsModelFailure() {
        assertEquals(
            "Permissão de microfone necessária.",
            speechRecognitionErrorMessage(SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS),
        )
    }
}
