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
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.otiniel.copy.ui.theme.CopyBorder
import com.otiniel.copy.ui.theme.CopyMuted
import com.otiniel.copy.ui.theme.CopyPrimary
import com.otiniel.copy.ui.theme.CopySurface
import com.otiniel.copy.ui.theme.CopySurfaceRaised
import com.otiniel.copy.ui.theme.CopyText

private data class ChatLine(val isUser: Boolean, val text: String)

@Composable
fun ConversationScreen(
    initialPrompt: String,
    onBack: () -> Unit,
) {
    val lines = remember(initialPrompt) {
        mutableStateListOf<ChatLine>().apply {
            if (initialPrompt.isNotBlank()) add(ChatLine(isUser = true, text = initialPrompt))
        }
    }
    var draft by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = CopySurface,
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, CopyBorder),
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                DemoLabel("IA NÃO CONECTADA")
                Text(
                    "Este é um teste da interface. O texto não será enviado a um modelo nem a um serviço remoto.",
                    color = CopyMuted,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                )
            }
        }

        if (lines.isEmpty()) {
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
                    Text("Escreva uma mensagem para testar o fluxo visual da conversa.", color = CopyMuted, fontSize = 12.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(lines) { line ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (line.isUser) Arrangement.End else Arrangement.Start,
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(0.88f),
                            color = if (line.isUser) CopyPrimary.copy(alpha = 0.16f) else CopySurface,
                            shape = RoundedCornerShape(18.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (line.isUser) CopyPrimary.copy(alpha = 0.3f) else CopyBorder,
                            ),
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Text(
                                    if (line.isUser) "VOCÊ" else "ESTADO DO COPY",
                                    color = if (line.isUser) CopyPrimary else CopyMuted,
                                    fontSize = 9.sp,
                                    letterSpacing = 1.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(line.text, color = CopyText, fontSize = 13.sp, lineHeight = 19.sp)
                            }
                        }
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
                onValueChange = { draft = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Mensagem de teste…", fontSize = 12.sp) },
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
            Button(
                onClick = {
                    val message = draft.trim()
                    if (message.isNotEmpty()) {
                        lines.add(ChatLine(isUser = true, text = message))
                        lines.add(
                            ChatLine(
                                isUser = false,
                                text = "Mensagem registrada apenas nesta prévia. O núcleo de IA ainda não está conectado.",
                            ),
                        )
                        draft = ""
                    }
                },
                enabled = draft.isNotBlank(),
                shape = RoundedCornerShape(15.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CopyPrimary, contentColor = CopySurface),
            ) {
                androidx.compose.material3.Icon(Icons.Rounded.Send, contentDescription = "Enviar mensagem")
            }
        }
    }
}
