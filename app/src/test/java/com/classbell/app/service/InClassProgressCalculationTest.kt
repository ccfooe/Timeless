package com.classbell.app.service

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.LocalTime
import java.time.temporal.ChronoUnit

class InClassProgressCalculationTest {

    @Test
    fun testManualSessionProgressWhenJustStarted() {
        val startTime = LocalTime.of(10, 39)
        val endTime = startTime.plusMinutes(45) // 11:24
        val now = LocalTime.of(10, 39) // Just started!

        val totalSecs = Duration.between(startTime, endTime).seconds.coerceAtLeast(1)
        val elapsedSecs = Duration.between(startTime, now).seconds.coerceIn(0, totalSecs)
        val progressRatio = (elapsedSecs.toFloat() / totalSecs.toFloat()).coerceIn(0f, 1f)
        val progressPercent = ((elapsedSecs * 100) / totalSecs).toInt().coerceIn(0, 100)
        val remainingMinutes = ChronoUnit.MINUTES.between(now, endTime).coerceAtLeast(0).toInt()

        // When just started, progress MUST be 0%, NOT 33%!
        assertEquals(0f, progressRatio, 0.001f)
        assertEquals(0, progressPercent)
        assertEquals(45, remainingMinutes)
    }

    @Test
    fun testManualSessionProgressMidway() {
        val startTime = LocalTime.of(10, 39)
        val endTime = startTime.plusMinutes(45) // 11:24
        val now = LocalTime.of(11, 1, 30) // 22.5 minutes elapsed

        val totalSecs = Duration.between(startTime, endTime).seconds.coerceAtLeast(1)
        val elapsedSecs = Duration.between(startTime, now).seconds.coerceIn(0, totalSecs)
        val progressRatio = (elapsedSecs.toFloat() / totalSecs.toFloat()).coerceIn(0f, 1f)
        val progressPercent = ((elapsedSecs * 100) / totalSecs).toInt().coerceIn(0, 100)
        val remainingMinutes = ChronoUnit.MINUTES.between(now, endTime).coerceAtLeast(0).toInt()

        assertEquals(0.5f, progressRatio, 0.01f)
        assertEquals(50, progressPercent)
        assertEquals(22, remainingMinutes)
    }

    @Test
    fun testManualSessionProgressAtEnd() {
        val startTime = LocalTime.of(10, 39)
        val endTime = startTime.plusMinutes(45) // 11:24
        val now = LocalTime.of(11, 24)

        val totalSecs = Duration.between(startTime, endTime).seconds.coerceAtLeast(1)
        val elapsedSecs = Duration.between(startTime, now).seconds.coerceIn(0, totalSecs)
        val progressRatio = (elapsedSecs.toFloat() / totalSecs.toFloat()).coerceIn(0f, 1f)
        val progressPercent = ((elapsedSecs * 100) / totalSecs).toInt().coerceIn(0, 100)
        val remainingMinutes = ChronoUnit.MINUTES.between(now, endTime).coerceAtLeast(0).toInt()

        assertEquals(1.0f, progressRatio, 0.001f)
        assertEquals(100, progressPercent)
        assertEquals(0, remainingMinutes)
    }

    @Test
    fun testModernStreamlineProgressBarFormatting() {
        val bar0 = InClassNotificationFormatter.formatModernProgressBar(0, length = 16)
        assertEquals("────────────────", bar0)

        val bar100 = InClassNotificationFormatter.formatModernProgressBar(100, length = 16)
        assertEquals("━━━━━━━━━━━━━━━━", bar100)

        val bar29 = InClassNotificationFormatter.formatModernProgressBar(29, length = 16)
        assertEquals("━━━━────────────", bar29)
        org.junit.Assert.assertFalse(bar29.contains("▰") || bar29.contains("▱"))

        val inClassSub = InClassNotificationFormatter.formatInClassSubText("第3节", 29)
        assertEquals("第3节 · 进度 29%", inClassSub)

        val recessSub = InClassNotificationFormatter.formatRecessSubText("课间休息", 50)
        assertEquals("课间休息 · 进度 50%", recessSub)
    }

