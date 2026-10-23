package com.otiniel.copy.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.otiniel.copy.data.MemoryInputValidator
import com.otiniel.copy.data.MemoryItem
import com.otiniel.copy.data.MemoryRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class MemoryUiState(
    val memories: List<MemoryItem> = emptyList(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val error: String? = null,
    val notice: String? = null,
)

class MemoryViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = MemoryRepository(application.applicationContext)

    private val _uiState = MutableStateFlow(MemoryUiState())
    val uiState = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun save(
        id: Long?,
        title: String,
        detail: String,
        onFinished: (Boolean) -> Unit,
    ) {
        val validationMessage = MemoryInputValidator.validate(title, detail)
        if (validationMessage != null) {
            _uiState.update { it.copy(error = validationMessage, notice = null) }
            onFinished(false)
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null, notice = null) }
            try {
                val latestMemories = withContext(Dispatchers.IO) {
                    repository.save(id, title.trim(), detail.trim())
                    repository.getAll()
                }
                _uiState.update {
                    it.copy(
                        memories = latestMemories,
                        isSaving = false,
                        error = null,
                        notice = if (id == null) {
                            "Memória salva neste dispositivo."
                        } else {
                            "Memória atualizada neste dispositivo."
                        },
                    )
                }
                onFinished(true)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        error = "Não foi possível salvar a memória. Seus dados anteriores foram mantidos.",
                        notice = null,
                    )
                }
                onFinished(false)
            }
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null, notice = null) }
            try {
                val latestMemories = withContext(Dispatchers.IO) {
                    repository.delete(id)
                    repository.getAll()
                }
                _uiState.update {
                    it.copy(
                        memories = latestMemories,
                        isSaving = false,
                        error = null,
                        notice = "Memória excluída do dispositivo.",
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        error = "Não foi possível excluir a memória. Tente novamente.",
                        notice = null,
                    )
                }
            }
        }
    }

    fun dismissFeedback() {
        _uiState.update { it.copy(error = null, notice = null) }
    }

    private fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, notice = null) }
            try {
                val latestMemories = withContext(Dispatchers.IO) { repository.getAll() }
                _uiState.update { it.copy(memories = latestMemories, isLoading = false) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Não foi possível carregar as memórias salvas neste dispositivo.",
                    )
                }
            }
        }
    }

    override fun onCleared() {
        repository.close()
        super.onCleared()
    }
}
