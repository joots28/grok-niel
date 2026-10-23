package com.otiniel.copy.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.otiniel.copy.data.CopyTab
import com.otiniel.copy.ui.theme.CopyBackground
import com.otiniel.copy.ui.theme.CopyBorder
import com.otiniel.copy.ui.theme.CopyMuted
import com.otiniel.copy.ui.theme.CopyPrimary
import com.otiniel.copy.ui.theme.CopySurface
import com.otiniel.copy.ui.theme.CopySurfaceRaised
import com.otiniel.copy.ui.theme.CopyText

@Composable
fun CopyApp() {
    var selectedTabName by rememberSaveable { mutableStateOf(CopyTab.HOME.name) }
    var conversationOpen by rememberSaveable { mutableStateOf(false) }
    var promptToOpen by rememberSaveable { mutableStateOf("") }
    var activeConversationId by rememberSaveable { mutableStateOf<Long?>(null) }
    val selectedTab = CopyTab.valueOf(selectedTabName)

    Scaffold(
        containerColor = CopyBackground,
        topBar = {
            if (conversationOpen) {
                ConversationTopBar(onBack = { conversationOpen = false })
            } else {
                MainTopBar()
            }
        },
        bottomBar = {
            if (!conversationOpen) {
                CopyNavigationBar(
                    selectedTab = selectedTab,
                    onSelect = { selectedTabName = it.name },
                )
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            if (conversationOpen) {
                ConversationScreen(
                    sessionId = activeConversationId,
                    initialPrompt = promptToOpen,
                    onBack = { conversationOpen = false },
                )
            } else {
                when (selectedTab) {
                    CopyTab.HOME -> HomeScreen(
                        onOpenConversation = { prompt ->
                            activeConversationId = null
                            promptToOpen = prompt
                            conversationOpen = true
                        },
                        onOpenSavedConversation = { sessionId ->
                            activeConversationId = sessionId
                            promptToOpen = ""
                            conversationOpen = true
                        },
                        onOpenHistory = { selectedTabName = CopyTab.HISTORY.name },
                    )
                    CopyTab.COMMANDS -> CommandsScreen()
                    CopyTab.CONTROL -> ControlScreen()
                    CopyTab.MEMORY -> MemoryScreen()
                    CopyTab.HISTORY -> HistoryScreen(
                        onOpenConversation = { sessionId ->
                            activeConversationId = sessionId
                            promptToOpen = ""
                            conversationOpen = true
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun MainTopBar() {
    androidx.compose.foundation.layout.Column(
        modifier = Modifier
            .padding(horizontal = 20.dp, vertical = 13.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CopyWordmark()
            Spacer(modifier = Modifier.weight(1f))
            Surface(
                color = CopySurfaceRaised,
                shape = CircleShape,
                border = androidx.compose.foundation.BorderStroke(1.dp, CopyBorder),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Surface(
                        modifier = Modifier.size(6.dp),
                        color = CopyMuted,
                        shape = CircleShape,
                    ) {}
                    Text("OFFLINE", fontSize = 9.sp, color = CopyMuted, letterSpacing = 1.sp)
                }
            }
        }
        Surface(
            color = CopySurface,
            shape = CircleShape,
            border = androidx.compose.foundation.BorderStroke(1.dp, CopyBorder),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Surface(modifier = Modifier.size(6.dp), color = CopyPrimary, shape = CircleShape) {}
                Text(
                    "INTERFACE PREVIEW",
                    color = CopyPrimary,
                    fontSize = 9.sp,
                    letterSpacing = 1.1.sp,
                )
                Spacer(modifier = Modifier.weight(1f))
                Text("0.1.0", color = CopyMuted, fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun ConversationTopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.Rounded.ArrowBack, contentDescription = "Voltar", tint = CopyText)
        }
        Spacer(modifier = Modifier.width(5.dp))
        androidx.compose.foundation.layout.Column {
            Text("Conversa", fontSize = 16.sp, color = CopyText)
            Text("Sessão local de demonstração", fontSize = 11.sp, color = CopyMuted)
        }
        Spacer(modifier = Modifier.weight(1f))
        DemoLabel("DEMO")
    }
}

private data class TabVisual(val tab: CopyTab, val icon: ImageVector)

@Composable
private fun CopyNavigationBar(selectedTab: CopyTab, onSelect: (CopyTab) -> Unit) {
    val tabs = listOf(
        TabVisual(CopyTab.HOME, Icons.Rounded.Home),
        TabVisual(CopyTab.COMMANDS, Icons.Rounded.Terminal),
        TabVisual(CopyTab.CONTROL, Icons.Rounded.Tune),
        TabVisual(CopyTab.MEMORY, Icons.Rounded.Psychology),
        TabVisual(CopyTab.HISTORY, Icons.Rounded.History),
    )
    NavigationBar(
        containerColor = CopySurface,
        contentColor = CopyText,
    ) {
        tabs.forEach { item ->
            NavigationBarItem(
                selected = selectedTab == item.tab,
                onClick = { onSelect(item.tab) },
                icon = { Icon(item.icon, contentDescription = item.tab.label) },
                label = { Text(item.tab.label, fontSize = 9.sp, maxLines = 1) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = CopyPrimary,
                    selectedTextColor = CopyPrimary,
                    indicatorColor = CopySurfaceRaised,
                    unselectedIconColor = CopyMuted,
                    unselectedTextColor = CopyMuted,
                ),
            )
        }
    }
}
