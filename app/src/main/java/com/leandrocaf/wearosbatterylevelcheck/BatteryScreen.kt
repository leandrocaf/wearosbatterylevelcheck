package com.leandrocaf.wearosbatterylevelcheck

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.leandrocaf.wearosbatterylevelcheck.ui.theme.WearOsBatteryLevelCheckTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatteryScreen(viewModel: BatteryViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.silentRefresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Bateria do Relógio") })
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            when (val state = uiState) {
                is BatteryUiState.Loading -> LoadingContent()
                is BatteryUiState.NoWatchFound -> NoWatchContent(onRetry = viewModel::refresh)
                is BatteryUiState.Connected -> ConnectedContent(state = state, onRefresh = viewModel::refresh)
                is BatteryUiState.Error -> ErrorContent(message = state.message, onRetry = viewModel::refresh)
            }
        }
    }
}

@Composable
private fun LoadingContent() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator()
        Spacer(Modifier.height(16.dp))
        Text("Procurando relógio...")
    }
}

@Composable
private fun NoWatchContent(onRetry: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Nenhum relógio conectado",
            style = MaterialTheme.typography.titleMedium
        )
        Button(onClick = onRetry) {
            Text("Tentar novamente")
        }
    }
}

@Composable
private fun ConnectedContent(state: BatteryUiState.Connected, onRefresh: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(horizontal = 32.dp)
    ) {
        Text(
            text = state.watchName,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "${state.level}%",
            style = MaterialTheme.typography.displayLarge,
            fontWeight = FontWeight.Bold
        )
        if (state.isCharging) {
            Text(
                text = "Carregando",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(Modifier.height(16.dp))
        LinearProgressIndicator(
            progress = { state.level / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp),
            color = batteryColor(state.level),
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
        Spacer(Modifier.height(24.dp))
        OutlinedButton(onClick = onRefresh) {
            Text("Atualizar")
        }
    }
}

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Erro",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.error
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Button(onClick = onRetry) {
            Text("Tentar novamente")
        }
    }
}

@Composable
private fun batteryColor(level: Int) = when {
    level < 20  -> Color(0xFFF44336) // vermelho
    level <= 50 -> Color(0xFFFFC107) // amarelo
    else        -> Color(0xFF4CAF50) // verde
}

@Preview(showBackground = true)
@Composable
private fun ConnectedPreview() {
    WearOsBatteryLevelCheckTheme {
        ConnectedContent(
            state = BatteryUiState.Connected(level = 72, isCharging = false, watchName = "Galaxy Watch 6"),
            onRefresh = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun NoWatchPreview() {
    WearOsBatteryLevelCheckTheme {
        NoWatchContent(onRetry = {})
    }
}
