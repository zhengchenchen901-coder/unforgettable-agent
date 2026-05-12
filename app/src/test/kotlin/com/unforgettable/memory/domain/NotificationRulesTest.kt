package com.unforgettable.memory.domain

import com.unforgettable.memory.domain.notification.NotificationRules
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationRulesTest {
    @Test
    fun ignoresVerificationCodesAndMarketing() {
        assertTrue(NotificationRules.shouldIgnore("短信", "您的验证码是 123456"))
        assertTrue(NotificationRules.shouldIgnore("Shop", "限时优惠，今日促销"))
    }

    @Test
    fun keepsActionableMessages() {
        assertFalse(NotificationRules.shouldIgnore("老板", "明天开会前把PPT发我"))
        assertTrue(NotificationRules.hasActionVerb("明天开会前把PPT发我"))
        assertTrue(NotificationRules.hasDeadlineSignal("明天开会前把PPT发我"))
    }
}

