package com.otiniel.copy

import android.Manifest
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val CopyBackground = Color(0xFF07090F)
private val CopyMidnight = Color(0xFF0B1425)
private val CopySurface = Color(0xFF131A26)
private val CopyElevated = Color(0xFF1A2434)
private val CopyBlue = Color(0xFF1677FF)
private val CopyCyan = Color(0xFF45D9FF)
private val CopyText = Color(0xFFF2F6FF)
private val CopyMuted = Color(0xFF98A8BE)
private val CopyLine = Color(0xFF25364D)

@Composable
fun CopyApp(database: CopyDatabase, onSpeak: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val messages = remember { mutableStateListOf<ChatMessage>() }
    var page by rememberSaveable { mutableStateOf("Assistente") }
    var input by rememberSaveable { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    var clearDialog by remember { mutableStateOf(false) }
    var offline by rememberSaveable { mutableStateOf(false) }
    var memoryEnabled by rememberSaveable { mutableStateOf(true) }
    var syncEnabled by rememberSaveable { mutableStateOf(false) }
    var responseStyle by rememberSaveable { mutableStateOf("Equilibrado") }
    var selectedModule by rememberSaveable { mutableStateOf("") }
    val memories = remember { mutableStateListOf("Prefiro respostas objetivas e práticas.", "Quero priorizar funcionamento local e privacidade.") }

    LaunchedEffect(database) {
        val saved = withContext(Dispatchers.IO) { database.getMessages() }
        messages.clear()
        messages.addAll(saved)
    }

    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        if (!spokenText.isNullOrBlank()) input = spokenText
    }
    val startSpeech: () -> Unit = {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-BR")
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Fale sua mensagem para o Copy")
        }
        try {
            speechLauncher.launch(intent)
        } catch (_: Exception) {
            Toast.makeText(context, "Reconhecimento de voz indisponível neste aparelho.", Toast.LENGTH_SHORT).show()
        }
    }
    val microphonePermission = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startSpeech()
        else Toast.makeText(context, "Permissão de microfone não concedida.", Toast.LENGTH_SHORT).show()
    }

    val sendMessage: (String?) -> Unit = { overrideText ->
        val text = (overrideText ?: input).trim()
        if (text.isNotEmpty() && !sending) {
            input = ""
            sending = true
            page = "Conversas"
            scope.launch {
                try {
                    val timestamp = System.currentTimeMillis()
                    val user = ChatMessage(role = "user", content = text, createdAt = timestamp)
                    val answer = ChatMessage(role = "assistant", content = AssistantEngine.reply(text), createdAt = timestamp + 1L)
                    val stored = withContext(Dispatchers.IO) {
                        database.insertMessage(user) to database.insertMessage(answer)
                    }
                    messages.add(stored.first)
                    messages.add(stored.second)
                } catch (_: Exception) {
                    Toast.makeText(context, "Não foi possível salvar a mensagem local.", Toast.LENGTH_SHORT).show()
                } finally {
                    sending = false
                }
            }
        }
    }

    if (clearDialog) {
        AlertDialog(
            onDismissRequest = { clearDialog = false },
            title = { Text("Limpar conversa?") },
            text = { Text("As mensagens locais desta conversa serão apagadas deste dispositivo.") },
            confirmButton = {
                TextButton(onClick = {
                    clearDialog = false
                    scope.launch {
                        withContext(Dispatchers.IO) { database.clearMessages() }
                        messages.clear()
                    }
                }) { Text("Apagar", color = CopyCyan) }
            },
            dismissButton = { TextButton(onClick = { clearDialog = false }) { Text("Cancelar") } },
            containerColor = CopySurface,
            titleContentColor = CopyText,
            textContentColor = CopyMuted
        )
    }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = CopyBlue,
            onPrimary = Color.White,
            secondary = CopyCyan,
            background = CopyBackground,
            onBackground = CopyText,
            surface = CopySurface,
            onSurface = CopyText
        )
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = CopyBackground,
            bottomBar = {
                NavigationBar(containerColor = Color(0xFF090D15), contentColor = CopyMuted) {
                    val destinations = listOf(
                        Triple("Assistente", "⌁", "Assistente"),
                        Triple("Conversas", "◷", "Conversas"),
                        Triple("Central", "▦", "Central"),
                        Triple("Memória", "⌘", "Memória"),
                        Triple("Integrações", "⌁", "Integrações")
                    )
                    destinations.forEach { item ->
                        NavigationBarItem(
                            selected = page == item.first,
                            onClick = { page = item.first; selectedModule = "" },
                            icon = { Text(item.second, fontSize = 19.sp, fontWeight = FontWeight.Medium) },
                            label = { Text(item.third, fontSize = 10.sp, maxLines = 1) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = CopyCyan,
                                selectedTextColor = CopyCyan,
                                indicatorColor = Color(0xFF142642),
                                unselectedIconColor = CopyMuted,
                                unselectedTextColor = CopyMuted
                            )
                        )
                    }
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).imePadding()
            ) {
                CopyHeader(
                    page = page,
                    offline = offline,
                    onSettings = { page = "Configurações"; selectedModule = "" }
                )

                when (page) {
                    "Assistente" -> {
                        Column(
                            modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
                                .padding(horizontal = 20.dp)
                        ) {
                            Spacer(Modifier.height(20.dp))
                            Text("—  INTELIGÊNCIA PESSOAL", color = CopyCyan, fontSize = 10.sp, letterSpacing = 2.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(12.dp))
                            Text("Vamos tirar", color = CopyText, fontSize = 32.sp, lineHeight = 36.sp, fontWeight = FontWeight.Light)
                            Text("a próxima ideia", color = CopyText, fontSize = 32.sp, lineHeight = 36.sp, fontWeight = FontWeight.Light)
                            Text("do papel.", color = CopyBlue, fontSize = 34.sp, lineHeight = 38.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                if (offline) "Modo offline. Comandos locais, no seu controle." else "Você conduz. As possibilidades acompanham.",
                                color = CopyMuted, fontSize = 13.sp
                            )
                            Spacer(Modifier.height(12.dp))
                            SignalAperture(offline = offline, modifier = Modifier.fillMaxWidth().height(220.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 18.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("SIGNAL APERTURE", color = CopyMuted, fontSize = 9.sp, letterSpacing = 1.5.sp)
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Box(Modifier.size(5.dp).background(if (offline) CopyMuted else CopyCyan, CircleShape))
                                    Text(if (offline) "OFFLINE" else "EM ESPERA", color = CopyMuted, fontSize = 9.sp, letterSpacing = 1.sp)
                                }
                            }
                            Divider(color = CopyLine)
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Próximos movimentos", color = CopyText, fontSize = 17.sp, fontWeight = FontWeight.Medium)
                                Text("NO SEU RITMO", color = CopyMuted, fontSize = 8.sp, letterSpacing = 1.sp)
                            }
                            HomeAction(
                                title = if (messages.isEmpty()) "Dar forma ao meu dia" else "Retomar conversa",
                                subtitle = if (messages.isEmpty()) "Um plano simples. Uma direção clara." else "Continuar o histórico desta sessão.",
                                action = "↗",
                                onClick = { if (messages.isEmpty()) sendMessage("Me ajude a planejar meu dia") else page = "Conversas" }
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                SmallAction("+", "Criar lembrete", Modifier.weight(1f)) { sendMessage("Quero criar um lembrete") }
                                SmallAction("↗", "Explorar capacidades", Modifier.weight(1f)) { page = "Central" }
                            }
                            Spacer(Modifier.height(16.dp))
                            Text("ESTADO DO SISTEMA", color = CopyMuted, fontSize = 9.sp, letterSpacing = 1.5.sp)
                            Spacer(Modifier.height(7.dp))
                            Text(if (offline) "Modo local selecionado" else "Demonstração local · IA não conectada", color = CopyMuted, fontSize = 12.sp)
                            Spacer(Modifier.height(16.dp))
                        }
                        CommandDock(
                            input = input,
                            onInput = { input = it },
                            onSend = { sendMessage(null) },
                            onVoice = { microphonePermission.launch(Manifest.permission.RECORD_AUDIO) },
                            sending = sending
                        )
                    }

                    "Conversas" -> {
                        if (messages.isEmpty()) {
                            Column(Modifier.weight(1f).fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.Center) {
                                Text("Seu histórico começa aqui.", color = CopyText, fontSize = 27.sp, fontWeight = FontWeight.Light)
                                Spacer(Modifier.height(8.dp))
                                Text("As mensagens desta fundação são armazenadas localmente no aparelho.", color = CopyMuted, fontSize = 14.sp, lineHeight = 21.sp)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                item {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text("Histórico local", color = CopyMuted, fontSize = 11.sp, letterSpacing = 1.sp)
                                        TextButton(onClick = { clearDialog = true }) { Text("Limpar", color = CopyCyan) }
                                    }
                                }
                                items(messages, key = { it.id }) { MessageBubble(it, onSpeak) }
                                if (sending) item { Text("Salvando conversa local…", color = CopyMuted, fontSize = 12.sp) }
                            }
                        }
                        CommandDock(input, { input = it }, { sendMessage(null) }, { microphonePermission.launch(Manifest.permission.RECORD_AUDIO) }, sending)
                    }

                    "Central" -> {
                        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp)) {
                            Text("CENTRO DE COMANDO", color = CopyCyan, fontSize = 10.sp, letterSpacing = 2.sp)
                            Spacer(Modifier.height(10.dp))
                            Text("Tudo em um só lugar.", color = CopyText, fontSize = 28.sp, fontWeight = FontWeight.Light)
                            Spacer(Modifier.height(8.dp))
                            Text("Módulos previstos para o Copy. Os estados abaixo são informativos; serviços ainda não conectados não são apresentados como ativos.", color = CopyMuted, fontSize = 13.sp, lineHeight = 20.sp)
                            Spacer(Modifier.height(18.dp))
                            val modules = listOf(
                                "Extensões e plugins" to "Catálogo, instalação, permissões e compatibilidade.",
                                "Skills e fluxos" to "Capacidades reutilizáveis e fluxos especializados.",
                                "Servidores MCP" to "Conectores, ferramentas expostas e diagnóstico.",
                                "Ferramentas" to "Ações disponíveis, escopos e confirmações.",
                                "Modelos e provedores" to "Seleção local/remota e configuração de provedores.",
                                "Automações" to "Rotinas, gatilhos, horários e histórico.",
                                "Atividade e auditoria" to "Execuções, erros e decisões de permissão."
                            )
                            modules.forEachIndexed { index, module ->
                                ModuleRow(
                                    number = "%02d".format(index + 1),
                                    title = module.first,
                                    description = module.second,
                                    status = "Previsto · não conectado",
                                    onClick = { selectedModule = module.first }
                                )
                                Spacer(Modifier.height(8.dp))
                            }
                            if (selectedModule.isNotEmpty()) {
                                Spacer(Modifier.height(8.dp))
                                Surface(color = CopyMidnight, shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, CopyLine)) {
                                    Column(Modifier.padding(16.dp)) {
                                        Text(selectedModule, color = CopyText, fontSize = 18.sp, fontWeight = FontWeight.Medium)
                                        Spacer(Modifier.height(8.dp))
                                        Text("Este módulo faz parte do escopo planejado. A interface pode ser explorada, mas a integração real ainda precisa ser implementada e testada.", color = CopyMuted, fontSize = 13.sp, lineHeight = 20.sp)
                                        Spacer(Modifier.height(12.dp))
                                        TextButton(onClick = { selectedModule = "" }) { Text("Fechar detalhes", color = CopyCyan) }
                                    }
                                }
                            }
                        }
                    }

                    "Memória" -> {
                        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp)) {
                            Text("MEMÓRIA PESSOAL", color = CopyCyan, fontSize = 10.sp, letterSpacing = 2.sp)
                            Spacer(Modifier.height(10.dp))
                            Text("O que o Copy guarda.", color = CopyText, fontSize = 28.sp, fontWeight = FontWeight.Light)
                            Spacer(Modifier.height(8.dp))
                            Text("Prévia visual. Estes exemplos são temporários e ainda não representam um sistema de memória persistente.", color = CopyMuted, fontSize = 13.sp, lineHeight = 20.sp)
                            Spacer(Modifier.height(16.dp))
                            SettingRow("Memória habilitada", "Preferência desta sessão", memoryEnabled) { memoryEnabled = it }
                            SettingRow("Sincronização opcional", "Não há serviço de sincronização conectado", syncEnabled) { syncEnabled = it }
                            Spacer(Modifier.height(16.dp))
                            Text("EXEMPLOS DE MEMÓRIA", color = CopyMuted, fontSize = 10.sp, letterSpacing = 1.5.sp)
                            Spacer(Modifier.height(8.dp))
                            if (!memoryEnabled) {
                                Text("A visualização de memória está desativada nesta sessão.", color = CopyMuted, fontSize = 13.sp)
                            } else {
                                memories.forEachIndexed { index, memory ->
                                    Surface(color = CopySurface, shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, CopyLine)) {
                                        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Column(Modifier.weight(1f)) {
                                                Text(memory, color = CopyText, fontSize = 14.sp)
                                                Spacer(Modifier.height(4.dp))
                                                Text("Exemplo · não persistido", color = CopyMuted, fontSize = 10.sp)
                                            }
                                            TextButton(onClick = { memories.removeAt(index) }) { Text("Remover", color = CopyCyan, fontSize = 11.sp) }
                                        }
                                    }
                                    Spacer(Modifier.height(8.dp))
                                }
                            }
                            OutlinedButton(onClick = { memories.add("Nova preferência de exemplo") }, border = BorderStroke(1.dp, CopyBlue)) {
                                Text("Adicionar exemplo", color = CopyCyan)
                            }
                        }
                    }

                    "Integrações" -> {
                        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp)) {
                            Text("DISPOSITIVO E CONEXÕES", color = CopyCyan, fontSize = 10.sp, letterSpacing = 2.sp)
                            Spacer(Modifier.height(10.dp))
                            Text("Integrações sob seu controle.", color = CopyText, fontSize = 27.sp, fontWeight = FontWeight.Light)
                            Spacer(Modifier.height(8.dp))
                            Text("Nenhuma integração abaixo é apresentada como conectada sem implementação real.", color = CopyMuted, fontSize = 13.sp, lineHeight = 20.sp)
                            Spacer(Modifier.height(16.dp))
                            listOf(
                                "Arquivos" to "Acesso ainda não implementado",
                                "Notificações" to "Permissão e serviço ainda não implementados",
                                "Calendário" to "Conector ainda não implementado",
                                "Microfone e voz" to "Reconhecimento depende do serviço Android disponível",
                                "Câmera e imagens" to "Acesso ainda não implementado",
                                "Localização" to "Acesso ainda não implementado",
                                "Acessibilidade" to "Serviço ainda não implementado"
                            ).forEach { item ->
                                ModuleRow("•", item.first, item.second, "Não conectado", onClick = { selectedModule = item.first })
                                Spacer(Modifier.height(8.dp))
                            }
                            if (selectedModule.isNotEmpty()) {
                                Text("$selectedModule: esta integração ainda não está conectada. Nenhuma permissão foi concedida por esta tela.", color = CopyMuted, fontSize = 12.sp, lineHeight = 18.sp)
                                TextButton(onClick = { selectedModule = "" }) { Text("Fechar", color = CopyCyan) }
                            }
                        }
                    }

                    else -> {
                        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp)) {
                            Text("CONFIGURAÇÕES", color = CopyCyan, fontSize = 10.sp, letterSpacing = 2.sp)
                            Spacer(Modifier.height(10.dp))
                            Text("Seu sistema. Suas regras.", color = CopyText, fontSize = 28.sp, fontWeight = FontWeight.Light)
                            Spacer(Modifier.height(8.dp))
                            Text("As opções abaixo controlam apenas a experiência desta demonstração.", color = CopyMuted, fontSize = 13.sp)
                            Spacer(Modifier.height(16.dp))
                            SettingRow("Preferir operação local", "Preferência visual; roteador ainda não conectado", offline) { offline = it }
                            Spacer(Modifier.height(8.dp))
                            SettingRow("Memória habilitada", "Controla a prévia de memória desta sessão", memoryEnabled) { memoryEnabled = it }
                            Spacer(Modifier.height(8.dp))
                            SettingRow("Sincronização opcional", "Nenhum backend de sincronização conectado", syncEnabled) { syncEnabled = it }
                            Spacer(Modifier.height(18.dp))
                            Text("ESTILO DE RESPOSTA", color = CopyMuted, fontSize = 10.sp, letterSpacing = 1.5.sp)
                            Spacer(Modifier.height(8.dp))
                            listOf("Conciso", "Equilibrado", "Detalhado").forEach { option ->
                                Row(
                                    Modifier.fillMaxWidth().clickable { responseStyle = option }.padding(vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(option, color = CopyText, fontSize = 14.sp)
                                    }
                                    RadioButton(selected = responseStyle == option, onClick = { responseStyle = option }, colors = RadioButtonDefaults.colors(selectedColor = CopyCyan))
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            OutlinedButton(onClick = { page = "Assistente" }, border = BorderStroke(1.dp, CopyBlue)) {
                                Text("Voltar ao assistente", color = CopyCyan)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CopyHeader(page: String, offline: Boolean, onSettings: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().background(CopyBackground).padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp), modifier = Modifier.weight(1f)) {
            CopyMark()
            Text("COPY", color = CopyText, fontSize = 21.sp, letterSpacing = 3.sp, fontWeight = FontWeight.Bold)
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Box(Modifier.size(5.dp).background(if (offline) CopyMuted else CopyCyan, CircleShape))
                Text(if (offline) "Modo offline" else "Local · demonstração", color = CopyMuted, fontSize = 9.sp)
            }
            Text(if (page == "Configurações") "PREFERÊNCIAS" else "PROTÓTIPO", color = CopyMuted, fontSize = 8.sp, letterSpacing = 1.sp)
        }
        Spacer(Modifier.width(8.dp))
        Text("⚙", color = CopyMuted, fontSize = 21.sp, modifier = Modifier.clickable(onClick = onSettings).padding(5.dp))
    }
    Divider(color = CopyLine.copy(alpha = 0.7f))
}

