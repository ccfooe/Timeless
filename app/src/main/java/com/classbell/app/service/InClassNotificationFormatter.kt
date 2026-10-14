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
     * Formats clean, modern subText for in-class status.
     * Example: "第3节 · 进度 29%"
     */
    fun formatInClassSubText(periodLabel: String, progressPercent: Int, room: String = ""): String {
        val labelPart = if (periodLabel.isNotBlank()) "$periodLabel · " else ""
        return "${labelPart}进度 $progressPercent%"
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
     * Formats recess subText without character bars.
     * Example: "课间休息 · 进度 50%"
     */
    fun formatRecessSubText(breakTitle: String, progressPercent: Int, room: String = ""): String {
        val titlePart = if (breakTitle.isNotBlank()) "$breakTitle · " else ""
        return "${titlePart}进度 $progressPercent%"
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

    /**
     * Bauhaus expanded bigText layout for In-Class:
     * Clean single-line layout without character progress bars (relies on native blue system ProgressBar).
     * Example:
     * "距下课 14 分钟 · 预计 09:15 下课"
     */
    fun formatBauhausInClassBigText(
        remainingMinutes: Int,
        endTimeStr: String
    ): String {
        return "距下课 $remainingMinutes 分钟 · 预计 $endTimeStr 下课"
    }

    /**
     * Bauhaus expanded bigText layout for Recess:
     * Clean single-line layout without character progress bars.
     * Example:
     * "下一节: 第4节 · 距上课 8 分钟 · 预计 09:25 上课"
     */
    fun formatBauhausRecessBigText(
        remainingMinutes: Int,
        nextStartTimeStr: String,
        nextPeriod: String = ""
    ): String {
        val nextPart = if (nextPeriod.isNotBlank()) "下一节: $nextPeriod · " else ""
        return "${nextPart}距上课 $remainingMinutes 分钟 · 预计 $nextStartTimeStr 上课"
    }

    /**
     * Lock screen subText: incorporates compact streamline progress bar for glanceable progress
     * when device is locked (since Android lock screens hide native system ProgressBars).
     * Example: "第7节 40% ━━━━──────"
     */
    fun formatLockscreenSubText(label: String, progressPercent: Int, barLength: Int = 10): String {
        val bar = formatModernProgressBar(progressPercent, barLength)
        val labelPart = if (label.isNotBlank()) "$label " else ""
        return "$labelPart$progressPercent% $bar"
    }

    /**
     * Lock screen expanded bigText layout for In-Class / Manual Session:
     * Compact timeline rail between start clock and end clock, followed by remaining time and percent.
     * With barLength = 10 (22 chars total line), it NEVER line-wraps across any device or font size.
     * Example:
     * "16:20 ━━━━────── 17:05\n距下课 27 分钟 · 进度 40%"
     */
    fun formatLockscreenInClassBigText(
        startTimeStr: String,
        endTimeStr: String,
        remainingMinutes: Int,
        progressPercent: Int,
        barLength: Int = 10
    ): String {
        val bar = formatModernProgressBar(progressPercent, barLength)
        return "$startTimeStr $bar $endTimeStr\n距下课 $remainingMinutes 分钟 · 进度 $progressPercent%"
    }

    /**
     * Lock screen expanded bigText layout for Recess:
     * Compact timeline rail between recess start and next period start.
     * Example:
     * "09:15 ━━━━━───── 09:25\n下一节: 第4节 · 距上课 8 分钟 · 进度 50%"
     */
    fun formatLockscreenRecessBigText(
        startTimeStr: String,
        endTimeStr: String,
        nextPeriod: String,
        remainingMinutes: Int,
        progressPercent: Int,
        barLength: Int = 10
    ): String {
        val bar = formatModernProgressBar(progressPercent, barLength)
        val nextPart = if (nextPeriod.isNotBlank()) "下一节: $nextPeriod · " else ""
        return "$startTimeStr $bar $endTimeStr\n${nextPart}距上课 $remainingMinutes 分钟 · 进度 $progressPercent%"
    }
}
