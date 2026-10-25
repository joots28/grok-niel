package com.otiniel.copy

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

private val CopyBackground = Color(0xFF070910)
private val CopySurface = Color(0xFF111522)
private val CopyAccent = Color(0xFF91A0FF)
private val CopyText = Color(0xFFF2F4FF)
private val CopyMuted = Color(0xFF9CA5BC)

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
                    primary = CopyAccent,
                    onPrimary = Color(0xFF080A12),
                    background = CopyBackground,
                    onBackground = CopyText,
                    surface = CopySurface,
                    onSurface = CopyText
                )
            ) {
                CopyScreen(
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

@Composable
private fun CopyScreen(database: CopyDatabase, onSpeak: (String) -> Unit) {
    val context = LocalContext.current
    val messages = remember { mutableStateListOf<ChatMessage>() }
    val scope = rememberCoroutineScope()
    var input by rememberSaveable { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }

    LaunchedEffect(database) {
        val saved = withContext(Dispatchers.IO) { database.getMessages() }
        messages.clear()
        messages.addAll(saved)
    }

    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val spokenText = result.data
            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()
        if (!spokenText.isNullOrBlank()) input = spokenText
    }

    val startSpeech: () -> Unit = {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-BR")
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Fale sua mensagem para o Copy")
        }
        try {
            speechLauncher.launch(intent)
        } catch (_: Exception) {
            Toast.makeText(
                context,
                "Reconhecimento de voz indisponível neste aparelho.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    val microphonePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startSpeech()
        else Toast.makeText(
            context,
            "Permissão de microfone não concedida.",
            Toast.LENGTH_SHORT
        ).show()
    }

    val sendMessage: () -> Unit = {
        val text = input.trim()
        if (text.isNotEmpty() && !isSending) {
            input = ""
            isSending = true
            scope.launch {
                try {
                    val timestamp = System.currentTimeMillis()
                    val userMessage = ChatMessage(
                        role = "user",
                        content = text,
                        createdAt = timestamp
                    )
                    val response = ChatMessage(
                        role = "assistant",
                        content = AssistantEngine.reply(text),
                        createdAt = timestamp + 1L
                    )
                    val storedPair = withContext(Dispatchers.IO) {
                        database.insertMessage(userMessage) to database.insertMessage(response)
                    }
                    messages.add(storedPair.first)
                    messages.add(storedPair.second)
                } catch (_: Exception) {
                    Toast.makeText(
                        context,
                        "Não foi possível salvar a mensagem local.",
                        Toast.LENGTH_SHORT
                    ).show()
                } finally {
                    isSending = false
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Limpar conversa?") },
            text = { Text("As mensagens locais desta conversa serão apagadas deste dispositivo.") },
            confirmButton = {
                TextButton(onClick = {
                    showClearDialog = false
                    scope.launch {
                        withContext(Dispatchers.IO) { database.clearMessages() }
                        messages.clear()
                    }
                }) { Text("Apagar") }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("Cancelar") }
            },
            containerColor = CopySurface,
            titleContentColor = CopyText,
            textContentColor = CopyMuted
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = CopyBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    modifier = Modifier.size(46.dp),
                    shape = RoundedCornerShape(15.dp),
                    color = CopyAccent
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "C",
                            color = Color(0xFF080A12),
                            fontSize = 25.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "COPY",
                        color = CopyText,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 3.sp
                    )
                    Text(
                        text = "Assistente pessoal · fundação local",
                        color = CopyMuted,
                        fontSize = 11.sp
                    )
                }
                TextButton(onClick = { showClearDialog = true }) {
                    Text("Limpar", color = CopyMuted)
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp)
                    .background(CopySurface, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .background(CopyAccent, CircleShape)
                )
                Text(
                    text = "MOTOR DE IA AINDA NÃO CONECTADO",
                    color = CopyMuted,
                    fontSize = 10.sp,
                    letterSpacing = 0.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(
                    start = 18.dp,
                    end = 18.dp,
                    top = 18.dp,
                    bottom = 20.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (messages.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(top = 32.dp, bottom = 20.dp),
                            horizontalAlignment = Alignment.Start,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Sua central.\nSob seu controle.",
                                color = CopyText,
                                fontSize = 31.sp,
                                lineHeight = 37.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "A fundação já salva a conversa no aparelho. " +
                                    "Comandos locais básicos funcionam; o motor inteligente será conectado numa etapa separada.",
                                color = CopyMuted,
                                fontSize = 14.sp,
                                lineHeight = 21.sp
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { input = "ajuda" }) { Text("Ajuda local") }
                                TextButton(onClick = { input = "Que horas são?" }) {
                                    Text("Que horas são?", color = CopyAccent)
                                }
                            }
                        }
                    }
                }
                items(messages, key = { it.id }) { message ->
                    MessageBubble(message = message, onSpeak = onSpeak)
                }
                if (isSending) {
                    item {
                        Text(
                            text = "Salvando conversa local…",
                            color = CopyMuted,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CopyBackground)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Fale com o Copy…", color = CopyMuted) },
                    shape = RoundedCornerShape(18.dp),
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { sendMessage() }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CopyAccent,
                        unfocusedBorderColor = Color(0xFF30364A),
                        focusedTextColor = CopyText,
                        unfocusedTextColor = CopyText,
                        cursorColor = CopyAccent
                    )
                )
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    TextButton(
                        onClick = {
                            microphonePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) { Text("Voz", color = CopyAccent, fontSize = 12.sp) }
                    Button(
                        onClick = { sendMessage() },
                        enabled = input.isNotBlank() && !isSending,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) { Text("Enviar", fontSize = 12.sp) }
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(message: ChatMessage, onSpeak: (String) -> Unit) {
    val isUser = message.role == "user"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(if (isUser) 0.88f else 0.96f),
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomEnd = if (isUser) 5.dp else 18.dp,
                bottomStart = if (isUser) 18.dp else 5.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (isUser) Color(0xFF202A4B) else CopySurface
            )
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 15.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Text(
                    text = if (isUser) "VOCÊ" else "COPY · LOCAL",
                    color = if (isUser) CopyAccent else CopyMuted,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = message.content,
                    color = CopyText,
                    fontSize = 14.sp,
                    lineHeight = 21.sp
                )
                if (!isUser) {
                    TextButton(
                        onClick = { onSpeak(message.content) },
                        contentPadding = PaddingValues(0.dp)
                    ) { Text("Ouvir resposta", color = CopyAccent, fontSize = 11.sp) }
                }
            }
        }
    }
}
