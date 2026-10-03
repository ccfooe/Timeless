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

        val inClassSub = InClassNotificationFormatter.formatInClassSubText("第3节", 29, "贡院401")
        assertEquals("第3节 29% ━━━━──────────── · 📍贡院401", inClassSub)

        val recessSub = InClassNotificationFormatter.formatRecessSubText("课间休息", 50, "贡院401")
        assertEquals("课间休息 50% ━━━━━━━━──────── · 📍贡院401", recessSub)
    }
}
