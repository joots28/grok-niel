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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Psychology
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
import com.otiniel.copy.data.MemoryItem
import com.otiniel.copy.data.initialMemories
import com.otiniel.copy.ui.theme.CopyBorder
import com.otiniel.copy.ui.theme.CopyMuted
import com.otiniel.copy.ui.theme.CopyPrimary
import com.otiniel.copy.ui.theme.CopySurface
import com.otiniel.copy.ui.theme.CopySurfaceRaised
import com.otiniel.copy.ui.theme.CopyText

@Composable
fun MemoryScreen() {
    val memories = remember {
        mutableStateListOf<MemoryItem>().apply { addAll(initialMemories) }
    }
    var dialogOpen by remember { mutableStateOf(false) }
    var editingId by remember { mutableStateOf<Long?>(null) }
    var title by remember { mutableStateOf("") }
    var detail by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 20.dp, end = 20.dp, top = 10.dp, bottom = 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(15.dp),
    ) {
        item {
            PageHeader(
                eyebrow = "MEMÓRIA SOB SEU CONTROLE",
                title = "Memória",
                subtitle = "Consulte, ajuste ou remova itens da sessão de demonstração.",
            )
        }
        item {
            AccentCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Psychology, contentDescription = null, tint = CopyPrimary)
                    Spacer(Modifier.width(10.dp))
                    Text("Memória temporária", fontWeight = FontWeight.SemiBold)
                }
                Text(
                    "Os dados abaixo existem somente no estado desta tela e podem desaparecer ao reiniciar o app. SQLite e sincronização ainda não estão conectados.",
                    color = CopyMuted,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                )
                DemoLabel()
                Button(
                    onClick = {
                        editingId = null
                        title = ""
                        detail = ""
                        dialogOpen = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CopyPrimary, contentColor = CopySurface),
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Adicionar memória de demonstração")
                }
            }
        }
        if (memories.isEmpty()) {
            item {
                AccentCard {
                    Text("Nenhum item salvo nesta sessão", fontWeight = FontWeight.SemiBold)
                    Text("Use o botão acima para adicionar um item de teste.", color = CopyMuted, fontSize = 12.sp)
                }
            }
        } else {
            items(memories, key = { it.id }) { memory ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        editingId = memory.id
                        title = memory.title
                        detail = memory.detail
                        dialogOpen = true
                    },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = CopySurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CopyBorder),
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = CopySurfaceRaised),
                        ) {
                            Icon(
                                Icons.Rounded.Psychology,
                                contentDescription = null,
                                modifier = Modifier.padding(10.dp),
                                tint = CopyPrimary,
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(memory.title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text(memory.detail, color = CopyMuted, fontSize = 12.sp, lineHeight = 17.sp)
                            Text("MEMÓRIA DE TESTE", color = CopyPrimary, fontSize = 9.sp, letterSpacing = 0.8.sp)
                        }
                        IconButton(onClick = {
                            memories.removeAll { it.id == memory.id }
                        }) {
                            Icon(Icons.Rounded.DeleteOutline, contentDescription = "Excluir memória", tint = CopyMuted)
                        }
                    }
                }
            }
        }
        item {
            Text(
                "O projeto final deverá oferecer confirmação, edição, exportação e exclusão persistente, com memória de longo prazo opcional.",
                color = CopyMuted,
                fontSize = 11.sp,
                lineHeight = 16.sp,
            )
        }
    }

    if (dialogOpen) {
        AlertDialog(
            onDismissRequest = { dialogOpen = false },
            title = { Text(if (editingId == null) "Nova memória de teste" else "Editar memória de teste") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Título") },
                        singleLine = true,
                        colors = memoryFieldColors(),
                    )
                    OutlinedTextField(
                        value = detail,
                        onValueChange = { detail = it },
                        label = { Text("Conteúdo") },
                        minLines = 3,
                        colors = memoryFieldColors(),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = title.isNotBlank() && detail.isNotBlank(),
                    onClick = {
                        val id = editingId
                        if (id == null) {
                            val nextId = (memories.maxOfOrNull { it.id } ?: 0L) + 1L
                            memories.add(MemoryItem(nextId, title.trim(), detail.trim()))
                        } else {
                            val index = memories.indexOfFirst { it.id == id }
                            if (index >= 0) memories[index] = MemoryItem(id, title.trim(), detail.trim())
                        }
                        dialogOpen = false
                    },
                ) { Text("Salvar") }
            },
            dismissButton = {
                TextButton(onClick = { dialogOpen = false }) { Text("Cancelar") }
            },
            containerColor = CopySurface,
            titleContentColor = CopyText,
            textContentColor = CopyText,
        )
    }
}

@Composable
private fun memoryFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = CopyPrimary,
    unfocusedBorderColor = CopyBorder,
    focusedTextColor = CopyText,
    unfocusedTextColor = CopyText,
    cursorColor = CopyPrimary,
)
