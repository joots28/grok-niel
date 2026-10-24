package com.otiniel.copy.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.otiniel.copy.data.LocalModelRepository
import com.otiniel.copy.data.LocalModelStatus
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class LocalModelUiState(
    val status: LocalModelStatus = LocalModelStatus(installed = false),
    val isWorking: Boolean = false,
    val error: String? = null,
    val notice: String? = null,
)

class LocalModelViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = LocalModelRepository(application.applicationContext)
    private val _uiState = MutableStateFlow(LocalModelUiState())
    val uiState = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            try {
                val status = withContext(Dispatchers.IO) { repository.status() }
                _uiState.update { it.copy(status = status, error = status.message, notice = null) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.update { it.copy(error = "Não foi possível verificar o modelo local.") }
            }
        }
    }

    fun importModel(uri: Uri) {
        if (_uiState.value.isWorking) return
        viewModelScope.launch {
            _uiState.update { it.copy(isWorking = true, error = null, notice = null) }
            try {
                val status = withContext(Dispatchers.IO) { repository.importModel(uri) }
                _uiState.update {
                    it.copy(
                        status = status,
                        isWorking = false,
                        error = null,
                        notice = "Modelo importado para o armazenamento privado deste dispositivo.",
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(
                        isWorking = false,
                        error = error.message?.take(240)
                            ?: "Não foi possível importar o modelo selecionado.",
                        notice = null,
                    )
                }
            }
        }
    }

    fun removeModel() {
        if (_uiState.value.isWorking) return
        viewModelScope.launch {
            _uiState.update { it.copy(isWorking = true, error = null, notice = null) }
            try {
                val removed = withContext(Dispatchers.IO) { repository.removeModel() }
                val status = withContext(Dispatchers.IO) { repository.status() }
                _uiState.update {
                    it.copy(
                        status = status,
                        isWorking = false,
                        error = if (removed) null else "Não foi possível excluir o arquivo de modelo.",
                        notice = if (removed) "Modelo removido do dispositivo." else null,
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(isWorking = false, error = "Não foi possível remover o modelo local.", notice = null)
                }
            }
        }
    }

    fun reportError(message: String) {
        _uiState.update { it.copy(error = message.take(240), notice = null) }
    }

    companion object {
        fun sizeLabel(bytes: Long): String {
            if (bytes <= 0L) return "tamanho não identificado"
            val megabytes = bytes.toDouble() / (1024.0 * 1024.0)
            return String.format(Locale("pt", "BR"), "%.0f MB", megabytes)
        }
    }
}
