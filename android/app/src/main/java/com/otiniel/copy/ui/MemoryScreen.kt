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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
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
import com.otiniel.copy.data.MemoryInputValidator
import com.otiniel.copy.data.MemoryItem
import com.otiniel.copy.ui.theme.CopyBorder
import com.otiniel.copy.ui.theme.CopyMuted
import com.otiniel.copy.ui.theme.CopyPrimary
import com.otiniel.copy.ui.theme.CopySurface
import com.otiniel.copy.ui.theme.CopySurfaceRaised
import com.otiniel.copy.ui.theme.CopyText

@Composable
fun MemoryScreen(memoryViewModel: MemoryViewModel = viewModel()) {
    val uiState by memoryViewModel.uiState.collectAsState()
    var dialogOpen by remember { mutableStateOf(false) }
    var editingId by remember { mutableStateOf<Long?>(null) }
    var title by remember { mutableStateOf("") }
    var detail by remember { mutableStateOf("") }
    var memoryToDelete by remember { mutableStateOf<MemoryItem?>(null) }
    var confirmDeleteAll by remember { mutableStateOf(false) }
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
        onResult = { uri -> if (uri != null) memoryViewModel.exportTo(uri) },
    )

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
                subtitle = "Você escolhe o que guardar e pode revisar ou excluir cada item.",
            )
        }
        item {
            AccentCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Psychology, contentDescription = null, tint = CopyPrimary)
                    Spacer(Modifier.width(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("Armazenamento local", fontWeight = FontWeight.SemiBold)
                        Text("SQLite · banco privado do app", color = CopyMuted, fontSize = 11.sp)
                    }
                }
                Text(
                    "Somente as memórias que você confirmar em Salvar são armazenadas. Nesta versão, os dados ficam neste aparelho e não são sincronizados com a nuvem. O banco ainda não tem criptografia própria.",
                    color = CopyMuted,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                )
                Button(
                    onClick = {
                        memoryViewModel.dismissFeedback()
                        editingId = null
                        title = ""
                        detail = ""
                        dialogOpen = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isSaving,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CopyPrimary,
                        contentColor = CopySurface,
                    ),
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Adicionar memória")
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(
                        enabled = !uiState.isSaving && !uiState.isLoading,
                        onClick = {
                            memoryViewModel.dismissFeedback()
                            exportLauncher.launch("copy-memories-${System.currentTimeMillis()}.json")
                        },
                    ) { Text("Exportar JSON") }
                    TextButton(
                        enabled = !uiState.isSaving && uiState.memories.isNotEmpty(),
                        onClick = {
                            memoryViewModel.dismissFeedback()
                            confirmDeleteAll = true
                        },
                    ) { Text("Excluir todas") }
                }
            }
        }

        if (uiState.error != null) {
            item {
                AccentCard {
                    Text("Não foi possível concluir", fontWeight = FontWeight.SemiBold)
                    Text(uiState.error.orEmpty(), color = CopyMuted, fontSize = 12.sp, lineHeight = 18.sp)
                    TextButton(onClick = memoryViewModel::dismissFeedback) { Text("Fechar") }
                }
            }
        } else if (uiState.notice != null) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(uiState.notice.orEmpty(), color = CopyPrimary, fontSize = 12.sp)
                    TextButton(onClick = memoryViewModel::dismissFeedback) { Text("Dispensar") }
                }
            }
        }

        if (uiState.isLoading) {
            item {
                AccentCard { Text("Carregando memórias salvas…", color = CopyMuted, fontSize = 12.sp) }
            }
        } else if (uiState.memories.isEmpty()) {
            item {
                AccentCard {
                    Text("Nenhuma memória salva", fontWeight = FontWeight.SemiBold)
                    Text(
                        "O Copy não cria memórias automaticamente nesta etapa. Adicione apenas o que deseja manter.",
                        color = CopyMuted,
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                    )
                }
            }
        } else {
            items(uiState.memories, key = { it.id }) { memory ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        memoryViewModel.dismissFeedback()
                        editingId = memory.id
                        title = memory.title
                        detail = memory.detail
                        dialogOpen = true
                    },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = CopySurface),
                    border = BorderStroke(1.dp, CopyBorder),
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
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(memory.title, color = CopyText, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text(memory.detail, color = CopyMuted, fontSize = 12.sp, lineHeight = 17.sp)
                            Text("SALVO NESTE DISPOSITIVO", color = CopyPrimary, fontSize = 9.sp, letterSpacing = 0.7.sp)
                            Text("Toque no cartão para editar", color = CopyMuted, fontSize = 10.sp)
                        }
                        IconButton(
                            onClick = { memoryToDelete = memory },
                            enabled = !uiState.isSaving,
                        ) {
                            Icon(
                                Icons.Rounded.DeleteOutline,
                                contentDescription = "Excluir memória",
                                tint = CopyMuted,
                            )
                        }
                    }
                }
            }
        }

        item {
            Text(
                "O arquivo JSON contém o conteúdo das memórias; salve-o apenas em um local confiável. O banco local ainda não possui criptografia própria.",
                color = CopyMuted,
                fontSize = 11.sp,
                lineHeight = 16.sp,
            )
        }
    }

    if (dialogOpen) {
        AlertDialog(
            onDismissRequest = { if (!uiState.isSaving) dialogOpen = false },
            title = { Text(if (editingId == null) "Nova memória local" else "Editar memória local") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it.take(MemoryInputValidator.MAX_TITLE_LENGTH) },
                        label = { Text("Título") },
                        supportingText = {
                            Text("${title.length}/${MemoryInputValidator.MAX_TITLE_LENGTH}")
                        },
                        singleLine = true,
                        colors = memoryFieldColors(),
                    )
                    OutlinedTextField(
                        value = detail,
                        onValueChange = { detail = it.take(MemoryInputValidator.MAX_DETAIL_LENGTH) },
                        label = { Text("Conteúdo") },
                        supportingText = {
                            Text("${detail.length}/${MemoryInputValidator.MAX_DETAIL_LENGTH}")
                        },
                        minLines = 3,
                        maxLines = 6,
                        colors = memoryFieldColors(),
                    )
                    Text(
                        "Ao salvar, você confirma que deseja manter este conteúdo no armazenamento local do Copy.",
                        color = CopyMuted,
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !uiState.isSaving && title.isNotBlank() && detail.isNotBlank(),
                    onClick = {
                        memoryViewModel.save(editingId, title, detail) { saved ->
                            if (saved) {
                                dialogOpen = false
                                editingId = null
                                title = ""
                                detail = ""
                            }
                        }
                    },
                ) { Text(if (uiState.isSaving) "Salvando…" else "Salvar") }
            },
            dismissButton = {
                TextButton(
                    enabled = !uiState.isSaving,
                    onClick = { dialogOpen = false },
                ) { Text("Cancelar") }
            },
            containerColor = CopySurface,
            titleContentColor = CopyText,
            textContentColor = CopyText,
        )
    }


    if (confirmDeleteAll) {
        AlertDialog(
            onDismissRequest = { if (!uiState.isSaving) confirmDeleteAll = false },
            title = { Text("Excluir todas as memórias?") },
            text = {
                Text(
                    "Todas as ${uiState.memories.size} memórias salvas neste dispositivo serão apagadas. Essa ação não pode ser desfeita.",
                    color = CopyMuted,
                )
            },
            confirmButton = {
                TextButton(
                    enabled = !uiState.isSaving && uiState.memories.isNotEmpty(),
                    onClick = {
                        confirmDeleteAll = false
                        memoryViewModel.deleteAll()
                    },
                ) { Text(if (uiState.isSaving) "Excluindo…" else "Excluir todas") }
            },
            dismissButton = {
                TextButton(
                    enabled = !uiState.isSaving,
                    onClick = { confirmDeleteAll = false },
                ) { Text("Cancelar") }
            },
            containerColor = CopySurface,
            titleContentColor = CopyText,
            textContentColor = CopyText,
        )
    }

    memoryToDelete?.let { memory ->
        AlertDialog(
            onDismissRequest = { memoryToDelete = null },
            title = { Text("Excluir memória?") },
            text = {
                Text(
                    "“${memory.title}” será removida do armazenamento local. Essa ação não pode ser desfeita.",
                    color = CopyMuted,
                )
            },
            confirmButton = {
                TextButton(
                    enabled = !uiState.isSaving,
                    onClick = {
                        memoryViewModel.delete(memory.id)
                        memoryToDelete = null
                    },
                ) { Text("Excluir") }
            },
            dismissButton = {
                TextButton(onClick = { memoryToDelete = null }) { Text("Cancelar") }
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
