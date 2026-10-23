package com.otiniel.copy.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
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
    val error: String? = null,
)

class ConversationViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ConversationRepository(application.applicationContext)
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
                    val activeId = sessionId ?: repository.createSession(initialPrompt)
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
                val messages = withContext(Dispatchers.IO) {
                    repository.addMessage(activeId, ROLE_USER, clean)
                    repository.addMessage(
                        activeId,
                        ROLE_ASSISTANT,
                        "Mensagem salva localmente. A IA ainda não está conectada, então nenhuma resposta de modelo foi gerada.",
                    )
                    repository.getMessages(activeId)
                }
                _uiState.update { it.copy(messages = messages, isSending = false, error = null) }
                refreshSessions()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(
                        isSending = false,
                        error = "Não foi possível salvar a mensagem. Tente novamente.",
                    )
                }
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

    override fun onCleared() {
        repository.close()
        super.onCleared()
    }

    private companion object {
        const val ROLE_USER = "user"
        const val ROLE_ASSISTANT = "assistant"
        const val MAX_MESSAGE_LENGTH = 12000
    }
}
