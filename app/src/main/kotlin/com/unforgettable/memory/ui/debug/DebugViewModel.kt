package com.unforgettable.memory.ui.debug

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.unforgettable.memory.UnforgettableApp
import com.unforgettable.memory.data.storage.entity.RawNotificationEntity
import com.unforgettable.memory.service.ExtractionWorker
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DebugViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as UnforgettableApp

    val rawNotifications = app.container.database.rawNotificationDao()
        .observeRecent()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val extractionLogs = app.container.database.aiExtractionLogDao()
        .observeRecent()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun injectDemoNotification() {
        viewModelScope.launch {
            val rawId = app.container.database.rawNotificationDao().insert(
                RawNotificationEntity(
                    packageName = "com.tencent.mm",
                    title = "老板",
                    content = "明天开会前把PPT发我",
                    timestamp = System.currentTimeMillis(),
                ),
            )
            ExtractionWorker.enqueue(app, rawId)
        }
    }
}

