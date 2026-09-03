package com.classbell.app.core

import com.classbell.app.data.model.Course
import com.classbell.app.data.model.Holiday
import com.classbell.app.data.model.HolidayType
import com.classbell.app.data.model.MeetingOccurrence
import com.classbell.app.data.model.MeetingStatus
import com.classbell.app.data.model.Override
import com.classbell.app.data.model.OverrideKind
import com.classbell.app.data.model.PeriodSlot
import com.classbell.app.data.model.Semester
import com.classbell.app.data.model.WeekPattern
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit

/**
 * Pure function engine for expanding courses into meeting occurrences for any date,
 * handling week ranges, odd/even patterns, holiday suppression, and schedule shifts.
 */
object OccurrenceEngine {

    /**
     * Expands and returns meetings for [date].
     */
    fun meetingsOn(
        date: LocalDate,
        semester: Semester,
        courses: List<Course>,
        periodSlots: List<PeriodSlot>,
        holidays: List<Holiday> = emptyList(),
        overrides: List<Override> = emptyList(),
        currentTime: LocalTime = LocalTime.now(),
        todayDate: LocalDate = LocalDate.now()
    ): List<MeetingOccurrence> {
        val weekNumber = WeekCalculator.calculateWeekNumber(date, semester.week1Monday)
        if (weekNumber < 1) return emptyList()

        // 1. Check for manual override first (highest priority)
        val override = overrides.firstOrNull { it.date == date }

        // 2. Check for holiday
        val holiday = holidays.firstOrNull { !date.isBefore(it.startDate) && !date.isAfter(it.endDate) }

        var isHolidaySuppressed = false
        var holidayName: String? = null
        var effectiveDayOfWeek = date.dayOfWeek.value // 1 (Mon) .. 7 (Sun)

        if (override != null) {
            when (override.kind) {
                OverrideKind.CANCEL -> {
                    isHolidaySuppressed = true
                    holidayName = "调休停课"
                }
                OverrideKind.SHIFT -> {
                    override.followsWeekday?.let { effectiveDayOfWeek = it }
                }
            }
        } else if (holiday != null) {
            holidayName = holiday.name
            when (holiday.type) {
                HolidayType.REST -> {
                    isHolidaySuppressed = true
                }
                HolidayType.MAKEUP -> {
                    holiday.followsWeekday?.let { effectiveDayOfWeek = it }
                }
            }
        }

        // 3. Find matching courses for the effective day and week
        val matchingCourses = courses.filter { course ->
            course.dayOfWeek == effectiveDayOfWeek &&
                    weekNumber in course.weekFrom..course.weekTo &&
                    matchesPattern(weekNumber, course.weekPattern) &&
                    weekNumber !in course.weekExclusions
        }.sortedBy { it.periodStart }

        // 4. Map to PeriodSlot clock times
        val periodSlotMap = periodSlots.associateBy { it.order }

        return matchingCourses.map { course ->
            val startSlot = periodSlotMap[course.periodStart]
            val endSlot = periodSlotMap[course.periodEnd]

            val startTime = startSlot?.startTime ?: LocalTime.of(8, 30)
            val endTime = endSlot?.endTime ?: LocalTime.of(12, 0)

            val status = determineStatus(
                targetDate = date,
                todayDate = todayDate,
                currentTime = currentTime,
                startTime = startTime,
                endTime = endTime,
                isHoliday = isHolidaySuppressed
            )

            val progress = calculateProgress(
                targetDate = date,
                todayDate = todayDate,
                currentTime = currentTime,
                startTime = startTime,
                endTime = endTime
            )

            val remainingMinutes = if (date == todayDate && currentTime.isAfter(startTime) && currentTime.isBefore(endTime)) {
                ChronoUnit.MINUTES.between(currentTime, endTime).coerceAtLeast(0)
            } else {
                0L
            }

            MeetingOccurrence(
                course = course,
                date = date,
                periodStart = course.periodStart,
                periodEnd = course.periodEnd,
                startTime = startTime,
                endTime = endTime,
                isHolidaySuppressed = isHolidaySuppressed,
                holidayName = holidayName,
                status = status,
                progress = progress,
                remainingMinutes = remainingMinutes
            )
        }
    }

    fun matchesPattern(weekNumber: Int, pattern: WeekPattern): Boolean {
        return when (pattern) {
            WeekPattern.ALL -> true
            WeekPattern.ODD -> weekNumber % 2 != 0
            WeekPattern.EVEN -> weekNumber % 2 == 0
        }
    }

    private fun determineStatus(
        targetDate: LocalDate,
        todayDate: LocalDate,
        currentTime: LocalTime,
        startTime: LocalTime,
        endTime: LocalTime,
        isHoliday: Boolean
    ): MeetingStatus {
        if (isHoliday) return MeetingStatus.HOLIDAY

        return when {
            targetDate.isBefore(todayDate) -> MeetingStatus.FINISHED
            targetDate.isAfter(todayDate) -> MeetingStatus.UPCOMING
            else -> {
                when {
                    currentTime.isBefore(startTime) -> MeetingStatus.UPCOMING
                    currentTime.isAfter(endTime) || currentTime == endTime -> MeetingStatus.FINISHED
                    else -> MeetingStatus.IN_PROGRESS
                }
            }
        }
    }

    private fun calculateProgress(
        targetDate: LocalDate,
        todayDate: LocalDate,
        currentTime: LocalTime,
        startTime: LocalTime,
        endTime: LocalTime
    ): Float {
        if (targetDate != todayDate) {
            return if (targetDate.isBefore(todayDate)) 1f else 0f
        }
        if (currentTime.isBefore(startTime)) return 0f
        if (!currentTime.isBefore(endTime)) return 1f

        val totalSeconds = Duration.between(startTime, endTime).seconds.toFloat()
        if (totalSeconds <= 0) return 0f

        val elapsedSeconds = Duration.between(startTime, currentTime).seconds.toFloat()
        return (elapsedSeconds / totalSeconds).coerceIn(0f, 1f)
    }
}
