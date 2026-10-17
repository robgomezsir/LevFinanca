package com.example.lev.ui.screens.sync

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lev.data.repository.SyncLogEntry
import com.example.lev.data.repository.SyncMode
import com.example.lev.data.repository.SyncRepository
import com.example.lev.data.repository.SyncUiState
import com.example.lev.domain.SyncConflict
import com.example.lev.domain.SyncException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SyncActionState(
    val busy: Boolean = false,
    val message: String? = null,
    val isError: Boolean = false,
    val pendingConfirm: ConfirmKind? = null,
    val confirmCounts: Map<String, Int> = emptyMap(),
)

enum class ConfirmKind { PUSH_REPLACE, PULL_REPLACE }

class SyncViewModel(
    private val syncRepository: SyncRepository,
) : ViewModel() {
    val uiState: StateFlow<SyncUiState> =
        syncRepository.state.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SyncUiState(),
        )
    val syncLog: StateFlow<List<SyncLogEntry>> =
        syncRepository.syncLog.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )
    val conflicts: StateFlow<List<SyncConflict>> =
        syncRepository.conflicts.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    private val _action = MutableStateFlow(SyncActionState())
    val action: StateFlow<SyncActionState> = _action.asStateFlow()

    fun saveConnection(
        url: String,
        secret: String,
    ) {
        viewModelScope.launch {
            setBusy()
            runCatching { syncRepository.saveConnection(url, secret) }
                .onSuccess { setMessage("Conexão salva. Toque em Testar conexão.") }
                .onFailure { setError(it.message ?: "URL inválida.") }
        }
    }

    fun testConnection() {
        viewModelScope.launch {
            setBusy()
            runCatching { syncRepository.testConnection().getOrThrow() }
                .onSuccess { setMessage(it) }
                .onFailure { setError(it.message ?: "Falha no teste.") }
        }
    }

    fun syncNow() {
        viewModelScope.launch {
            setBusy()
            runCatching { syncRepository.syncMerge() }
                .onSuccess { _action.value = SyncActionState() }
                .onFailure { setError(it.message ?: "Falha no sync.") }
        }
    }

    fun setMode(mode: SyncMode) {
        viewModelScope.launch { syncRepository.setMode(mode) }
    }

    fun setAutoSync(enabled: Boolean) {
        viewModelScope.launch { syncRepository.setAutoSync(enabled) }
    }

    fun askPushReplace() {
        viewModelScope.launch {
            setBusy()
            runCatching { syncRepository.localCounts() }
                .onSuccess {
                    _action.value = SyncActionState(pendingConfirm = ConfirmKind.PUSH_REPLACE, confirmCounts = it)
                }
                .onFailure { setError(it.message ?: "Falha ao contar registros.") }
        }
    }

    fun askPullReplace() {
        viewModelScope.launch {
            setBusy()
            runCatching { syncRepository.localCounts() }
                .onSuccess {
                    _action.value = SyncActionState(pendingConfirm = ConfirmKind.PULL_REPLACE, confirmCounts = it)
                }
                .onFailure { setError(it.message ?: "Falha ao contar registros.") }
        }
    }

    fun confirmReplace() {
        val kind = _action.value.pendingConfirm ?: return
        viewModelScope.launch {
            setBusy()
            runCatching {
                if (kind == ConfirmKind.PUSH_REPLACE) syncRepository.pushReplace() else syncRepository.pullReplace()
            }.onSuccess { setMessage(it) }
                .onFailure {
                    val msg = (it as? SyncException)?.message ?: it.message ?: "Falha na operação."
                    setError(msg)
                }
        }
    }

    fun dismissConfirm() {
        _action.value = _action.value.copy(pendingConfirm = null, busy = false)
    }

    fun compact() {
        viewModelScope.launch {
            setBusy()
            runCatching { syncRepository.compactTombstones() }
                .onSuccess { setMessage(it) }
                .onFailure { setError(it.message ?: "Falha na compactação.") }
        }
    }

    fun clearMessage() {
        _action.value = _action.value.copy(message = null, isError = false, busy = false)
    }

    fun scriptText(): String = runCatching { syncRepository.scriptAssetText() }.getOrElse { "" }

    private fun setBusy() {
        _action.value = _action.value.copy(busy = true, message = null, isError = false)
    }

    private fun setMessage(text: String) {
        _action.value = SyncActionState(message = text)
    }

    private fun setError(text: String) {
        _action.value = SyncActionState(message = text, isError = true)
    }
}
