package com.classbell.app.core

import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object TimeUtils {

    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    fun formatTime(time: LocalTime): String = time.format(timeFormatter)

    fun formatDateWithWeekday(date: LocalDate): String {
        val weekdayName = when (date.dayOfWeek.value) {
            1 -> "周一"
            2 -> "周二"
            3 -> "周三"
            4 -> "周四"
            5 -> "周五"
            6 -> "周六"
            7 -> "周日"
            else -> ""
        }
        return "${date.monthValue}月${date.dayOfMonth}日 $weekdayName"
    }

    fun formatHeaderDate(date: LocalDate): String {
        val weekdayFullName = when (date.dayOfWeek.value) {
            1 -> "星期一"
            2 -> "星期二"
            3 -> "星期三"
            4 -> "星期四"
            5 -> "星期五"
            6 -> "星期六"
            7 -> "星期日"
            else -> ""
        }
        return "${date.monthValue}月${date.dayOfMonth}日 $weekdayFullName"
    }

    fun getWeekdayShortName(dayOfWeek: Int): String {
        return when (dayOfWeek) {
            1 -> "周一"
            2 -> "周二"
            3 -> "周三"
            4 -> "周四"
            5 -> "周五"
            6 -> "周六"
            7 -> "周日"
            else -> ""
        }
    }

    fun getWeekdayNameChinese(dayOfWeek: Int): String {
        return when (dayOfWeek) {
            1 -> "一"
            2 -> "二"
            3 -> "三"
            4 -> "四"
            5 -> "五"
            6 -> "六"
            7 -> "日"
            else -> ""
        }
    }

    val BASE_ZONE: java.time.ZoneId = java.time.ZoneId.of("Asia/Shanghai")

    fun isDifferentFromBaseZone(instant: java.time.Instant = java.time.Instant.now()): Boolean {
        val currentZone = java.time.ZoneId.systemDefault()
        return currentZone.rules.getOffset(instant) != BASE_ZONE.rules.getOffset(instant)
    }

    fun getCurrentZoneDisplay(): String {
        val currentZone = java.time.ZoneId.systemDefault()
        val offset = currentZone.rules.getOffset(java.time.Instant.now())
        return "${currentZone.id} ($offset)"
    }

    data class ConvertedAffairDateTime(
        val effectiveDate: LocalDate,
        val effectiveTime: LocalTime?,
        val isConverted: Boolean,
        val originalTimeStr: String?
    )

    fun convertAffairDateTime(
        date: LocalDate,
        time: LocalTime?,
        autoConvert: Boolean,
        targetZone: java.time.ZoneId = java.time.ZoneId.systemDefault()
    ): ConvertedAffairDateTime {
        if (!autoConvert || time == null) {
            return ConvertedAffairDateTime(date, time, isConverted = false, originalTimeStr = null)
        }
        val beijingZdt = java.time.ZonedDateTime.of(date, time, BASE_ZONE)
        val beijingOffset = BASE_ZONE.rules.getOffset(beijingZdt.toInstant())
        val targetOffset = targetZone.rules.getOffset(beijingZdt.toInstant())
        if (beijingOffset == targetOffset) {
            return ConvertedAffairDateTime(date, time, isConverted = false, originalTimeStr = null)
        }

        val localZdt = beijingZdt.withZoneSameInstant(targetZone)
        return ConvertedAffairDateTime(
            effectiveDate = localZdt.toLocalDate(),
            effectiveTime = localZdt.toLocalTime(),
            isConverted = true,
            originalTimeStr = formatTime(time)
        )
    }

    /**
     * 将指定时区的时间换算为目标时区时间
     */
    fun convertBetweenZones(
        date: LocalDate,
        time: LocalTime,
        fromZone: java.time.ZoneId,
        toZone: java.time.ZoneId = java.time.ZoneId.systemDefault()
    ): Pair<LocalDate, LocalTime> {
        val srcZdt = java.time.ZonedDateTime.of(date, time, fromZone)
        val targetZdt = srcZdt.withZoneSameInstant(toZone)
        return Pair(targetZdt.toLocalDate(), targetZdt.toLocalTime())
    }

    /**
     * 解析常见时区别名或城市名称为标准 ZoneId
     */
    fun resolveZoneByName(name: String): java.time.ZoneId? {
        val clean = name.trim().lowercase()
        return when {
            clean.contains("london") || clean == "gmt" || clean == "bst" -> java.time.ZoneId.of("Europe/London")
            clean.contains("shanghai") || clean.contains("beijing") || clean == "cst" -> java.time.ZoneId.of("Asia/Shanghai")
            clean.contains("tokyo") || clean == "jst" -> java.time.ZoneId.of("Asia/Tokyo")
            clean.contains("new york") || clean == "est" || clean == "edt" -> java.time.ZoneId.of("America/New_York")
            clean.contains("los angeles") || clean == "pst" || clean == "pdt" -> java.time.ZoneId.of("America/Los_Angeles")
            clean == "utc" -> java.time.ZoneId.of("UTC")
            else -> runCatching { java.time.ZoneId.of(name.trim()) }.getOrNull()
        }
    }

    /**
     * 根据具体目标日期的 Instant 确定性计算时区所处的季节（冬令时/夏令时），避免大模型心算误差
     */
    fun getZoneSeasonName(
        zone: java.time.ZoneId,
        date: LocalDate,
        time: LocalTime = LocalTime.NOON
    ): String {
        val zdt = java.time.ZonedDateTime.of(date, time, zone)
        val isDaylight = zone.rules.isDaylightSavings(zdt.toInstant())
        val offset = zone.rules.getOffset(zdt.toInstant())
        return when {
            zone.id == "Europe/London" -> if (isDaylight) "BST夏令时(UTC+1)" else "GMT冬令时(UTC+0)"
            zone.id == "America/New_York" -> if (isDaylight) "EDT夏令时($offset)" else "EST冬令时($offset)"
            zone.id == "America/Los_Angeles" -> if (isDaylight) "PDT夏令时($offset)" else "PST冬令时($offset)"
            zone.id == "Europe/Paris" || zone.id == "Europe/Berlin" -> if (isDaylight) "CEST夏令时($offset)" else "CET冬令时($offset)"
            else -> if (isDaylight) "${zone.id}夏令时($offset)" else "${zone.id}标准时($offset)"
        }
    }
}


