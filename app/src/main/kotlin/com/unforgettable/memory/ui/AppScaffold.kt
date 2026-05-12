package com.unforgettable.memory.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.unforgettable.memory.ui.debug.DebugScreen
import com.unforgettable.memory.ui.home.HomeScreen
import com.unforgettable.memory.ui.settings.SettingsScreen
import com.unforgettable.memory.ui.tasks.TasksScreen

private enum class AppTab(
    val label: String,
    val icon: ImageVector,
) {
    Home("Home", Icons.Default.Home),
    Tasks("Tasks", Icons.Default.CheckCircle),
    Debug("Debug", Icons.Default.BugReport),
    Settings("Settings", Icons.Default.Settings),
}

@Composable
fun AppScaffold(startOnTasks: Boolean = false) {
    var selectedTab by rememberSaveable {
        mutableStateOf(if (startOnTasks) AppTab.Tasks else AppTab.Home)
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                AppTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
    ) { padding ->
        val modifier = Modifier.padding(padding)
        when (selectedTab) {
            AppTab.Home -> HomeScreen(modifier)
            AppTab.Tasks -> TasksScreen(modifier)
            AppTab.Debug -> DebugScreen(modifier)
            AppTab.Settings -> SettingsScreen(modifier)
        }
    }
}
