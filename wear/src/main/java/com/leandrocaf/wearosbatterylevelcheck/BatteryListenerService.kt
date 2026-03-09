package com.leandrocaf.wearosbatterylevelcheck

import android.content.Context
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
            PHONE_BATTERY_PATH -> updatePhoneBattery(event.data)
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

    companion object {
        const val BATTERY_REQUEST_PATH = "/battery_request"
        const val BATTERY_RESPONSE_PATH = "/battery_response"
        const val PHONE_BATTERY_PATH = "/phone_battery"
        val phoneBattery = MutableStateFlow(PhoneBatteryState())
    }
}

