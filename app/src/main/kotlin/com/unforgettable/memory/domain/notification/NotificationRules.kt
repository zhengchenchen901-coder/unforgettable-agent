package com.unforgettable.memory.domain.notification

object NotificationRules {
    val ignoreKeywords = listOf(
        "验证码",
        "校验码",
        "verification code",
        "code is",
        "广告",
        "优惠",
        "促销",
        "红包",
        "限时",
        "订阅",
        "营销",
        "newsletter",
        "unsubscribe",
        "快递已送达",
        "群聊",
    )

    val actionVerbs = listOf(
        "发送",
        "发我",
        "提交",
        "回复",
        "处理",
        "付款",
        "预约",
        "确认",
        "整理",
        "更新",
        "完成",
        "开会前",
        "别忘",
        "记得",
        "提醒",
        "send",
        "submit",
        "reply",
        "pay",
        "book",
        "schedule",
        "confirm",
        "finish",
        "before",
        "by ",
    )

    fun shouldIgnore(title: String, content: String): Boolean {
        val text = "$title\n$content".lowercase()
        return ignoreKeywords.any { text.contains(it.lowercase()) }
    }

    fun hasActionVerb(text: String): Boolean {
        val lower = text.lowercase()
        return actionVerbs.any { lower.contains(it.lowercase()) }
    }

    fun hasDeadlineSignal(text: String): Boolean {
        val lower = text.lowercase()
        return listOf(
            "今天",
            "明天",
            "后天",
            "下午",
            "上午",
            "今晚",
            "截止",
            "之前",
            "前",
            "asap",
            "urgent",
            "today",
            "tomorrow",
            "tonight",
            "deadline",
            "before",
            "by ",
        ).any { lower.contains(it) }
    }

    fun urgencyFor(text: String): String {
        val lower = text.lowercase()
        return when {
            listOf("紧急", "马上", "立刻", "尽快", "asap", "urgent", "今天", "今晚").any { lower.contains(it) } -> "high"
            listOf("明天", "截止", "之前", "前", "tomorrow", "deadline", "before").any { lower.contains(it) } -> "medium"
            else -> "low"
        }
    }
}

