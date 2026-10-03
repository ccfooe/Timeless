package com.classbell.app.service

/**
 * Pure domain formatter for InClassService notification and lockscreen cards.
 * Generates clean, modern progress bar with heavy white fill and thin open rail (inspired by Bilibili / modern media players),
 * completely free from coarse block characters.
 */
object InClassNotificationFormatter {

    /**
     * Generates a modern streamline progress bar.
     * Filled section uses heavy horizontal bar (━), open base rail uses thin horizontal line (─).
     * Perfectly connects horizontally without distortion across any font.
     */
    fun formatModernProgressBar(percent: Int, length: Int = 16): String {
        val clamped = percent.coerceIn(0, 100)
        val filled = (clamped * length) / 100
        val empty = (length - filled).coerceAtLeast(0)
        return "━".repeat(filled) + "─".repeat(empty)
    }

    /**
     * Formats clean, modern subText for in-class status with modern streamline progress rail.
     * Example: "第3节 29% ━━━━──────────── · 📍贡院401"
     */
    fun formatInClassSubText(periodLabel: String, progressPercent: Int, room: String, barLength: Int = 16): String {
        val bar = formatModernProgressBar(progressPercent, barLength)
        val roomPart = if (room.isNotBlank()) " · 📍$room" else ""
        val labelPart = if (periodLabel.isNotBlank()) "$periodLabel " else ""
        return "$labelPart$progressPercent% $bar$roomPart"
    }

    /**
     * Formats clean contentText for remaining class duration.
     * Example: "距下课 14 分钟 · 📍 贡院401"
     */
    fun formatInClassContentText(remainingMinutes: Int, room: String): String {
        val roomPart = if (room.isNotBlank()) " · 📍 $room" else ""
        return "距下课 $remainingMinutes 分钟$roomPart"
    }

    /**
     * Formats recess subText with modern streamline progress rail.
     * Example: "课间休息 50% ━━━━━━━━──────── · 📍贡院401"
     */
    fun formatRecessSubText(breakTitle: String, progressPercent: Int, room: String = "", barLength: Int = 16): String {
        val bar = formatModernProgressBar(progressPercent, barLength)
        val roomPart = if (room.isNotBlank()) " · 📍$room" else ""
        return "$breakTitle $progressPercent% $bar$roomPart"
    }

    /**
     * Formats recess contentText.
     * Example: "下一节: 第4节 · 距上课 8 分钟 · 📍 贡院401"
     */
    fun formatRecessContentText(nextPeriod: String, remainingMinutes: Int, room: String): String {
        val nextPart = if (nextPeriod.isNotBlank()) "下一节: $nextPeriod · " else ""
        val roomPart = if (room.isNotBlank()) " · 📍 $room" else ""
        return "${nextPart}距上课 $remainingMinutes 分钟$roomPart"
    }
}
