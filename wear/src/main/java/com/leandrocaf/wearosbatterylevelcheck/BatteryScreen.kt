package com.leandrocaf.wearosbatterylevelcheck

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText

@Composable
fun BatteryScreen() {
    val state by BatteryListenerService.phoneBattery.collectAsState()
    val lastUrl by BatteryListenerService.lastReceivedUrl.collectAsState()
    val context = LocalContext.current

    Scaffold(timeText = { TimeText() }) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (state.level < 0) {
                    WaitingContent()
                } else {
                    BatteryContent(state)
                }
            }
            if (lastUrl != null) {
                Text(
                    text = "🌐",
                    style = MaterialTheme.typography.title3,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 10.dp)
                        .clickable {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(lastUrl)).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            try {
                                context.startActivity(intent)
                            } catch (_: ActivityNotFoundException) { }
                        }
                )
            }
        }
    }
}

@Composable
private fun WaitingContent() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CircularProgressIndicator()
        Text(
            text = "Aguardando\nsmartphone...",
            style = MaterialTheme.typography.caption2,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun BatteryContent(state: PhoneBatteryState) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            progress = state.level / 100f,
            modifier = Modifier.fillMaxSize(),
            indicatorColor = batteryColor(state.level),
            trackColor = MaterialTheme.colors.onSurface.copy(alpha = 0.1f)
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = "${state.level}%",
                style = MaterialTheme.typography.display1,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Smartphone",
                style = MaterialTheme.typography.caption2,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
            )
            if (state.isCharging) {
                Spacer(Modifier.height(2.dp))
                if (state.level >= 100) {
                    Text(
                        text = "Carregado",
                        style = MaterialTheme.typography.caption2,
                        color = Color(0xFF4CAF50)
                    )
                } else {
                    Text(
                        text = "⚡ Carregando",
                        style = MaterialTheme.typography.caption2,
                        color = Color(0xFFFFC107)
                    )
                }
            }
        }
    }
}

@Composable
private fun batteryColor(level: Int) = when {
    level < 20  -> Color(0xFFF44336) // vermelho
    level <= 50 -> Color(0xFFFFC107) // amarelo
    else        -> Color(0xFF4CAF50) // verde
}

@Preview(
    device = "id:wearos_small_round",
    showSystemUi = true,
    backgroundColor = 0xff000000,
    showBackground = true
)
@Composable
private fun BatteryScreenPreview() {
    BatteryScreen()
}
