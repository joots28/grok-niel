package com.otiniel.copy.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Push-to-talk using Android's on-device recognizer only. It deliberately does not fall back
 * to a potentially network-backed recognizer when local recognition is unavailable.
 */
@Composable
fun OnDeviceVoiceInput(
    modifier: Modifier = Modifier,
    enabled: Boolean,
    onRecognized: (String) -> Unit,
    onListeningChanged: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    var activeRecognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }
    var isListening by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    fun startRecognition() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            statusMessage = "Reconhecimento offline exige Android 12 ou posterior."
            return
        }
        if (!SpeechRecognizer.isOnDeviceRecognitionAvailable(context)) {
            statusMessage = "Este aparelho não oferece reconhecimento de fala local compatível."
            return
        }

        val recognizer = try {
            SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
        } catch (_: RuntimeException) {
            statusMessage = "Não foi possível iniciar o reconhecimento offline."
            return
        }

        activeRecognizer = recognizer
        isListening = true
        onListeningChanged(true)
        statusMessage = "Escutando localmente…"

        recognizer.setRecognitionListener(object : RecognitionListener {
            private fun finish(message: String) {
                statusMessage = message
                isListening = false
                onListeningChanged(false)
                if (activeRecognizer === recognizer) activeRecognizer = null
            }

            override fun onReadyForSpeech(params: Bundle?) {
                statusMessage = "Fale agora…"
            }

            override fun onBeginningOfSpeech() {
                statusMessage = "Ouvindo…"
            }

            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit

            override fun onEndOfSpeech() {
                statusMessage = "Interpretando fala localmente…"
            }

            override fun onError(error: Int) {
                finish(speechRecognitionErrorMessage(error))
            }

            override fun onResults(results: Bundle?) {
                val spokenText = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    ?.trim()
                    .orEmpty()

                if (spokenText.isBlank()) {
                    finish("Não consegui entender a fala. Tente novamente.")
                } else {
                    finish("Voz reconhecida.")
                    onRecognized(spokenText)
                }
            }

            override fun onPartialResults(partialResults: Bundle?) = Unit
            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        })

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-BR")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        }
        try {
            recognizer.startListening(intent)
        } catch (_: RuntimeException) {
            isListening = false
            onListeningChanged(false)
            statusMessage = "Não foi possível começar a escutar."
            if (activeRecognizer === recognizer) activeRecognizer = null
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) startRecognition()
        else statusMessage = "Permissão do microfone não concedida."
    }

    DisposableEffect(activeRecognizer) {
        val currentRecognizer = activeRecognizer
        onDispose {
            if (currentRecognizer != null) {
                runCatching { currentRecognizer.cancel() }
                runCatching { currentRecognizer.destroy() }
            }
        }
    }

    Column(
        modifier = modifier.width(56.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        IconButton(
            enabled = enabled || isListening,
            onClick = {
                if (isListening) {
                    activeRecognizer?.let { runCatching { it.cancel() } }
                    activeRecognizer = null
                    isListening = false
                    onListeningChanged(false)
                    statusMessage = "Escuta cancelada."
                } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                    statusMessage = "Reconhecimento offline exige Android 12 ou posterior."
                } else if (!SpeechRecognizer.isOnDeviceRecognitionAvailable(context)) {
                    statusMessage = "Este aparelho não oferece reconhecimento de fala local compatível."
                } else if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) ==
                    PackageManager.PERMISSION_GRANTED
                ) {
                    startRecognition()
                } else {
                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            },
        ) {
            Icon(
                imageVector = if (isListening) Icons.Rounded.MicOff else Icons.Rounded.Mic,
                contentDescription = if (isListening) "Parar escuta" else "Falar com Luo",
            )
        }
        statusMessage?.let {
            Text(
                text = it,
                fontSize = 8.sp,
                lineHeight = 10.sp,
                textAlign = TextAlign.Center,
                maxLines = 3,
            )
        }
    }
}

internal fun speechRecognitionErrorMessage(errorCode: Int): String = when (errorCode) {
    SpeechRecognizer.ERROR_AUDIO -> "O Android não conseguiu capturar o áudio."
    SpeechRecognizer.ERROR_CLIENT -> "A sessão de voz foi interrompida. Tente novamente."
    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permissão de microfone necessária."
    SpeechRecognizer.ERROR_NETWORK,
    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "O serviço de reconhecimento local falhou. Tente novamente."
    SpeechRecognizer.ERROR_NO_MATCH -> "Não consegui entender a fala. Tente novamente."
    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "O reconhecimento de voz já está ocupado."
    SpeechRecognizer.ERROR_SERVER -> "O mecanismo de reconhecimento retornou um erro."
    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Nenhuma fala foi detectada."
    else -> "Falha no reconhecimento de voz (código $errorCode)."
}
