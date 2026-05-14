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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAlert
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.unforgettable.memory.data.storage.entity.AiExtractionLogEntity
import com.unforgettable.memory.data.storage.entity.MemoryAccessLogEntity
import com.unforgettable.memory.data.storage.entity.MemoryItemEntity
import com.unforgettable.memory.data.storage.entity.RawNotificationEntity
import com.unforgettable.memory.domain.memory.MemoryStatus
import com.unforgettable.memory.domain.notification.SupportedApps
import com.unforgettable.memory.service.NotificationListenerHealthSnapshot
import com.unforgettable.memory.ui.formatDateTime

private enum class DebugTab(val label: String) {
    Raw("Raw"),
    Logs("AI Logs"),
    Memory("Memory"),
    Health("Health"),
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
    val memoryItems by viewModel.memoryItems.collectAsStateWithLifecycle()
    val memoryAccessLogs by viewModel.memoryAccessLogs.collectAsStateWithLifecycle()
    val listenerHealth by viewModel.listenerHealth.collectAsStateWithLifecycle()
    val seedState by viewModel.seedState.collectAsStateWithLifecycle()
    var selected by rememberSaveable { mutableStateOf(DebugTab.Raw) }
    val rawListState = rememberLazyListState()
    val logsListState = rememberLazyListState()
    val memoryListState = rememberLazyListState()
    val healthListState = rememberLazyListState()
    val activeListState = when (selected) {
        DebugTab.Raw -> rawListState
        DebugTab.Logs -> logsListState
        DebugTab.Memory -> memoryListState
        DebugTab.Health -> healthListState
    }
    var pendingMemoryScrollRestore by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    LaunchedEffect(memoryItems.size, memoryAccessLogs.size, selected) {
        val restore = pendingMemoryScrollRestore
        if (selected == DebugTab.Memory && restore != null) {
            val maxIndex = (memoryDebugItemCount(memoryItems.size, memoryAccessLogs.size) - 1)
                .coerceAtLeast(0)
            memoryListState.scrollToItem(
                index = restore.first.coerceAtMost(maxIndex),
                scrollOffset = restore.second,
            )
            pendingMemoryScrollRestore = null
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Debug", style = MaterialTheme.typography.headlineMedium)
        NotificationSeedCard(
            state = seedState,
            onSeedRaw = viewModel::seedRawNotifications,
            onShowVisible = viewModel::showVisibleNotifications,
            onSeedRawAndVisible = viewModel::seedRawAndVisibleNotifications,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
        ) {
            DebugTab.entries.forEach { tab ->
                if (selected == tab) {
                    Button(onClick = { selected = tab }) { Text(tab.label) }
                } else {
                    OutlinedButton(onClick = { selected = tab }) { Text(tab.label) }
                }
            }
        }

        LazyColumn(
            state = activeListState,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            when (selected) {
                DebugTab.Raw -> {
                    if (rawNotifications.isEmpty()) {
                        item(key = "raw-empty") { EmptyDebugCard("还没有原始通知") }
                    } else {
                        items(rawNotifications, key = { "raw-${it.id}" }) { RawNotificationCard(it) }
                    }
                }
                DebugTab.Logs -> {
                    if (extractionLogs.isEmpty()) {
                        item(key = "logs-empty") { EmptyDebugCard("还没有 AI 提取日志") }
                    } else {
                        items(extractionLogs, key = { "log-${it.id}" }) { ExtractionLogCard(it) }
                    }
                }
                DebugTab.Memory -> {
                    item(key = "memory-manual") {
                        ManualMemoryCard(onAdd = viewModel::createUserMemory)
                    }
                    if (memoryItems.isEmpty()) {
                        item(key = "memory-empty") { EmptyDebugCard("No memory items yet") }
                    } else {
                        items(memoryItems, key = { "memory-${it.id}" }) { memory ->
                            MemoryCard(
                                memory = memory,
                                onConfirm = viewModel::confirmMemory,
                                onIgnore = viewModel::ignoreMemory,
                                onArchive = viewModel::archiveMemory,
                                onDelete = { id ->
                                    pendingMemoryScrollRestore = memoryListState.firstVisibleItemIndex to
                                        memoryListState.firstVisibleItemScrollOffset
                                    viewModel.deleteMemory(id)
                                },
                            )
                        }
                    }
                    item(key = "memory-access-header") {
                        Text("Recent memory access", style = MaterialTheme.typography.titleMedium)
                    }
                    if (memoryAccessLogs.isEmpty()) {
                        item(key = "memory-access-empty") { EmptyDebugCard("No memory access logs yet") }
                    } else {
                        items(memoryAccessLogs, key = { "memory-access-${it.id}" }) { log ->
                            MemoryAccessLogCard(log)
                        }
                    }
                }
                DebugTab.Health -> {
                    item(key = "health-listener") {
                        ListenerHealthCard(
                            health = listenerHealth,
                            onRefresh = viewModel::refreshListenerHealth,
                        )
                    }
                }
            }
        }
    }
}

private fun memoryDebugItemCount(memoryCount: Int, memoryAccessLogCount: Int): Int {
    val memoryRows = if (memoryCount == 0) 1 else memoryCount
    val accessLogRows = if (memoryAccessLogCount == 0) 1 else memoryAccessLogCount
    return 1 + memoryRows + 1 + accessLogRows
}

@Composable
private fun NotificationSeedCard(
    state: DebugSeedUiState,
    onSeedRaw: (Int) -> Unit,
    onShowVisible: (Int) -> Unit,
    onSeedRawAndVisible: (Int) -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Notification seeds", style = MaterialTheme.typography.titleMedium)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
            ) {
                Button(
                    enabled = !state.isRunning,
                    onClick = { onSeedRaw(1) },
                ) {
                    Icon(Icons.Default.AddAlert, contentDescription = null)
                    Text("入库 1", modifier = Modifier.padding(start = 6.dp))
                }
                Button(
                    enabled = !state.isRunning,
                    onClick = { onSeedRaw(25) },
                ) {
                    Icon(Icons.Default.AddAlert, contentDescription = null)
                    Text("入库 25", modifier = Modifier.padding(start = 6.dp))
                }
                Button(
                    enabled = !state.isRunning,
                    onClick = { onSeedRaw(100) },
                ) {
                    Icon(Icons.Default.AddAlert, contentDescription = null)
                    Text("入库 100", modifier = Modifier.padding(start = 6.dp))
                }
                OutlinedButton(
                    enabled = !state.isRunning,
                    onClick = { onShowVisible(25) },
                ) {
                    Icon(Icons.Default.AddAlert, contentDescription = null)
                    Text("通知栏 25", modifier = Modifier.padding(start = 6.dp))
                }
                OutlinedButton(
                    enabled = !state.isRunning,
                    onClick = { onSeedRawAndVisible(25) },
                ) {
                    Icon(Icons.Default.AddAlert, contentDescription = null)
                    Text("双模式 25", modifier = Modifier.padding(start = 6.dp))
                }
            }
            state.message?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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
private fun ListenerHealthCard(
    health: NotificationListenerHealthSnapshot,
    onRefresh: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Text("Notification Listener", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = if (health.notificationAccessEnabled) "Access enabled" else "Access disabled",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (health.notificationAccessEnabled) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.secondary
                        },
                    )
                }
                OutlinedButton(onClick = onRefresh) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Text("Refresh", modifier = Modifier.padding(start = 8.dp))
                }
            }
            HealthRow("Last event", health.lastEvent.orEmpty(), health.lastEventAt)
            HealthRow("Created", null, health.lastCreatedAt)
            HealthRow("Connected", null, health.lastConnectedAt)
            HealthRow("Disconnected", null, health.lastDisconnectedAt)
            HealthRow("Destroyed", null, health.lastDestroyedAt)
            HealthRow("Health check", null, health.lastHealthCheckAt)
            HealthRow("Rebind requested", null, health.lastRebindRequestedAt)
            HealthRow("Rebind skipped", null, health.lastRebindSkippedAt)
            HealthRow("Rebind failed", null, health.lastRebindFailedAt)
        }
    }
}

