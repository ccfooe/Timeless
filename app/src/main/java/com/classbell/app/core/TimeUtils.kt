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
}
