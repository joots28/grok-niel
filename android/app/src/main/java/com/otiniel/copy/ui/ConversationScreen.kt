package com.otiniel.copy.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.otiniel.copy.data.ConversationMessage
import com.otiniel.copy.ui.theme.CopyBorder
import com.otiniel.copy.ui.theme.CopyMuted
import com.otiniel.copy.ui.theme.CopyPrimary
import com.otiniel.copy.ui.theme.CopySurface
import com.otiniel.copy.ui.theme.CopySurfaceRaised
import com.otiniel.copy.ui.theme.CopyText

@Composable
fun ConversationScreen(
    sessionId: Long?,
    initialPrompt: String,
    onBack: () -> Unit,
    conversationViewModel: ConversationViewModel = viewModel(),
) {
    val uiState by conversationViewModel.uiState.collectAsState()
    var draft by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(sessionId, initialPrompt) {
        conversationViewModel.openSession(sessionId, initialPrompt)
    }
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) listState.animateScrollToItem(uiState.messages.lastIndex)
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = CopySurface,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, CopyBorder),
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                DemoLabel(
                    if (uiState.isLocalModelInstalled) "IA LOCAL · OFFLINE" else "MODELO LOCAL NÃO INSTALADO",
                )
                Text(
                    if (uiState.isLocalModelInstalled) {
                        "As respostas são geradas no dispositivo com o modelo instalado. Nenhuma mensagem é enviada a uma API remota. Títulos e mensagens são cifrados com AES-GCM; alguns metadados continuam no SQLite."
                    } else {
                        "Importe um modelo .litertlm gratuito na Central de controle para ativar respostas offline. Sem ele, o Copy salva mensagens localmente, mas não gera respostas de IA. Nenhuma mensagem é enviada a uma API remota."
                    },
                    color = CopyMuted,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                )
            }
        }

        if (uiState.isLoading) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = CopySurfaceRaised,
                shape = RoundedCornerShape(18.dp),
            ) {
                Text("Carregando conversa salva…", modifier = Modifier.padding(18.dp), color = CopyMuted, fontSize = 12.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (uiState.messages.isEmpty()) {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = CopySurfaceRaised,
                            shape = RoundedCornerShape(20.dp),
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(7.dp),
                            ) {
                                Text("Por onde começamos?", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                                Text("Escreva uma mensagem. Ela ficará salva no histórico local.", color = CopyMuted, fontSize = 12.sp)
                            }
                        }
                    }
                } else {
                    items(uiState.messages, key = { it.id }) { message ->
                        MessageBubble(message)
                    }
                }
            }
        }

        if (uiState.error != null) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = CopySurfaceRaised,
                shape = RoundedCornerShape(12.dp),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(uiState.error.orEmpty(), modifier = Modifier.weight(1f), color = CopyMuted, fontSize = 11.sp)
                    androidx.compose.material3.TextButton(onClick = conversationViewModel::dismissError) {
                        Text("Fechar")
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it.take(12000) },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Mensagem…", fontSize = 12.sp) },
                maxLines = 4,
                shape = RoundedCornerShape(16.dp),
                enabled = !uiState.isLoading && !uiState.isSending,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CopyPrimary,
                    unfocusedBorderColor = CopyBorder,
                    focusedTextColor = CopyText,
                    unfocusedTextColor = CopyText,
                    cursorColor = CopyPrimary,
                ),
            )
            Button(
                onClick = {
                    conversationViewModel.sendMessage(draft)
                    draft = ""
                },
                enabled = draft.isNotBlank() && !uiState.isLoading && !uiState.isSending,
                shape = RoundedCornerShape(15.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CopyPrimary, contentColor = CopySurface),
            ) {
                androidx.compose.material3.Icon(Icons.Rounded.Send, contentDescription = "Salvar mensagem")
            }
        }
        if (uiState.isSending) {
            Text("Salvando mensagem…", color = CopyMuted, fontSize = 10.sp)
        }
    }
}

@Composable
private fun MessageBubble(message: ConversationMessage) {
    val isUser = message.role == "user"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.9f),
            color = if (isUser) CopyPrimary.copy(alpha = 0.16f) else CopySurface,
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, if (isUser) CopyPrimary.copy(alpha = 0.3f) else CopyBorder),
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    if (isUser) "VOCÊ" else "COPY · RESPOSTA DE DEMONSTRAÇÃO",
                    color = if (isUser) CopyPrimary else CopyMuted,
                    fontSize = 9.sp,
                    letterSpacing = 0.8.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(message.content, color = CopyText, fontSize = 13.sp, lineHeight = 19.sp)
            }
        }
    }
}
