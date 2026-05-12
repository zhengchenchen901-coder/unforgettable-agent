package com.unforgettable.memory.ui.debug

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAlert
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.unforgettable.memory.data.storage.entity.AiExtractionLogEntity
import com.unforgettable.memory.data.storage.entity.RawNotificationEntity
import com.unforgettable.memory.domain.notification.SupportedApps
import com.unforgettable.memory.ui.formatDateTime

private enum class DebugTab(val label: String) {
    Raw("Raw"),
    Logs("AI Logs"),
}

@Composable
fun DebugScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val application = context.applicationContext as Application
    val viewModel: DebugViewModel = viewModel(
        factory = ViewModelProvider.AndroidViewModelFactory.getInstance(application),
    )
    val rawNotifications by viewModel.rawNotifications.collectAsStateWithLifecycle()
    val extractionLogs by viewModel.extractionLogs.collectAsStateWithLifecycle()
    var selected by rememberSaveable { mutableStateOf(DebugTab.Raw) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Debug", style = MaterialTheme.typography.headlineMedium)
        Button(onClick = viewModel::injectDemoNotification) {
            Icon(Icons.Default.AddAlert, contentDescription = null)
            Text("注入模拟通知", modifier = Modifier.padding(start = 8.dp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DebugTab.entries.forEach { tab ->
                if (selected == tab) {
                    Button(onClick = { selected = tab }) { Text(tab.label) }
                } else {
                    OutlinedButton(onClick = { selected = tab }) { Text(tab.label) }
                }
            }
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            when (selected) {
                DebugTab.Raw -> {
                    if (rawNotifications.isEmpty()) {
                        item { EmptyDebugCard("还没有原始通知") }
                    } else {
                        items(rawNotifications, key = { it.id }) { RawNotificationCard(it) }
                    }
                }
                DebugTab.Logs -> {
                    if (extractionLogs.isEmpty()) {
                        item { EmptyDebugCard("还没有 AI 提取日志") }
                    } else {
                        items(extractionLogs, key = { it.id }) { ExtractionLogCard(it) }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyDebugCard(text: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Text(
            text = text,
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun RawNotificationCard(notification: RawNotificationEntity) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = SupportedApps.appLabel(notification.packageName),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(notification.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(notification.content, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = formatDateTime(notification.timestamp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ExtractionLogCard(log: AiExtractionLogEntity) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(log.finalDecision, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Text(log.requestText, style = MaterialTheme.typography.bodyMedium)
            log.error?.takeIf { it.isNotBlank() }?.let {
                Text("error: $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
            }
            Text(
                text = formatDateTime(log.createdAt),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

