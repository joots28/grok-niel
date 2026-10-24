package com.otiniel.copy.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.otiniel.copy.core.LocalAssistantEngine
import com.otiniel.copy.core.SavedMemoryContext
import com.otiniel.copy.core.LuoPromptBuilder
import com.otiniel.copy.data.LocalModelRepository
import com.otiniel.copy.data.MemoryRepository
import com.otiniel.copy.data.ConversationMessage
import com.otiniel.copy.data.ConversationRepository
import com.otiniel.copy.data.ConversationSession
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ConversationUiState(
    val sessions: List<ConversationSession> = emptyList(),
    val activeSessionId: Long? = null,
    val messages: List<ConversationMessage> = emptyList(),
    val isLoading: Boolean = false,
    val isSending: Boolean = false,
    val isLocalModelInstalled: Boolean = false,
    val error: String? = null,
)

class ConversationViewModel(application: Application) : AndroidViewModel(application) {
    private val appContext = application.applicationContext
    private val repository = ConversationRepository(appContext)
    private val memoryRepository = MemoryRepository(appContext)
    private val modelRepository = LocalModelRepository(appContext)
    private val assistantEngine = LocalAssistantEngine(appContext, modelRepository)
    private val _uiState = MutableStateFlow(ConversationUiState())
    val uiState = _uiState.asStateFlow()

    fun refreshSessions() {
        viewModelScope.launch {
            try {
                val sessions = withContext(Dispatchers.IO) { repository.getSessions() }
                _uiState.update { it.copy(sessions = sessions) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.update { it.copy(error = "Não foi possível carregar o histórico local.") }
            }
        }
    }

    fun openSession(sessionId: Long?, initialPrompt: String) {
        _uiState.update { it.copy(isLocalModelInstalled = modelRepository.status().installed) }
        val current = _uiState.value
        if (sessionId != null && current.activeSessionId == sessionId && !current.isLoading) return
        if (sessionId == null && current.activeSessionId != null && !current.isLoading) {
            // Opening a new conversation must not reuse the previous session.
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    activeSessionId = sessionId,
                    messages = emptyList(),
                    isLoading = true,
                    isSending = false,
                    error = null,
                )
            }
            try {
                val result = withContext(Dispatchers.IO) {
                    val activeId = sessionId ?: repository.createSession()
                    activeId to repository.getMessages(activeId)
                }
                _uiState.update {
                    it.copy(
                        activeSessionId = result.first,
                        messages = result.second,
                        isLoading = false,
                        error = null,
                    )
                }
                refreshSessions()
                if (sessionId == null && initialPrompt.isNotBlank()) {
                    sendMessage(initialPrompt)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = "Não foi possível abrir esta conversa salva.")
                }
            }
        }
    }

    fun sendMessage(content: String) {
        val clean = content.trim()
        if (clean.isEmpty() || clean.length > MAX_MESSAGE_LENGTH) {
            _uiState.update {
                it.copy(error = if (clean.isEmpty()) "Escreva uma mensagem antes de enviar."
                else "A mensagem deve ter até $MAX_MESSAGE_LENGTH caracteres.")
            }
            return
        }
        if (_uiState.value.isSending || _uiState.value.isLoading) return

        val activeId = _uiState.value.activeSessionId
        if (activeId == null) {
            _uiState.update { it.copy(error = "Abra uma conversa antes de enviar mensagens.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSending = true, error = null) }
            try {
                val result = withContext(Dispatchers.IO) {
                    repository.addMessage(activeId, ROLE_USER, clean)
                    val priorMessages = repository.getMessages(activeId).dropLast(1)
                    val modelStatus = modelRepository.status()
                    val reply = if (!modelStatus.installed) {
                        "A IA local ainda não está ativada. Abra a Central de controle, baixe o modelo gratuito e importe o arquivo .litertlm. Nenhuma mensagem foi enviada a uma API remota."
                    } else {
                        val savedContext = SavedMemoryContext.build(memoryRepository.getAll())
                        assistantEngine.generate(
                            prompt = clean,
                            savedMemoryContext = savedContext,
                            history = priorMessages,
                        )
                    }
                    repository.addMessage(activeId, ROLE_ASSISTANT, reply)
                    repository.getMessages(activeId) to modelStatus.installed
                }
                _uiState.update {
                    it.copy(
                        messages = result.first,
                        isLocalModelInstalled = result.second,
                        isSending = false,
                        error = null,
                    )
                }
                refreshSessions()
            } catch (cancelled: CancellationException) {
                _uiState.update { it.copy(isSending = false) }
                throw cancelled
            } catch (_: Exception) {
                val latestMessages = try {
                    withContext(Dispatchers.IO) { repository.getMessages(activeId) }
                } catch (_: Exception) {
                    _uiState.value.messages
                }
                val modelInstalled = try {
                    withContext(Dispatchers.IO) { modelRepository.status().installed }
                } catch (_: Exception) {
                    false
                }
                _uiState.update {
                    it.copy(
                        messages = latestMessages,
                        isLocalModelInstalled = modelInstalled,
                        isSending = false,
                        error = "O modelo local não conseguiu gerar a resposta. Verifique o arquivo do modelo e tente novamente.",
                    )
                }
                refreshSessions()
            }
        }
    }

    fun deleteSession(sessionId: Long) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { repository.deleteSession(sessionId) }
                _uiState.update { state ->
                    state.copy(
                        sessions = state.sessions.filterNot { it.id == sessionId },
                        activeSessionId = if (state.activeSessionId == sessionId) null else state.activeSessionId,
                        messages = if (state.activeSessionId == sessionId) emptyList() else state.messages,
                    )
                }
                refreshSessions()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.update { it.copy(error = "Não foi possível excluir a conversa salva.") }
            }
        }
    }

    fun dismissError() = _uiState.update { it.copy(error = null) }

    fun releaseLocalModel() {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { assistantEngine.unload() }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // A release failure must not corrupt saved conversations.
            }
        }
    }

    override fun onCleared() {
        assistantEngine.close()
        memoryRepository.close()
        repository.close()
        super.onCleared()
    }

    private companion object {
        const val ROLE_USER = "user"
        const val ROLE_ASSISTANT = "assistant"
        const val MAX_MESSAGE_LENGTH = 12000
    }
}
