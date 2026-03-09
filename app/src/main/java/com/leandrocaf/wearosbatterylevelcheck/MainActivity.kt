package com.leandrocaf.wearosbatterylevelcheck

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.google.android.gms.wearable.Wearable
import com.leandrocaf.wearosbatterylevelcheck.ui.theme.WearOsBatteryLevelCheckTheme

class MainActivity : ComponentActivity() {

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != Intent.ACTION_BATTERY_CHANGED) return
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING
                    || status == BatteryManager.BATTERY_STATUS_FULL
            sendBatteryToWatch(level * 100 / scale, isCharging)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        setContent {
            WearOsBatteryLevelCheckTheme {
                BatteryScreen()
            }
        }
    }

    override fun onDestroy() {
        unregisterReceiver(batteryReceiver)
        super.onDestroy()
    }

    private fun sendBatteryToWatch(level: Int, isCharging: Boolean) {
        val data = "$level,$isCharging".toByteArray()
        Wearable.getNodeClient(this).connectedNodes.addOnSuccessListener { nodes ->
            nodes.forEach { node ->
                Wearable.getMessageClient(this).sendMessage(node.id, PHONE_BATTERY_PATH, data)
            }
        }
    }

    companion object {
        const val PHONE_BATTERY_PATH = "/phone_battery"
    }
}
