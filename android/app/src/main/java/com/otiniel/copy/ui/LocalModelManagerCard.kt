package com.otiniel.copy.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.otiniel.copy.data.LocalModelRepository
import com.otiniel.copy.ui.theme.CopyMuted
import com.otiniel.copy.ui.theme.CopyPrimary
import com.otiniel.copy.ui.theme.CopySurface
import com.otiniel.copy.ui.theme.CopyText

@Composable
fun LocalModelManagerCard(modelViewModel: LocalModelViewModel = viewModel()) {
    val state by modelViewModel.uiState.collectAsState()
    val context = LocalContext.current
    var confirmRemove by remember { mutableStateOf(false) }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri -> if (uri != null) modelViewModel.importModel(uri) },
    )

    AccentCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.material3.Icon(
                Icons.Rounded.Memory,
                contentDescription = null,
                tint = CopyPrimary,
            )
            Spacer(Modifier.width(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("IA local no dispositivo", fontWeight = FontWeight.SemiBold)
                Text(
                    if (state.status.installed) {
                        "MODELO INSTALADO · " + LocalModelViewModel.sizeLabel(state.status.sizeBytes)
                    } else {
                        "MODELO AINDA NÃO INSTALADO"
                    },
                    color = CopyMuted,
                    fontSize = 9.sp,
                    letterSpacing = 0.7.sp,
                )
            }
        }

        Text(
            "O Copy pode gerar respostas sem enviar conversas para uma API de IA. O modelo é opcional, gratuito e fica no armazenamento privado do app.",
            color = CopyMuted,
            fontSize = 12.sp,
            lineHeight = 18.sp,
        )
        Text(
            "Modelo sugerido: Qwen3-0.6B (Apache-2.0), arquivo de " + LocalModelRepository.MODEL_SIZE_LABEL + ". Baixar usa sua conexão de internet; depois da importação, a geração de texto ocorre no aparelho. Modelos pequenos têm limitações de qualidade e velocidade.",
            color = CopyMuted,
            fontSize = 11.sp,
            lineHeight = 17.sp,
        )

        Button(
            onClick = {
                try {
                    val intent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(LocalModelRepository.MODEL_DOWNLOAD_URL),
                    )
                    context.startActivity(intent)
                } catch (_: Exception) {
                    modelViewModel.reportError("Não foi possível abrir o navegador para baixar o modelo.")
                }
            },
            enabled = !state.isWorking,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = CopyPrimary,
                contentColor = CopySurface,
            ),
        ) {
            androidx.compose.material3.Icon(Icons.Rounded.CloudDownload, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Baixar modelo gratuito")
        }

        TextButton(
            onClick = { importLauncher.launch(arrayOf("*/*")) },
            enabled = !state.isWorking,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (state.isWorking) "Processando arquivo…" else "Importar arquivo .litertlm")
        }

        if (state.status.installed) {
            TextButton(
                onClick = { confirmRemove = true },
                enabled = !state.isWorking,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Remover modelo deste dispositivo")
            }
        }

        Row(verticalAlignment = Alignment.Top) {
            androidx.compose.material3.Icon(
                Icons.Rounded.Security,
                contentDescription = null,
                tint = CopyMuted,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "O importador confere a assinatura inicial e copia o arquivo para a área privada do app; o runtime valida o modelo ao carregá-lo. Não é necessário conceder acesso geral aos arquivos. Nenhum modelo é baixado automaticamente.",
                color = CopyMuted,
                fontSize = 10.sp,
                lineHeight = 15.sp,
            )
        }

        state.error?.let {
            Text(it, color = CopyText, fontSize = 12.sp)
        }
        state.notice?.let {
            Text(it, color = CopyMuted, fontSize = 12.sp)
        }
    }

    if (confirmRemove) {
        AlertDialog(
            onDismissRequest = { confirmRemove = false },
            title = { Text("Remover modelo local?") },
            text = {
                Text("As respostas offline ficarão indisponíveis até que um modelo seja importado novamente. Isso não exclui seu histórico nem suas memórias.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmRemove = false
                        modelViewModel.removeModel()
                    },
                ) {
                    Text("Remover")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmRemove = false }) {
                    Text("Cancelar")
                }
            },
        )
    }
}
