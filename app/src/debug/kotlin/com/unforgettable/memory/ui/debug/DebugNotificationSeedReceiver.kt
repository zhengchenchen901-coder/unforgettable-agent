package com.unforgettable.memory.ui.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.unforgettable.memory.UnforgettableApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class DebugNotificationSeedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != DebugNotificationSeeder.ACTION_SEED_NOTIFICATIONS) return

        val pendingResult = goAsync()
        val app = context.applicationContext as UnforgettableApp
        val count = intent.getIntExtra(DebugNotificationSeeder.EXTRA_COUNT, DEFAULT_COUNT)
        val insertRaw = intent.getBooleanExtra(DebugNotificationSeeder.EXTRA_INSERT_RAW, true)
        val postVisible = intent.getBooleanExtra(DebugNotificationSeeder.EXTRA_POST_VISIBLE, false)

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            runCatching {
                val result = DebugNotificationSeeder(app).seed(
                    count = count,
                    insertRaw = insertRaw,
                    postVisible = postVisible,
                )
                Log.i(TAG, "Seeded notifications from adb: $result")
            }.onFailure { error ->
                Log.w(TAG, "Failed to seed notifications from adb", error)
            }
            pendingResult.finish()
        }
    }

    companion object {
        private const val TAG = "DebugNotificationSeed"
        private const val DEFAULT_COUNT = 25
    }
}