@Composable
private fun CopyMark() {
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(4.dp).height(19.dp).background(CopyBlue, RoundedCornerShape(2.dp)))
        Box(Modifier.width(4.dp).height(25.dp).background(CopyCyan, RoundedCornerShape(2.dp)))
        Box(Modifier.width(4.dp).height(16.dp).background(CopyBlue, RoundedCornerShape(2.dp)))
    }
}

@Composable
private fun SignalAperture(offline: Boolean, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val center = Offset(w * 0.52f, h * 0.51f)
        val cBlue = if (offline) CopyMuted else CopyBlue
        val cCyan = if (offline) CopyMuted else CopyCyan
        drawLine(CopyLine.copy(alpha = 0.7f), Offset(w * 0.04f, h * 0.83f), Offset(w * 0.96f, h * 0.83f), 1f)
        drawLine(CopyLine.copy(alpha = 0.55f), Offset(w * 0.72f, h * 0.08f), Offset(w * 0.72f, h * 0.92f), 1f)
        for (i in 0..6) {
            val x = w * (0.08f + i * 0.14f)
            drawLine(CopyLine, Offset(x, h * 0.81f), Offset(x, h * 0.85f), 1f)
        }
        drawArc(cBlue.copy(alpha = 0.78f), 205f, 260f, false, Offset(w * 0.10f, h * 0.13f), Size(w * 0.80f, h * 0.67f), style = Stroke(width = 1.4.dp.toPx()))
        drawArc(cCyan.copy(alpha = 0.82f), 20f, 205f, false, Offset(w * 0.16f, h * 0.18f), Size(w * 0.69f, h * 0.60f), style = Stroke(width = 1.2.dp.toPx()))
        drawArc(cBlue.copy(alpha = 0.58f), 195f, 240f, false, Offset(w * 0.24f, h * 0.27f), Size(w * 0.51f, h * 0.45f), style = Stroke(width = 1.dp.toPx()))
        drawArc(cCyan.copy(alpha = 0.75f), 210f, 100f, false, Offset(w * 0.34f, h * 0.31f), Size(w * 0.34f, h * 0.36f), style = Stroke(width = 1.5.dp.toPx()))
        drawLine(cCyan.copy(alpha = 0.8f), Offset(w * 0.72f, h * 0.20f), Offset(w * 0.72f, h * 0.68f), 1.dp.toPx())
        drawCircle(cCyan, radius = 3.dp.toPx(), center = Offset(w * 0.72f, h * 0.20f))
        drawCircle(cCyan, radius = 2.dp.toPx(), center = Offset(w * 0.22f, h * 0.70f))
        drawLine(cBlue, Offset(w * 0.20f, h * 0.76f), Offset(w * 0.24f, h * 0.72f), 1.dp.toPx())
        drawLine(cBlue, Offset(w * 0.24f, h * 0.80f), Offset(w * 0.28f, h * 0.76f), 1.dp.toPx())
        drawLine(cBlue, Offset(w * 0.28f, h * 0.83f), Offset(w * 0.32f, h * 0.79f), 1.dp.toPx())
        drawCircle(cBlue.copy(alpha = 0.7f), radius = 1.5.dp.toPx(), center = center)
    }
}

