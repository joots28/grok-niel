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
                Text("Licenças de terceiros", fontWeight = FontWeight.SemiBold)
                Text("SQLCipher for Android Community Edition · versão 4.19.1", color = CopyPrimary, fontSize = 12.sp)
                Text(
                    """
                    Copyright (c) 2008-2023, ZETETIC LLC
                    All rights reserved.

                    Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

                    1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
                    2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
                    3. Neither the name of ZETETIC LLC nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.

                    THIS SOFTWARE IS PROVIDED BY ZETETIC LLC "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL ZETETIC LLC BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
                    """.trimIndent(),
                    color = CopyMuted,
                    fontSize = 10.sp,
                    lineHeight = 15.sp,
                )
            }
        }
    }
}
