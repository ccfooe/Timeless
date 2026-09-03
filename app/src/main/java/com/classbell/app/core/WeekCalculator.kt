package com.classbell.app.core

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Week calculation and semester phase identification according to PRD FR-1.2 and §10.1.
 */
object WeekCalculator {

    /**
     * Calculates the semester week number for [targetDate] based on [week1Monday].
     * Formula: floor((targetDate - week1Monday) / 7) + 1, clamped to >= 1.
     */
    fun calculateWeekNumber(targetDate: LocalDate, week1Monday: LocalDate): Int {
        val daysDiff = ChronoUnit.DAYS.between(week1Monday, targetDate)
        val week = (daysDiff / 7).toInt() + 1
        return if (daysDiff < 0) 1 else week
    }

    /**
     * Determines the phase label for a given week number.
     */
    fun getSemesterPhase(
        weekNumber: Int,
        teachingFrom: Int = 2,
        teachingTo: Int = 18,
        examFrom: Int? = 19,
        examTo: Int? = 20,
        practiceFrom: Int? = 21,
        practiceTo: Int? = 21
    ): String {
        return when {
            weekNumber < teachingFrom -> "报到周"
            weekNumber in teachingFrom..teachingTo -> "教学周"
            examFrom != null && examTo != null && weekNumber in examFrom..examTo -> "考试周"
            practiceFrom != null && practiceTo != null && weekNumber in practiceFrom..practiceTo -> "实践周"
            else -> "假期"
        }
    }

    /**
     * Returns the date range (Monday to Sunday) for a given week number in the semester.
     */
    fun getWeekDateRange(weekNumber: Int, week1Monday: LocalDate): Pair<LocalDate, LocalDate> {
        val monday = week1Monday.plusWeeks((weekNumber - 1).toLong())
        val sunday = monday.plusDays(6)
        return Pair(monday, sunday)
    }
}
