package com.leandrocaf.wearosbatterylevelcheck

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.wear.compose.material.MaterialTheme
import com.google.android.gms.wearable.Wearable

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestPhoneBattery()
        setContent {
            MaterialTheme {
                BatteryScreen()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        requestPhoneBattery()
    }

    private fun requestPhoneBattery() {
        Wearable.getNodeClient(this).connectedNodes.addOnSuccessListener { nodes ->
            nodes.forEach { node ->
                Wearable.getMessageClient(this)
                    .sendMessage(node.id, REQUEST_PHONE_BATTERY_PATH, null)
            }
        }
    }

    companion object {
        const val REQUEST_PHONE_BATTERY_PATH = "/request_phone_battery"
    }
}
