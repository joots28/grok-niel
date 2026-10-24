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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.otiniel.copy.ui.theme.CopyBorder
import com.otiniel.copy.ui.theme.CopyMuted
import com.otiniel.copy.ui.theme.CopyPrimary
import com.otiniel.copy.ui.theme.CopySurface
import com.otiniel.copy.ui.theme.CopySurfaceRaised
import com.otiniel.copy.ui.theme.CopyText
import androidx.compose.material3.Surface

private data class IntegrationOption(
    val name: String,
    val description: String,
    val icon: ImageVector,
)

@Composable
fun ControlScreen() {
    val options = listOf(
        IntegrationOption("Arquivos", "Acesso futuro apenas a arquivos escolhidos ou autorizados.", Icons.Rounded.FolderOpen),
        IntegrationOption("Calendário", "Consulta futura com permissão explícita.", Icons.Rounded.CalendarMonth),
        IntegrationOption("Notificações", "Integração futura sujeita às permissões do Android.", Icons.Rounded.Notifications),
        IntegrationOption("Recursos do aparelho", "Nenhum ajuste de sistema é executado nesta versão.", Icons.Rounded.PhoneAndroid),
    )
    val localToggles = remember { mutableStateMapOf<String, Boolean>() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 20.dp, end = 20.dp, top = 10.dp, bottom = 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(15.dp),
    ) {
        item {
            PageHeader(
                eyebrow = "PRIVACIDADE E PERMISSÕES",
                title = "Central de controle",
                subtitle = "Veja as integrações planejadas e mantenha o controle de cada autorização.",
            )
        }
        item {
            AccentCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Security, contentDescription = null, tint = CopyPrimary)
                    Spacer(Modifier.width(10.dp))
                    Text("Transparência primeiro", fontWeight = FontWeight.SemiBold)
                }
                Text(
                    "Os seletores abaixo guardam apenas um estado visual temporário. Eles não concedem permissões nem alteram o dispositivo.",
                    color = CopyMuted,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                )
                DemoLabel()
            }
        }
        items(options.size) { index ->
            val option = options[index]
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(19.dp),
                colors = CardDefaults.cardColors(containerColor = CopySurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, CopyBorder),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        color = CopySurfaceRaised,
                        shape = RoundedCornerShape(13.dp),
                    ) {
                        Icon(
                            option.icon,
                            contentDescription = null,
                            modifier = Modifier.padding(11.dp),
                            tint = CopyPrimary,
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(option.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text(option.description, color = CopyMuted, fontSize = 11.sp, lineHeight = 16.sp)
                        Text("AINDA NÃO CONECTADO", color = CopyMuted, fontSize = 9.sp, letterSpacing = 0.8.sp)
                    }
                    Switch(
                        checked = localToggles[option.name] ?: false,
                        onCheckedChange = { localToggles[option.name] = it },
                    )
                }
            }
        }
        item { LocalModelManagerCard() }

        item {
            Text(
                "Quando uma integração for implementada, ela deverá solicitar a permissão oficial do Android e informar os dados acessados.",
                color = CopyMuted,
                fontSize = 11.sp,
                lineHeight = 16.sp,
            )
        }

        item {
            AccentCard {
                Text("Proteção local de conteúdo", fontWeight = FontWeight.SemiBold)
                Text(
                    "O Copy usa AES-GCM fornecido pelo Android e mantém a chave no Android Keystore, sem dependência criptográfica paga.",
                    color = CopyMuted,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                )
                Text(
                    "Essa proteção cifra títulos e conteúdos, mas não o arquivo SQLite inteiro: identificadores, datas e relações continuam como metadados.",
                    color = CopyMuted,
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                )
            }
        }
    }
}
