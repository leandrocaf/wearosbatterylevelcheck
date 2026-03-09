package com.leandrocaf.wearosbatterylevelcheck

import android.content.Context
import android.os.BatteryManager
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService

class PhoneBatteryListenerService : WearableListenerService() {

    override fun onMessageReceived(event: MessageEvent) {
        if (event.path != REQUEST_PHONE_BATTERY_PATH) return

        val batteryManager = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val level = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        val isCharging = batteryManager.isCharging
        val response = "$level,$isCharging"

        Wearable.getMessageClient(this)
            .sendMessage(event.sourceNodeId, PHONE_BATTERY_PATH, response.toByteArray())
    }

    companion object {
        const val REQUEST_PHONE_BATTERY_PATH = "/request_phone_battery"
        const val PHONE_BATTERY_PATH = "/phone_battery"
    }
}
