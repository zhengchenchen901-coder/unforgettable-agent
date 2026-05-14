package com.unforgettable.memory.ui.debug

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.unforgettable.memory.MainActivity
import com.unforgettable.memory.R
import com.unforgettable.memory.UnforgettableApp
import com.unforgettable.memory.data.storage.entity.RawNotificationEntity
import com.unforgettable.memory.domain.notification.NotificationRules
import com.unforgettable.memory.domain.notification.SupportedApps
import com.unforgettable.memory.reminder.NotificationChannels
import com.unforgettable.memory.service.ExtractionWorker

data class DebugNotificationSeedResult(
    val rawInserted: Int,
    val extractionEnqueued: Int,
    val visiblePosted: Int,
    val visibleSkippedReason: String? = null,
)

class DebugNotificationSeeder(
    private val app: UnforgettableApp,
) {
    suspend fun seed(
        count: Int,
        insertRaw: Boolean,
        postVisible: Boolean,
    ): DebugNotificationSeedResult {
        val safeCount = count.coerceIn(MIN_COUNT, MAX_COUNT)
        val samples = DebugNotificationSamples.createBatch(safeCount)
        var rawInserted = 0
        var extractionEnqueued = 0
        var visiblePosted = 0
        var visibleSkippedReason: String? = null

        if (insertRaw) {
            val rawNotificationDao = app.container.database.rawNotificationDao()
            samples.forEach { sample ->
                val rawId = rawNotificationDao.insert(
                    RawNotificationEntity(
                        packageName = sample.packageName,
                        title = sample.title,
                        content = sample.content,
                        timestamp = sample.timestamp,
                    ),
                )
                rawInserted += 1

                val shouldExtract = SupportedApps.shouldRunAi(sample.packageName) &&
                    !NotificationRules.shouldIgnore(sample.title, sample.content)
                if (shouldExtract) {
                    ExtractionWorker.enqueue(app, rawId)
                    extractionEnqueued += 1
                }
            }
        }

        if (postVisible) {
            if (canPostNotifications()) {
                visiblePosted = postVisibleNotifications(samples)
            } else {
                visibleSkippedReason = "missing POST_NOTIFICATIONS permission"
            }
        }

        return DebugNotificationSeedResult(
            rawInserted = rawInserted,
            extractionEnqueued = extractionEnqueued,
            visiblePosted = visiblePosted,
            visibleSkippedReason = visibleSkippedReason,
        )
    }

    private fun canPostNotifications(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                app,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    private fun postVisibleNotifications(samples: List<DebugNotificationSample>): Int {
        val notificationManager = NotificationManagerCompat.from(app)
        val runId = ((System.currentTimeMillis() / 1_000) % 1_000).toInt()
        val pendingIntent = PendingIntent.getActivity(
            app,
            DEBUG_PENDING_INTENT_REQUEST_CODE,
            Intent(app, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        samples.forEachIndexed { index, sample ->
            val notification = NotificationCompat.Builder(app, NotificationChannels.DEBUG_SEED_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("${sample.appLabel} · ${sample.title}")
                .setContentText(sample.content)
                .setStyle(NotificationCompat.BigTextStyle().bigText(sample.content))
                .setWhen(sample.timestamp)
                .setShowWhen(true)
                .setContentIntent(pendingIntent)
                .setAutoCancel(false)
                .setGroup(DEBUG_NOTIFICATION_GROUP_KEY)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build()

            notificationManager.notify(notificationId(runId, index), notification)
        }

        val summary = NotificationCompat.Builder(app, NotificationChannels.DEBUG_SEED_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Debug notification seed")
            .setContentText("Generated ${samples.size} realistic notification samples")
            .setStyle(
                NotificationCompat.InboxStyle().also { style ->
                    samples.take(SUMMARY_LINE_LIMIT).forEach { sample ->
                        style.addLine("${sample.appLabel} · ${sample.title}: ${sample.content}")
                    }
                },
            )
            .setContentIntent(pendingIntent)
            .setAutoCancel(false)
            .setGroup(DEBUG_NOTIFICATION_GROUP_KEY)
            .setGroupSummary(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        notificationManager.notify(notificationId(runId, SUMMARY_NOTIFICATION_INDEX), summary)
        return samples.size
    }

    private fun notificationId(runId: Int, index: Int): Int {
        return DEBUG_NOTIFICATION_ID_BASE + runId * NOTIFICATION_ID_SLOT_SIZE + index
    }

    companion object {
        const val ACTION_SEED_NOTIFICATIONS = "com.unforgettable.memory.DEBUG_SEED_NOTIFICATIONS"
        const val EXTRA_COUNT = "count"
        const val EXTRA_INSERT_RAW = "raw"
        const val EXTRA_POST_VISIBLE = "visible"

        private const val MIN_COUNT = 1
        private const val MAX_COUNT = 200
        private const val NOTIFICATION_ID_SLOT_SIZE = MAX_COUNT + 1
        private const val DEBUG_NOTIFICATION_ID_BASE = 400_000
        private const val DEBUG_PENDING_INTENT_REQUEST_CODE = 9301
        private const val DEBUG_NOTIFICATION_GROUP_KEY = "debug_realistic_notifications"
        private const val SUMMARY_NOTIFICATION_INDEX = MAX_COUNT
        private const val SUMMARY_LINE_LIMIT = 6
    }
}

data class DebugNotificationSample(
    val packageName: String,
    val appLabel: String,
    val title: String,
    val content: String,
    val timestamp: Long,
)

private object DebugNotificationSamples {
    private val projects = listOf("星河版本", "记忆检索", "发布清单", "客户演示", "周会材料", "预算表")
    private val documents = listOf("PPT", "报价单", "会议纪要", "需求文档", "测试报告", "合同草稿")
    private val customers = listOf("远山科技", "北岸零售", "青禾教育", "Atlas Labs", "River Studio")
    private val people = listOf("陈晨", "Alex", "王老师", "Lily", "赵工", "Maya")

    private val templates = listOf(
        Template("com.tencent.mm", "微信", "老板", "明天开会前把{document}发我"),
        Template("com.tencent.mm", "微信", "{person}", "今晚 8 点前确认一下{project}的排期"),
        Template("com.tencent.mm", "微信", "家人", "下班前记得去药店买药，别忘了"),
        Template("com.tencent.mm", "微信", "产品群", "群聊：今天的站会纪要同步一下"),
        Template("com.ss.android.lark", "飞书", "{person}", "请在今天下班前更新{project}任务状态"),
        Template("com.ss.android.lark", "飞书", "项目助手", "{project} PR 需要你今天确认"),
        Template("com.ss.android.lark", "飞书", "日历", "15:00 {project} 评审会即将开始"),
        Template("com.alibaba.android.rimet", "钉钉", "钉钉待办", "明天 10 点前提交{project}周报"),
        Template("com.alibaba.android.rimet", "钉钉", "{customer}", "客户合同请尽快处理，今晚前给我反馈"),
        Template("com.alibaba.android.rimet", "钉钉", "审批助手", "{document}审批已通过"),
        Template("com.google.android.gm", "Gmail", "{person} via Gmail", "Please send the revised {document} by tomorrow afternoon"),
        Template("com.google.android.gm", "Gmail", "{customer}", "Can you confirm the {project} timeline before Friday?"),
        Template("com.google.android.apps.messaging", "短信", "短信", "您的验证码是 {code}，5分钟内有效"),
        Template("com.google.android.apps.messaging", "短信", "{person}", "明天下午帮我预约一下会议室"),
        Template("com.shop.demo", "电商", "Shop", "限时优惠，今日促销满 300 减 50"),
    )

    fun createBatch(count: Int): List<DebugNotificationSample> {
        val now = System.currentTimeMillis()
        return List(count) { index ->
            val template = templates[index % templates.size]
            val cycle = index / templates.size
            val timestamp = now - index * intervalMillis(index)
            DebugNotificationSample(
                packageName = template.packageName,
                appLabel = template.appLabel,
                title = fill(template.title, index, cycle),
                content = fill(template.content, index, cycle),
                timestamp = timestamp,
            )
        }
    }

    private fun fill(pattern: String, index: Int, cycle: Int): String {
        return pattern
            .replace("{project}", projects[(index + cycle) % projects.size])
            .replace("{document}", documents[(index + cycle * 2) % documents.size])
            .replace("{customer}", customers[(index + cycle) % customers.size])
            .replace("{person}", people[(index + cycle) % people.size])
            .replace("{code}", (100_000 + (index * 7_919) % 900_000).toString())
    }

    private fun intervalMillis(index: Int): Long {
        val minutes = 2L + (index % 9) * 3L
        return minutes * 60_000L
    }

    private data class Template(
        val packageName: String,
        val appLabel: String,
        val title: String,
        val content: String,
    )
}
