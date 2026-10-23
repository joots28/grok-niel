package com.otiniel.copy.data

enum class CopyTab(val label: String) {
    HOME("Início"),
    COMMANDS("Comandos"),
    CONTROL("Controle"),
    MEMORY("Memória"),
    HISTORY("Histórico"),
}

data class DemoCommand(
    val id: Int,
    val title: String,
    val description: String,
    val category: String,
    val access: String = "Prévia",
)

data class MemoryItem(
    val id: Long,
    val title: String,
    val detail: String,
)

data class HistoryItem(
    val title: String,
    val detail: String,
    val time: String,
)

val commandCategories = listOf("Todos", "Produtividade", "Sistema", "Dispositivo")

val demoCommands = listOf(
    DemoCommand(1, "Resumir texto", "Transforma um conteúdo longo em pontos principais.", "Produtividade"),
    DemoCommand(2, "Organizar tarefas", "Agrupa tarefas em uma lista revisável.", "Produtividade"),
    DemoCommand(3, "Criar lembrete", "Prepara um lembrete para configurar depois.", "Produtividade"),
    DemoCommand(4, "Pesquisar arquivos", "Planejada para buscar arquivos autorizados no aparelho.", "Sistema", "Permissão necessária"),
    DemoCommand(5, "Consultar calendário", "Planejada para consultar eventos após autorização.", "Sistema", "Permissão necessária"),
    DemoCommand(6, "Rotinas do dispositivo", "Atalhos do aparelho ainda não conectados.", "Dispositivo", "Não conectado"),
)

val demoHistory = listOf(
    HistoryItem("Planejamento da semana", "Organização e prioridades", "Ontem"),
    HistoryItem("Notas de estudo", "Resumo de um conteúdo", "Há 2 dias"),
    HistoryItem("Ideias para projeto", "Rascunho de ideias", "Há 4 dias"),
)

data class ConversationSession(
    val id: Long,
    val title: String,
    val preview: String,
    val updatedAt: Long,
    val messageCount: Int,
)

data class ConversationMessage(
    val id: Long,
    val sessionId: Long,
    val role: String,
    val content: String,
    val createdAt: Long,
)
