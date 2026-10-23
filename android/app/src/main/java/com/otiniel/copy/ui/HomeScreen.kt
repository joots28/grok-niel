package com.otiniel.copy.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.otiniel.copy.data.demoHistory
import com.otiniel.copy.ui.theme.CopyBorder
import com.otiniel.copy.ui.theme.CopyMuted
import com.otiniel.copy.ui.theme.CopyPrimary
import com.otiniel.copy.ui.theme.CopySecondary
import com.otiniel.copy.ui.theme.CopySurface
import com.otiniel.copy.ui.theme.CopySurfaceRaised
import com.otiniel.copy.ui.theme.CopyText

@Composable
fun HomeScreen(
    onOpenConversation: (String) -> Unit,
    onOpenHistory: () -> Unit,
) {
    var prompt by rememberSaveable { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 20.dp, end = 20.dp, top = 10.dp, bottom = 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item {
            PageHeader(
                eyebrow = "SEU ASSISTENTE PESSOAL",
                title = "Seu espaço.\nSeu controle.",
                subtitle = "Uma base privada para conversar, organizar ideias e preparar ações.",
            )
        }
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = CopySurface),
                border = BorderStroke(1.dp, CopyBorder),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                listOf(CopyPrimary.copy(alpha = 0.15f), CopySecondary.copy(alpha = 0.04f), CopySurface),
                            ),
                        )
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = CopySurfaceRaised),
                            shape = RoundedCornerShape(16.dp),
                        ) {
                            Icon(
                                Icons.Rounded.Bolt,
                                contentDescription = null,
                                modifier = Modifier.padding(12.dp),
                                tint = CopyPrimary,
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Copy está na base inicial", color = CopyText, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                            Text("Interface nativa em construção", color = CopyMuted, fontSize = 12.sp)
                        }
                    }
                    DemoLabel()
                    Text(
                        "A conversa abaixo funciona como prévia local. Nenhuma IA, microfone ou integração do aparelho está conectada ainda.",
                        color = CopyMuted,
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                    )
                }
            }
        }
        item {
            AccentCard {
                Text("Comece por uma conversa", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    "Escreva uma mensagem para abrir a interface de conversa de demonstração.",
                    color = CopyMuted,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                )
                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Escreva para o Copy…", fontSize = 13.sp) },
                    minLines = 2,
                    maxLines = 4,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CopyPrimary,
                        unfocusedBorderColor = CopyBorder,
                        focusedTextColor = CopyText,
                        unfocusedTextColor = CopyText,
                        cursorColor = CopyPrimary,
                    ),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                ) {
                    Button(
                        onClick = {
                            onOpenConversation(prompt.trim())
                            prompt = ""
                        },
                        enabled = prompt.isNotBlank(),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CopyPrimary, contentColor = CopySurface),
                    ) {
                        Icon(Icons.Rounded.Send, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Abrir conversa")
                    }
                    Button(
                        onClick = { onOpenConversation("") },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CopySurfaceRaised, contentColor = CopyMuted),
                    ) {
                        Icon(Icons.Rounded.Mic, contentDescription = "Voz ainda não conectada")
                    }
                }
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Sessões recentes", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Text("Exemplos de navegação", color = CopyMuted, fontSize = 11.sp)
                }
                Button(
                    onClick = onOpenHistory,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CopySurfaceRaised, contentColor = CopyText),
                ) {
                    Text("Ver todas", fontSize = 11.sp)
                    Spacer(Modifier.width(5.dp))
                    Icon(Icons.Rounded.ArrowForward, contentDescription = null)
                }
            }
        }
        items(demoHistory.size) { index ->
            val session = demoHistory[index]
            Card(
                modifier = Modifier.fillMaxWidth(),
                onClick = onOpenHistory,
                shape = RoundedCornerShape(17.dp),
                colors = CardDefaults.cardColors(containerColor = CopySurface),
                border = BorderStroke(1.dp, CopyBorder),
            ) {
                Row(
                    modifier = Modifier.padding(15.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.History, contentDescription = null, tint = CopyPrimary)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(session.title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text(session.detail, color = CopyMuted, fontSize = 11.sp)
                    }
                    Text(session.time, color = CopyMuted, fontSize = 10.sp)
                }
            }
        }
        item {
            Spacer(Modifier.height(3.dp))
            Text(
                "PRÓXIMO MARCO · PERSISTÊNCIA LOCAL E CONTRATO DE IA",
                color = CopyMuted,
                fontSize = 9.sp,
                letterSpacing = 1.sp,
            )
        }
    }
}