@Composable
private fun HealthRow(label: String, value: String?, timestamp: Long?) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = listOfNotNull(
                value?.takeIf { it.isNotBlank() },
                timestamp?.let { formatDateTime(it) },
            ).joinToString(" / ").ifBlank { "-" },
            style = MaterialTheme.typography.bodySmall,
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

@Composable
private fun MemoryCard(
    memory: MemoryItemEntity,
    onConfirm: (Long) -> Unit,
    onIgnore: (Long) -> Unit,
    onArchive: (Long) -> Unit,
    onDelete: (Long) -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Text(memory.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        text = "${memory.type} / ${memory.status}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Text(
                    text = "${(memory.confidence * 100).toInt()}%",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(memory.content, style = MaterialTheme.typography.bodyMedium)
            DebugField("Keywords", memory.keywords.joinToString(", ").ifBlank { "-" })
            DebugField("Memory source", memory.source)
            DebugField("App source", memory.sourcePackage?.let { SupportedApps.appLabel(it) } ?: "-")
            DebugField("Importance", "%.2f".format(memory.importance))
            DebugField("Evidence", memory.evidenceCount.toString())
            DebugField("Last seen", formatDateTime(memory.lastSeenAt))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
            ) {
                if (memory.status == MemoryStatus.PENDING) {
                    Button(onClick = { onConfirm(memory.id) }) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Text("Confirm", modifier = Modifier.padding(start = 6.dp))
                    }
                    OutlinedButton(onClick = { onIgnore(memory.id) }) {
                        Icon(Icons.Default.Close, contentDescription = null)
                        Text("Ignore", modifier = Modifier.padding(start = 6.dp))
                    }
                }
                if (memory.status == MemoryStatus.ACTIVE) {
                    OutlinedButton(onClick = { onArchive(memory.id) }) {
                        Icon(Icons.Default.Archive, contentDescription = null)
                        Text("Archive", modifier = Modifier.padding(start = 6.dp))
                    }
                }
                OutlinedButton(onClick = { onDelete(memory.id) }) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Text("Delete", modifier = Modifier.padding(start = 6.dp))
                }
            }
        }
    }
}