@Composable
private fun HomeAction(title: String, subtitle: String, action: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        color = Color.Transparent,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, CopyLine)
    ) {
        Row(Modifier.padding(horizontal = 13.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(action, color = CopyCyan, fontSize = 22.sp, modifier = Modifier.padding(end = 12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, color = CopyText, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Text(subtitle, color = CopyMuted, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Text("↗", color = CopyBlue, fontSize = 18.sp)
        }
    }
}

@Composable
private fun SmallAction(symbol: String, label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(modifier = modifier.clickable(onClick = onClick), color = CopyMidnight, shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, CopyLine)) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(symbol, color = CopyCyan, fontSize = 16.sp)
            Text(label, color = CopyText, fontSize = 10.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun CommandDock(
    input: String,
    onInput: (String) -> Unit,
    onSend: () -> Unit,
    onVoice: () -> Unit,
    sending: Boolean
) {
    Column(
        Modifier.fillMaxWidth().background(CopyBackground).padding(horizontal = 14.dp, vertical = 8.dp)
            .border(BorderStroke(1.dp, CopyBlue.copy(alpha = 0.65f)), RoundedCornerShape(13.dp))
            .padding(10.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("—  TEXTO", color = CopyCyan, fontSize = 9.sp, letterSpacing = 1.5.sp)
            Text("VOZ DO DISPOSITIVO", color = CopyMuted, fontSize = 8.sp, letterSpacing = 0.8.sp)
        }
        Row(Modifier.fillMaxWidth().padding(top = 7.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = input,
                onValueChange = onInput,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Dê uma instrução ao Copy...", color = CopyMuted, fontSize = 13.sp) },
                shape = RoundedCornerShape(8.dp),
                maxLines = 3,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSend() }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CopyCyan,
                    unfocusedBorderColor = Color.Transparent,
                    focusedTextColor = CopyText,
                    unfocusedTextColor = CopyText,
                    cursorColor = CopyCyan,
                    focusedContainerColor = CopySurface,
                    unfocusedContainerColor = CopySurface
                )
            )
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(
                    onClick = onVoice,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CopyElevated, contentColor = CopyCyan),
                    shape = RoundedCornerShape(8.dp)
                ) { Text("Voz", fontSize = 11.sp) }
                Button(
                    onClick = onSend,
                    enabled = input.isNotBlank() && !sending,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CopyBlue, contentColor = Color.White),
                    shape = RoundedCornerShape(8.dp)
                ) { Text("→", fontSize = 17.sp) }
            }
        }
        Text("Demonstração local. O áudio não é gravado pelo Copy.", color = CopyMuted, fontSize = 9.sp, modifier = Modifier.padding(top = 5.dp))
    }
}

