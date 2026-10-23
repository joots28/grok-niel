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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.otiniel.copy.data.ConversationSession
import com.otiniel.copy.ui.theme.CopyBorder
import com.otiniel.copy.ui.theme.CopyMuted
import com.otiniel.copy.ui.theme.CopyPrimary
import com.otiniel.copy.ui.theme.CopySurface
import com.otiniel.copy.ui.theme.CopySurfaceRaised
import com.otiniel.copy.ui.theme.CopyText
import java.text.DateFormat
import java.util.Date

@Composable
fun HistoryScreen(
    onOpenConversation: (Long) -> Unit,
    conversationViewModel: ConversationViewModel = viewModel(),
) {
    val uiState by conversationViewModel.uiState.collectAsState()
    var query by rememberSaveable { mutableStateOf("") }
    var sessionToDelete by remember { mutableStateOf<ConversationSession?>(null) }

    LaunchedEffect(Unit) { conversationViewModel.refreshSessions() }

    val sessions = uiState.sessions.filter {
        it.title.contains(query, ignoreCase = true) ||
            it.preview.contains(query, ignoreCase = true)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 20.dp, end = 20.dp, top = 10.dp, bottom = 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(15.dp),
    ) {
        item {
            PageHeader(
                eyebrow = "CONTEXTO E CONTINUIDADE",
                title = "Histórico",
                subtitle = "Suas conversas salvas neste dispositivo.",
            )
        }
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("Buscar conversas…", fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = CopyMuted) },
                shape = RoundedCornerShape(15.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CopyPrimary,
                    unfocusedBorderColor = CopyBorder,
                    focusedTextColor = CopyText,
                    unfocusedTextColor = CopyText,
                    cursorColor = CopyPrimary,
                ),
            )
        }

        if (uiState.error != null) {
            item {
                AccentCard {
                    Text("Não foi possível carregar o histórico", fontWeight = FontWeight.SemiBold)
                    Text(uiState.error.orEmpty(), color = CopyMuted, fontSize = 12.sp)
                    TextButton(onClick = conversationViewModel::dismissError) { Text("Fechar") }
                }
            }
        }

        if (sessions.isEmpty()) {
            item {
                AccentCard {
                    Text(
                        if (query.isBlank()) "Nenhuma conversa salva" else "Nenhuma conversa encontrada",
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        if (query.isBlank()) "As conversas aparecerão aqui depois que você iniciar uma sessão."
                        else "Confira a escrita ou tente outro termo.",
                        color = CopyMuted,
                        fontSize = 12.sp,
                    )
                }
            }
        } else {
            items(sessions, key = { it.id }) { session ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onOpenConversation(session.id) },
                    shape = RoundedCornerShape(19.dp),
                    colors = CardDefaults.cardColors(containerColor = CopySurface),
                    border = BorderStroke(1.dp, CopyBorder),
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Card(
                            shape = RoundedCornerShape(13.dp),
                            colors = CardDefaults.cardColors(containerColor = CopySurfaceRaised),
                        ) {
                            Icon(
                                Icons.Rounded.ChatBubbleOutline,
                                contentDescription = null,
                                modifier = Modifier.padding(11.dp),
                                tint = CopyPrimary,
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text(session.title, color = CopyText, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text(
                                session.preview.ifBlank { "Conversa sem mensagens" },
                                color = CopyMuted,
                                fontSize = 12.sp,
                                maxLines = 2,
                            )
                            Text(
                                "${session.messageCount} mensagem(ns) · ${formatTimestamp(session.updatedAt)}",
                                color = CopyMuted,
                                fontSize = 10.sp,
                            )
                        }
                        IconButton(onClick = { sessionToDelete = session }) {
                            Icon(Icons.Rounded.DeleteOutline, contentDescription = "Excluir conversa", tint = CopyMuted)
                        }
                    }
                }
            }
        }
        item {
            Text(
                "As sessões são armazenadas localmente. A IA e a sincronização em nuvem ainda não estão conectadas.",
                color = CopyMuted,
                fontSize = 11.sp,
                lineHeight = 16.sp,
            )
        }
    }

    sessionToDelete?.let { session ->
        AlertDialog(
            onDismissRequest = { sessionToDelete = null },
            title = { Text("Excluir conversa?") },
            text = {
                Text(
                    "“${session.title}” e todas as mensagens dela serão removidas deste dispositivo. Essa ação não pode ser desfeita.",
                    color = CopyMuted,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        conversationViewModel.deleteSession(session.id)
                        sessionToDelete = null
                    },
                ) { Text("Excluir") }
            },
            dismissButton = {
                TextButton(onClick = { sessionToDelete = null }) { Text("Cancelar") }
            },
            containerColor = CopySurface,
            titleContentColor = CopyText,
            textContentColor = CopyText,
        )
    }
}

private fun formatTimestamp(timestamp: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(timestamp))