@Composable
private fun ManualMemoryCard(
    onAdd: (String, String, String, String) -> Unit,
) {
    var type by rememberSaveable { mutableStateOf("preference") }
    var title by rememberSaveable { mutableStateOf("") }
    var content by rememberSaveable { mutableStateOf("") }
    var keywords by rememberSaveable { mutableStateOf("") }
    val typeOptions = listOf(
        "person" to "Person",
        "project" to "Project",
        "preference" to "Preference",
        "pattern" to "Pattern",
    )

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Add trusted memory", style = MaterialTheme.typography.titleMedium)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Type", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                ) {
                    typeOptions.forEach { (value, label) ->
                        FilterChip(
                            selected = type == value,
                            onClick = { type = value },
                            label = { Text(label) },
                        )
                    }
                }
            }
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                label = { Text("Content") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = keywords,
                onValueChange = { keywords = it },
                label = { Text("Keywords") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                enabled = title.isNotBlank() && content.isNotBlank(),
                onClick = {
                    onAdd(type, title, content, keywords)
                    title = ""
                    content = ""
                    keywords = ""
                },
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Text("Add memory", modifier = Modifier.padding(start = 6.dp))
            }
        }
    }
}

@Composable
private fun MemoryAccessLogCard(log: MemoryAccessLogEntity) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text("rawNotificationId=${log.rawNotificationId}", style = MaterialTheme.typography.titleSmall)
            DebugField(
                label = "Retrieved",
                value = log.retrievedMemoryIds.joinToString(", ").ifBlank { "none" },
            )
            Text(log.requestText, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = formatDateTime(log.createdAt),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DebugField(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
