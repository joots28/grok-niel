package com.otiniel.copy

import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import java.util.Locale

class MainActivity : ComponentActivity() {
    private var textToSpeech: TextToSpeech? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        textToSpeech = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech?.setLanguage(Locale("pt", "BR"))
            }
        }

        val database = CopyDatabase(applicationContext)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFF1677FF),
                    secondary = Color(0xFF45D9FF),
                    background = Color(0xFF07090F),
                    onBackground = Color(0xFFF2F6FF),
                    surface = Color(0xFF131A26),
                    onSurface = Color(0xFFF2F6FF)
                )
            ) {
                CopyApp(
                    database = database,
                    onSpeak = { text ->
                        textToSpeech?.speak(
                            text,
                            TextToSpeech.QUEUE_FLUSH,
                            null,
                            "copy-last-response"
                        )
                    }
                )
            }
        }
    }

    override fun onDestroy() {
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null
        super.onDestroy()
    }
}
