package com.unforgettable.memory.ui.tasks

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AssistChip
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.unforgettable.memory.data.storage.entity.TaskEntity
import com.unforgettable.memory.domain.notification.SupportedApps
import com.unforgettable.memory.ui.formatDateTime
import com.unforgettable.memory.ui.formatReminder

private enum class TaskFilter(val label: String) {
    Pending("待办"),
    Overdue("已逾期"),
    Completed("已完成"),
}

@Composable
fun TasksScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val application = context.applicationContext as Application
    val viewModel: TasksViewModel = viewModel(
        factory = ViewModelProvider.AndroidViewModelFactory.getInstance(application),
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var filter by rememberSaveable { mutableStateOf(TaskFilter.Pending) }
    val now = System.currentTimeMillis()
    val visibleTasks = when (filter) {
        TaskFilter.Pending -> state.pending.filter { it.deadlineAt == null || it.deadlineAt >= now }
        TaskFilter.Overdue -> state.pending.filter { it.deadlineAt != null && it.deadlineAt < now }
        TaskFilter.Completed -> state.completed
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Tasks", style = MaterialTheme.typography.headlineMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TaskFilter.entries.forEach { item ->
                if (filter == item) {
                    Button(onClick = { filter = item }) { Text(item.label) }
                } else {
                    OutlinedButton(onClick = { filter = item }) { Text(item.label) }
                }
            }
        }

        if (visibleTasks.isEmpty()) {
            EmptyTasks(filter)
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(visibleTasks, key = { it.id }) { task ->
                    TaskCard(
                        task = task,
                        isCompleted = filter == TaskFilter.Completed,
                        onComplete = { viewModel.complete(task.id) },
                        onDelayOneHour = { viewModel.delayOneHour(task.id) },
                        onRemindTomorrow = { viewModel.remindTomorrow(task.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyTasks(filter: TaskFilter) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = when (filter) {
                    TaskFilter.Pending -> "暂无待办"
                    TaskFilter.Overdue -> "没有逾期任务"
                    TaskFilter.Completed -> "还没有完成记录"
                },
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = "收到可识别通知后，任务会自动出现在这里。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TaskCard(
    task: TaskEntity,
    isCompleted: Boolean,
    onComplete: () -> Unit,
    onDelayOneHour: () -> Unit,
    onRemindTomorrow: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(
                    onClick = {},
                    label = { Text(SupportedApps.appLabel(task.sourcePackage)) },
                )
                AssistChip(
                    onClick = {},
                    label = { Text(task.urgency) },
                )
                Text(
                    text = "${(task.confidence * 100).toInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = task.content,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "截止：${task.deadlineAt?.let { formatDateTime(it) } ?: "未设置"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "提醒：${formatReminder(task.reminderAt)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (!isCompleted) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(onClick = onComplete) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Text("完成", modifier = Modifier.padding(start = 6.dp))
                    }
                    OutlinedButton(onClick = onDelayOneHour) {
                        Icon(Icons.Default.Schedule, contentDescription = null)
                        Text("延后1小时", modifier = Modifier.padding(start = 6.dp))
                    }
                    OutlinedButton(onClick = onRemindTomorrow) {
                        Text("明天提醒")
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "完成于 ${formatDateTime(task.updatedAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
