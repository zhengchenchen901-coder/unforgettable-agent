package com.unforgettable.memory.domain.notification

object SupportedApps {
    private val labels = mapOf(
        "com.tencent.mm" to "微信",
        "com.ss.android.lark" to "飞书",
        "com.alibaba.android.rimet" to "钉钉",
        "com.google.android.gm" to "Gmail",
        "com.google.android.apps.messaging" to "短信",
    )

    fun shouldRunAi(packageName: String): Boolean = labels.containsKey(packageName)

    fun appLabel(packageName: String): String = labels[packageName] ?: packageName
}

