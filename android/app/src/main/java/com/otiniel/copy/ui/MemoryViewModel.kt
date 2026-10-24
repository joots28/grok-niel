package com.otiniel.copy.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.otiniel.copy.data.MemoryInputValidator
import com.otiniel.copy.data.MemoryItem
import com.otiniel.copy.data.MemoryExportFormatter
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


    fun deleteAll() {
        if (_uiState.value.isSaving) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null, notice = null) }
            try {
                val result = withContext(Dispatchers.IO) {
                    val deletedCount = repository.deleteAll()
                    deletedCount to repository.getAll()
                }
                _uiState.update {
                    it.copy(
                        memories = result.second,
                        isSaving = false,
                        error = null,
                        notice = if (result.first == 0) {
                            "Não havia memórias para excluir."
                        } else {
                            "${result.first} memória(s) excluída(s) deste dispositivo."
                        },
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        error = "Não foi possível excluir todas as memórias. Tente novamente.",
                        notice = null,
                    )
                }
            }
        }
    }

    fun exportTo(uri: Uri) {
        if (_uiState.value.isSaving) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null, notice = null) }
            try {
                val exportedCount = withContext(Dispatchers.IO) {
                    val memories = repository.getAll()
                    val json = MemoryExportFormatter.toJson(memories, System.currentTimeMillis())
                    val output = getApplication<Application>().contentResolver.openOutputStream(uri)
                        ?: error("Unable to open selected export destination.")
                    output.bufferedWriter(Charsets.UTF_8).use { writer -> writer.write(json) }
                    memories.size
                }
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        error = null,
                        notice = "Exportação concluída: $exportedCount memória(s) em JSON.",
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        error = "Não foi possível exportar. As memórias locais não foram alteradas.",
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