    @Test
    fun testBauhausUltraMinimalistBigTextFormatting() {
        // Complete minimalism: exactly one line, e.g. "距下课 14 分钟 · 预计 09:15 下课"
        val inClassBig = InClassNotificationFormatter.formatBauhausInClassBigText(
            remainingMinutes = 14,
            endTimeStr = "09:15"
        )
        assertEquals("距下课 14 分钟 · 预计 09:15 下课", inClassBig)

        val recessBig = InClassNotificationFormatter.formatBauhausRecessBigText(
            remainingMinutes = 8,
            nextStartTimeStr = "09:25",
            nextPeriod = "第2节"
        )
        assertEquals("下一节: 第2节 · 距上课 8 分钟 · 预计 09:25 上课", recessBig)
    }

    @Test
    fun testLockscreenStreamlineProgressBarFormatting() {
        val lockSub = InClassNotificationFormatter.formatLockscreenSubText("第7节", 40, barLength = 10)
        assertEquals("第7节 40% ━━━━──────", lockSub)

        val lockInClassBig = InClassNotificationFormatter.formatLockscreenInClassBigText(
            startTimeStr = "16:20",
            endTimeStr = "17:05",
            remainingMinutes = 27,
            progressPercent = 40,
            barLength = 10
        )
        assertEquals("16:20 ━━━━────── 17:05\n距下课 27 分钟 · 进度 40%", lockInClassBig)

        val lockRecessBig = InClassNotificationFormatter.formatLockscreenRecessBigText(
            startTimeStr = "09:15",
            endTimeStr = "09:25",
            nextPeriod = "第4节",
            remainingMinutes = 8,
            progressPercent = 50,
            barLength = 10
        )
        assertEquals("09:15 ━━━━━───── 09:25\n下一节: 第4节 · 距上课 8 分钟 · 进度 50%", lockRecessBig)
    }

    @Test
    fun testMultiOemDynamicIslandContractCoverage() {
        val extras = MultiOemDynamicIslandContract.buildUniversalExtrasMap(
            capsuleTitle = "第7节",
            whenMillis = 1728920000000L,
            progressPercent = 40,
            appName = "Timeless"
        )
        // 1. Android 15+ Native Promoted Ongoing
        assertEquals(true, extras[MultiOemDynamicIslandContract.KEY_PROMOTED_ONGOING])

        // 2. OPPO / OnePlus ColorOS 14/15/16 Fluid Cloud
        assertEquals(true, extras[MultiOemDynamicIslandContract.KEY_COLOROS_FLUID_CLOUD])
        assertEquals(true, extras[MultiOemDynamicIslandContract.KEY_OPLUS_FLUID_CLOUD])
        assertEquals(1, extras[MultiOemDynamicIslandContract.KEY_OPLUS_NOTIFICATION_TYPE])
        assertEquals(0, extras[MultiOemDynamicIslandContract.KEY_OPLUS_VIEW_TYPE])
        assertEquals(40, extras[MultiOemDynamicIslandContract.KEY_COLOROS_PROGRESS])

        // 3. Xiaomi HyperOS Focus Notification
        assertEquals(true, extras[MultiOemDynamicIslandContract.KEY_MIUI_FOCUS_NOTIFICATION])
        assertEquals(1, extras[MultiOemDynamicIslandContract.KEY_MIUI_FOCUS_NOTIFICATION_TYPE])
        assertEquals("第7节", extras[MultiOemDynamicIslandContract.KEY_MIUI_FOCUS_TITLE])

        // 4. vivo OriginOS Atomic Island
        assertEquals(true, extras[MultiOemDynamicIslandContract.KEY_VIVO_ATOMIC_ISLAND])
        assertEquals("第7节", extras[MultiOemDynamicIslandContract.KEY_VIVO_SUMMARY])

        // 5. Huawei HarmonyOS Live View
        assertEquals(true, extras[MultiOemDynamicIslandContract.KEY_HW_LIVE_VIEW])
        assertEquals(1, extras[MultiOemDynamicIslandContract.KEY_HW_LIVE_VIEW_TYPE])
        assertEquals("第7节", extras[MultiOemDynamicIslandContract.KEY_HW_LIVE_VIEW_TITLE])
    }
}
