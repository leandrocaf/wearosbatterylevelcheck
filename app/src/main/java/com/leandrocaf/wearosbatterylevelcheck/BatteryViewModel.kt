package com.leandrocaf.wearosbatterylevelcheck

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed class LinkSendState {
    data object Idle : LinkSendState()
    data object Sending : LinkSendState()
    data class Success(val url: String) : LinkSendState()
    data class Error(val message: String) : LinkSendState()
}

sealed class BatteryUiState {
    data object Loading : BatteryUiState()
    data object NoWatchFound : BatteryUiState()
    data class Connected(
        val level: Int,
        val isCharging: Boolean,
        val watchName: String
    ) : BatteryUiState()
    data class Error(val message: String) : BatteryUiState()
}

class BatteryViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<BatteryUiState>(BatteryUiState.Loading)
    val uiState: StateFlow<BatteryUiState> = _uiState.asStateFlow()

    private val _linkSendState = MutableStateFlow<LinkSendState>(LinkSendState.Idle)
    val linkSendState: StateFlow<LinkSendState> = _linkSendState.asStateFlow()

    private val nodeClient = Wearable.getNodeClient(application)
    private val messageClient = Wearable.getMessageClient(application)

    private val messageListener = MessageClient.OnMessageReceivedListener { event: MessageEvent ->
        if (event.path == BATTERY_RESPONSE_PATH) {
            viewModelScope.launch {
                try {
                    val parts = String(event.data).split(",")
                    val level = parts.getOrNull(0)?.toIntOrNull() ?: 0
                    val isCharging = parts.getOrNull(1)?.toBooleanStrictOrNull() ?: false
                    val nodes = nodeClient.connectedNodes.await()
                    val watchName = nodes.find { it.id == event.sourceNodeId }?.displayName ?: "Relógio"
                    _uiState.value = BatteryUiState.Connected(level, isCharging, watchName)
                } catch (e: Exception) {
                    _uiState.value = BatteryUiState.Error("Erro ao ler dados da bateria")
                }
            }
        }
    }

    init {
        messageClient.addListener(messageListener)
        refresh()
        startPeriodicRefresh()
    }

    // Atualização explícita (botão): mostra loading
    fun refresh() {
        viewModelScope.launch {
            _uiState.value = BatteryUiState.Loading
            try {
                val nodes = nodeClient.connectedNodes.await()
                if (nodes.isEmpty()) {
                    _uiState.value = BatteryUiState.NoWatchFound
                    return@launch
                }
                nodes.forEach { node ->
                    messageClient.sendMessage(node.id, BATTERY_REQUEST_PATH, null).await()
                }
            } catch (e: Exception) {
                _uiState.value = BatteryUiState.Error(e.message ?: "Erro ao conectar com o relógio")
            }
        }
    }

    // Atualização silenciosa (periódica / onResume): não altera o estado visível
    fun silentRefresh() {
        viewModelScope.launch {
            try {
                val nodes = nodeClient.connectedNodes.await()
                nodes.forEach { node ->
                    messageClient.sendMessage(node.id, BATTERY_REQUEST_PATH, null).await()
                }
            } catch (_: Exception) {}
        }
    }

    private fun startPeriodicRefresh() {
        viewModelScope.launch {
            while (true) {
                delay(REFRESH_INTERVAL_MS)
                silentRefresh()
            }
        }
    }

    fun sendUrlToWatch(url: String) {
        viewModelScope.launch {
            _linkSendState.value = LinkSendState.Sending
            try {
                val nodes = nodeClient.connectedNodes.await()
                if (nodes.isEmpty()) {
                    _linkSendState.value = LinkSendState.Error("Nenhum relógio conectado")
                    return@launch
                }
                nodes.forEach { node ->
                    messageClient.sendMessage(node.id, SEND_URL_PATH, url.toByteArray()).await()
                }
                _linkSendState.value = LinkSendState.Success(url)
            } catch (e: Exception) {
                _linkSendState.value = LinkSendState.Error(e.message ?: "Erro ao enviar link")
            }
        }
    }

    fun dismissLinkState() {
        _linkSendState.value = LinkSendState.Idle
    }

    override fun onCleared() {
        messageClient.removeListener(messageListener)
        super.onCleared()
    }

    companion object {
        const val BATTERY_REQUEST_PATH = "/battery_request"
        const val BATTERY_RESPONSE_PATH = "/battery_response"
        const val SEND_URL_PATH = "/send_url"
        private const val REFRESH_INTERVAL_MS = 30_000L
    }
}