@Composable
private fun ModuleRow(number: String, title: String, description: String, status: String, onClick: () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), color = CopySurface, shape = RoundedCornerShape(10.dp), border = BorderStroke(1.dp, CopyLine)) {
        Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(11.dp)) {
            Text(number, color = CopyCyan, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, color = CopyText, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Text(description, color = CopyMuted, fontSize = 11.sp, lineHeight = 15.sp)
                Text(status, color = CopyCyan.copy(alpha = 0.8f), fontSize = 9.sp)
            }
            Text("↗", color = CopyBlue, fontSize = 17.sp)
        }
    }
}

@Composable
private fun SettingRow(title: String, description: String, enabled: Boolean, onChange: (Boolean) -> Unit) {
    Surface(color = CopySurface, shape = RoundedCornerShape(10.dp), border = BorderStroke(1.dp, CopyLine)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 13.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, color = CopyText, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Text(description, color = CopyMuted, fontSize = 10.sp, lineHeight = 14.sp)
            }
            Switch(checked = enabled, onCheckedChange = onChange, colors = SwitchDefaults.colors(checkedThumbColor = CopyCyan, checkedTrackColor = CopyBlue))
        }
    }
}

@Composable
private fun MessageBubble(message: ChatMessage, onSpeak: (String) -> Unit) {
    val isUser = message.role == "user"
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start) {
        Surface(
            modifier = Modifier.fillMaxWidth(if (isUser) 0.88f else 0.96f),
            color = if (isUser) Color(0xFF13294A) else CopySurface,
            shape = RoundedCornerShape(topStart = 15.dp, topEnd = 15.dp, bottomEnd = if (isUser) 4.dp else 15.dp, bottomStart = if (isUser) 15.dp else 4.dp),
            border = BorderStroke(1.dp, if (isUser) CopyBlue.copy(alpha = 0.5f) else CopyLine)
        ) {
            Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text(if (isUser) "VOCÊ" else "COPY · LOCAL", color = if (isUser) CopyCyan else CopyMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Text(message.content, color = CopyText, fontSize = 14.sp, lineHeight = 21.sp)
                if (!isUser) {
                    TextButton(onClick = { onSpeak(message.content) }, contentPadding = PaddingValues(0.dp)) {
                        Text("Ouvir resposta", color = CopyCyan, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
