package com.otiniel.copy.ui

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
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.otiniel.copy.data.demoHistory
import com.otiniel.copy.ui.theme.CopyBorder
import com.otiniel.copy.ui.theme.CopyMuted
import com.otiniel.copy.ui.theme.CopyPrimary
import com.otiniel.copy.ui.theme.CopySurface
import com.otiniel.copy.ui.theme.CopySurfaceRaised
import com.otiniel.copy.ui.theme.CopyText

@Composable
fun HistoryScreen(onOpenConversation: (String) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    val sessions = demoHistory.filter {
        it.title.contains(query, ignoreCase = true) ||
            it.detail.contains(query, ignoreCase = true)
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
                subtitle = "Encontre sessões de exemplo e abra uma conversa local.",
            )
        }
        item { DemoLabel() }
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("Buscar sessões…", fontSize = 13.sp) },
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
        if (sessions.isEmpty()) {
            item {
                AccentCard {
                    Text("Nenhuma sessão encontrada", fontWeight = FontWeight.SemiBold)
                    Text("Confira a escrita ou tente outro termo.", color = CopyMuted, fontSize = 12.sp)
                }
            }
        } else {
            items(sessions, key = { it.title }) { session ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onOpenConversation(session.title) },
                    shape = RoundedCornerShape(19.dp),
                    colors = CardDefaults.cardColors(containerColor = CopySurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CopyBorder),
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
                            Text(session.title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text(session.detail, color = CopyMuted, fontSize = 12.sp)
                        }
                        Text(session.time, color = CopyMuted, fontSize = 10.sp)
                    }
                }
            }
        }
        item {
            Text(
                "Esta lista contém dados fixos de demonstração; o armazenamento persistente será adicionado em outra etapa.",
                color = CopyMuted,
                fontSize = 11.sp,
                lineHeight = 16.sp,
            )
        }
    }
}
