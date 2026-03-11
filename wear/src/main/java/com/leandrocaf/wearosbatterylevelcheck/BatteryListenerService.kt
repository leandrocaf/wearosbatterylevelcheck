package com.leandrocaf.wearosbatterylevelcheck

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.BatteryManager
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.flow.MutableStateFlow

data class PhoneBatteryState(val level: Int = -1, val isCharging: Boolean = false)

class BatteryListenerService : WearableListenerService() {

    override fun onMessageReceived(event: MessageEvent) {
        when (event.path) {
            BATTERY_REQUEST_PATH -> respondWithWatchBattery(event.sourceNodeId)
            PHONE_BATTERY_PATH   -> updatePhoneBattery(event.data)
            SEND_URL_PATH        -> openUrlInBrowser(String(event.data))
        }
    }

    // Celular pediu a bateria do relógio → responde com o nível local
    private fun respondWithWatchBattery(sourceNodeId: String) {
        val batteryManager = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val level = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        val isCharging = batteryManager.isCharging
        val response = "$level,$isCharging"
        Wearable.getMessageClient(this)
            .sendMessage(sourceNodeId, BATTERY_RESPONSE_PATH, response.toByteArray())
    }

    // Celular enviou sua própria bateria → atualiza o StateFlow observado pela tela
    private fun updatePhoneBattery(data: ByteArray) {
        val parts = String(data).split(",")
        val level = parts.getOrNull(0)?.toIntOrNull() ?: return
        val isCharging = parts.getOrNull(1)?.toBooleanStrictOrNull() ?: false
        phoneBattery.value = PhoneBatteryState(level, isCharging)
    }

    // Celular enviou um URL → salva no StateFlow e tenta abrir no navegador do relógio
    private fun openUrlInBrowser(url: String) {
        val normalizedUrl = Regex("^\\w+://").find(url)?.let { m ->
            m.value.lowercase() + url.substring(m.value.length)
        } ?: url
        lastReceivedUrl.value = normalizedUrl
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_LAST_URL, normalizedUrl).apply()
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(normalizedUrl)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            // Sem navegador instalado — usuário pode abrir pelo ícone 🌐 na tela
        }
    }

    companion object {
        const val BATTERY_REQUEST_PATH = "/battery_request"
        const val BATTERY_RESPONSE_PATH = "/battery_response"
        const val PHONE_BATTERY_PATH = "/phone_battery"
        const val SEND_URL_PATH = "/send_url"
        const val PREFS_NAME = "battery_prefs"
        const val KEY_LAST_URL = "last_url"
        val phoneBattery = MutableStateFlow(PhoneBatteryState())
        val lastReceivedUrl = MutableStateFlow<String?>(null)
    }
}
