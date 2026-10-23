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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.otiniel.copy.data.DemoCommand
import com.otiniel.copy.data.commandCategories
import com.otiniel.copy.data.demoCommands
import com.otiniel.copy.ui.theme.CopyBorder
import com.otiniel.copy.ui.theme.CopyMuted
import com.otiniel.copy.ui.theme.CopyPrimary
import com.otiniel.copy.ui.theme.CopySurface
import com.otiniel.copy.ui.theme.CopySurfaceRaised
import com.otiniel.copy.ui.theme.CopyText

@Composable
fun CommandsScreen() {
    var search by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("Todos") }
    var selectedCommand by rememberSaveable { mutableStateOf<DemoCommand?>(null) }
    val filtered = demoCommands.filter { command ->
        val matchesText = command.title.contains(search, ignoreCase = true) ||
            command.description.contains(search, ignoreCase = true)
        val matchesCategory = category == "Todos" || command.category == category
        matchesText && matchesCategory
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
                eyebrow = "AUTOMAÇÕES",
                title = "Comandos",
                subtitle = "Explore as capacidades planejadas e veja o que ainda depende de conexão.",
            )
        }
        item { DemoLabel() }
        item {
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("Buscar comandos…", fontSize = 13.sp) },
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
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(commandCategories.size) { index ->
                    val item = commandCategories[index]
                    FilterChip(
                        selected = category == item,
                        onClick = { category = item },
                        label = { Text(item, fontSize = 11.sp) },
                    )
                }
            }
        }
        if (filtered.isEmpty()) {
            item {
                AccentCard {
                    Text("Nenhum comando encontrado", fontWeight = FontWeight.SemiBold)
                    Text("Tente outro termo ou categoria.", color = CopyMuted, fontSize = 12.sp)
                }
            }
        } else {
            items(filtered, key = { it.id }) { command ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { selectedCommand = command },
                    shape = RoundedCornerShape(19.dp),
                    colors = CardDefaults.cardColors(containerColor = CopySurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CopyBorder),
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Card(
                            shape = RoundedCornerShape(13.dp),
                            colors = CardDefaults.cardColors(containerColor = CopySurfaceRaised),
                        ) {
                            Icon(
                                Icons.Rounded.Bolt,
                                contentDescription = null,
                                modifier = Modifier.padding(11.dp),
                                tint = CopyPrimary,
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(7.dp),
                        ) {
                            Text(command.title, color = CopyText, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text(command.description, color = CopyMuted, fontSize = 12.sp, lineHeight = 17.sp)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(command.category.uppercase(), color = CopyPrimary, fontSize = 9.sp, letterSpacing = 0.8.sp)
                                Spacer(Modifier.weight(1f))
                                Text(command.access.uppercase(), color = CopyMuted, fontSize = 9.sp)
                            }
                        }
                    }
                }
            }
        }
        item {
            Text(
                "Os cartões descrevem capacidades previstas; nenhum comando executa ações do aparelho nesta versão.",
                color = CopyMuted,
                fontSize = 11.sp,
                lineHeight = 16.sp,
            )
        }
    }

    selectedCommand?.let { command ->
        AlertDialog(
            onDismissRequest = { selectedCommand = null },
            title = { Text(command.title) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(command.description)
                    DemoLabel(command.access)
                    Text("Status: catálogo visual. A execução real ainda não está conectada.", color = CopyMuted, fontSize = 12.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedCommand = null }) { Text("Fechar") }
            },
            containerColor = CopySurface,
            titleContentColor = CopyText,
            textContentColor = CopyText,
        )
    }
}
