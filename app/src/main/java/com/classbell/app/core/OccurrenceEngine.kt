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
import com.classbell.app.data.model.SubPeriodKind
import com.classbell.app.data.model.SubPeriodState
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

        val dayOverrides = overrides.filter { it.date == date }
        val wholeDayCancel = dayOverrides.firstOrNull { it.kind == OverrideKind.CANCEL && it.courseId == null }
        val wholeDayShift = dayOverrides.firstOrNull { it.kind == OverrideKind.SHIFT && it.courseId == null }
        val singleCancels = dayOverrides.filter { it.kind == OverrideKind.CANCEL && it.courseId != null }.mapNotNull { it.courseId }.toSet()
        val singleAdjusts = dayOverrides.filter { it.kind == OverrideKind.COURSE_ADJUST && it.courseId != null }

        val holiday = holidays.firstOrNull { !date.isBefore(it.startDate) && !date.isAfter(it.endDate) }

        var isHolidaySuppressed = false
        var holidayName: String? = null
        var effectiveDayOfWeek = date.dayOfWeek.value // 1 (Mon) .. 7 (Sun)
        var isShiftDay = false
        var defaultShiftLabel: String? = null

        fun weekdayToCn(day: Int): String = when (day) {
            1 -> "一"
            2 -> "二"
            3 -> "三"
            4 -> "四"
            5 -> "五"
            6 -> "六"
            else -> "日"
        }

        if (wholeDayCancel != null) {
            isHolidaySuppressed = true
            holidayName = wholeDayCancel.note?.ifBlank { null } ?: "调休停课"
        } else if (wholeDayShift != null) {
            val targetDay = wholeDayShift.followsWeekday ?: date.dayOfWeek.value
            effectiveDayOfWeek = targetDay
            isShiftDay = true
            defaultShiftLabel = wholeDayShift.note?.ifBlank { null } ?: "调·周${weekdayToCn(targetDay)}"
        } else if (holiday != null) {
            holidayName = holiday.name
            when (holiday.type) {
                HolidayType.REST -> {
                    isHolidaySuppressed = true
                }
                HolidayType.MAKEUP -> {
                    val targetDay = holiday.followsWeekday ?: date.dayOfWeek.value
                    effectiveDayOfWeek = targetDay
                    isShiftDay = true
                    defaultShiftLabel = "补·周${weekdayToCn(targetDay)}"
                }
            }
        }

        val periodSlotMap = periodSlots.associateBy { it.order }

        val occurrences = mutableListOf<MeetingOccurrence>()

        // 1. Regular or cloned weekly courses
        val matchingCourses = courses.filter { course ->
            course.dayOfWeek == effectiveDayOfWeek &&
                    weekNumber in course.weekFrom..course.weekTo &&
                    matchesPattern(weekNumber, course.weekPattern) &&
                    weekNumber !in course.weekExclusions &&
                    course.id !in singleCancels
        }

        for (course in matchingCourses) {
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

            val subState = if (date == todayDate && !isHolidaySuppressed) {
                resolveSubPeriodState(course, periodSlots, currentTime)
            } else null

            val progress = if (isHolidaySuppressed) {
                0f
            } else if (subState != null && (subState.kind == SubPeriodKind.IN_CLASS || subState.kind == SubPeriodKind.BREAK)) {
                subState.progress
            } else {
                calculateProgress(
                    targetDate = date,
                    todayDate = todayDate,
                    currentTime = currentTime,
                    startTime = startTime,
                    endTime = endTime
                )
            }

            val remainingMinutes = if (isHolidaySuppressed) {
                0L
            } else if (subState != null && (subState.kind == SubPeriodKind.IN_CLASS || subState.kind == SubPeriodKind.BREAK)) {
                subState.remainingMinutes
            } else if (date == todayDate && currentTime.isAfter(startTime) && currentTime.isBefore(endTime)) {
                ChronoUnit.MINUTES.between(currentTime, endTime).coerceAtLeast(0)
            } else {
                0L
            }

            occurrences.add(
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
                    remainingMinutes = remainingMinutes,
                    subPeriodState = subState,
                    isShiftOverride = isShiftDay,
                    shiftLabel = defaultShiftLabel
                )
            )
        }

        if (!isHolidaySuppressed) {
            // 2. Single-course ad-hoc adjustments on this date
            for (adj in singleAdjusts) {
                val baseCourse = courses.firstOrNull { it.id == adj.courseId } ?: continue
                val pStart = adj.targetPeriodStart ?: baseCourse.periodStart
                val pEnd = adj.targetPeriodEnd ?: baseCourse.periodEnd
                val room = adj.targetRoom?.ifBlank { null } ?: baseCourse.room

                val adjustedCourse = baseCourse.copy(
                    periodStart = pStart,
                    periodEnd = pEnd,
                    room = room
                )

                val startSlot = periodSlotMap[pStart]
                val endSlot = periodSlotMap[pEnd]
                val startTime = startSlot?.startTime ?: LocalTime.of(8, 30)
                val endTime = endSlot?.endTime ?: LocalTime.of(12, 0)

                val status = determineStatus(
                    targetDate = date,
                    todayDate = todayDate,
                    currentTime = currentTime,
                    startTime = startTime,
                    endTime = endTime,
                    isHoliday = false
                )

                val subState = if (date == todayDate) {
                    resolveSubPeriodState(adjustedCourse, periodSlots, currentTime)
                } else null

                val progress = if (subState != null && (subState.kind == SubPeriodKind.IN_CLASS || subState.kind == SubPeriodKind.BREAK)) {
                    subState.progress
                } else {
                    calculateProgress(
                        targetDate = date,
                        todayDate = todayDate,
                        currentTime = currentTime,
                        startTime = startTime,
                        endTime = endTime
                    )
                }

                val remainingMinutes = if (subState != null && (subState.kind == SubPeriodKind.IN_CLASS || subState.kind == SubPeriodKind.BREAK)) {
                    subState.remainingMinutes
                } else if (date == todayDate && currentTime.isAfter(startTime) && currentTime.isBefore(endTime)) {
                    ChronoUnit.MINUTES.between(currentTime, endTime).coerceAtLeast(0)
                } else {
                    0L
                }

                occurrences.add(
                    MeetingOccurrence(
                        course = adjustedCourse,
                        date = date,
                        periodStart = pStart,
                        periodEnd = pEnd,
                        startTime = startTime,
                        endTime = endTime,
                        isHolidaySuppressed = false,
                        holidayName = null,
                        status = status,
                        progress = progress,
                        remainingMinutes = remainingMinutes,
                        subPeriodState = subState,
                        isShiftOverride = true,
                        shiftLabel = adj.note?.ifBlank { null } ?: "调课"
                    )
                )
            }
        }

        return occurrences.sortedWith(compareBy({ it.periodStart }, { it.startTime }))
    }

    fun matchesPattern(weekNumber: Int, pattern: WeekPattern): Boolean {
        return when (pattern) {
            WeekPattern.ALL -> true
            WeekPattern.ODD -> weekNumber % 2 != 0
            WeekPattern.EVEN -> weekNumber % 2 == 0
        }
    }

    /**
     * Resolves the granular sub-period state (In-Class vs Recess/Break) for a given course
     * and current time, based on the university's period slot timetable.
     */
    fun resolveSubPeriodState(
        course: Course,
        periodSlots: List<PeriodSlot>,
        currentTime: LocalTime
    ): SubPeriodState? {
        val periodSlotMap = periodSlots.associateBy { it.order }
        val courseSlots = (course.periodStart..course.periodEnd).mapNotNull { periodSlotMap[it] }
        if (courseSlots.isEmpty()) return null

        val overallStartTime = courseSlots.first().startTime
        val overallEndTime = courseSlots.last().endTime

        // 1. Before overall start time: Pre-class stage
        if (currentTime.isBefore(overallStartTime)) {
            val secsUntilStart = Duration.between(currentTime, overallStartTime).seconds
            val minsUntilStart = ChronoUnit.MINUTES.between(currentTime, overallStartTime)
            return SubPeriodState(
                kind = SubPeriodKind.PRE_CLASS,
                periodOrder = courseSlots.first().order,
                periodIndexInCourse = 1,
                totalPeriodsInCourse = courseSlots.size,
                periodLabel = courseSlots.first().label,
                nextPeriodLabel = courseSlots.first().label,
                startTime = currentTime,
                endTime = overallStartTime,
                progress = 0f,
                remainingSeconds = secsUntilStart,
                remainingMinutes = minsUntilStart,
                isBreak = false,
                breakTitle = "课前准备"
            )
        }

        // 2. After overall end time: All periods finished
        if (currentTime.isAfter(overallEndTime) || currentTime == overallEndTime) {
            return SubPeriodState(
                kind = SubPeriodKind.FINISHED,
                periodOrder = courseSlots.last().order,
                periodIndexInCourse = courseSlots.size,
                totalPeriodsInCourse = courseSlots.size,
                periodLabel = courseSlots.last().label,
                nextPeriodLabel = null,
                startTime = overallEndTime,
                endTime = overallEndTime,
                progress = 1f,
                remainingSeconds = 0L,
                remainingMinutes = 0L,
                isBreak = false,
                breakTitle = null
            )
        }

        // 3. Within course timeframe: Check each period slot and the breaks between them
        for (i in courseSlots.indices) {
            val slot = courseSlots[i]

            // A. Inside this period: [slot.startTime, slot.endTime)
            if (!currentTime.isBefore(slot.startTime) && currentTime.isBefore(slot.endTime)) {
                val totalSecs = Duration.between(slot.startTime, slot.endTime).seconds.coerceAtLeast(1)
                val elapsedSecs = Duration.between(slot.startTime, currentTime).seconds.coerceIn(0, totalSecs)
                val progress = elapsedSecs.toFloat() / totalSecs.toFloat()
                val remSecs = Duration.between(currentTime, slot.endTime).seconds.coerceAtLeast(0)
                val remMins = ChronoUnit.MINUTES.between(currentTime, slot.endTime).coerceAtLeast(0)
                val nextLabel = if (i + 1 < courseSlots.size) courseSlots[i + 1].label else null

                return SubPeriodState(
                    kind = SubPeriodKind.IN_CLASS,
                    periodOrder = slot.order,
                    periodIndexInCourse = i + 1,
                    totalPeriodsInCourse = courseSlots.size,
                    periodLabel = slot.label,
                    nextPeriodLabel = nextLabel,
                    startTime = slot.startTime,
                    endTime = slot.endTime,
                    progress = progress.coerceIn(0f, 1f),
                    remainingSeconds = remSecs,
                    remainingMinutes = remMins,
                    isBreak = false,
                    breakTitle = null
                )
            }

            // B. Inside the break between this period and the next period
            if (i < courseSlots.size - 1) {
                val nextSlot = courseSlots[i + 1]
                if (!currentTime.isBefore(slot.endTime) && currentTime.isBefore(nextSlot.startTime)) {
                    val breakTotalSecs = Duration.between(slot.endTime, nextSlot.startTime).seconds.coerceAtLeast(1)
                    val breakElapsedSecs = Duration.between(slot.endTime, currentTime).seconds.coerceIn(0, breakTotalSecs)
                    val breakProgress = breakElapsedSecs.toFloat() / breakTotalSecs.toFloat()
                    val remSecs = Duration.between(currentTime, nextSlot.startTime).seconds.coerceAtLeast(0)
                    val remMins = ChronoUnit.MINUTES.between(currentTime, nextSlot.startTime).coerceAtLeast(0)
                    val breakMins = Duration.between(slot.endTime, nextSlot.startTime).toMinutes()
                    val title = if (breakMins >= 15) "大课间休息" else "课间休息"

                    return SubPeriodState(
                        kind = SubPeriodKind.BREAK,
                        periodOrder = slot.order,
                        periodIndexInCourse = i + 1,
                        totalPeriodsInCourse = courseSlots.size,
                        periodLabel = slot.label,
                        nextPeriodLabel = nextSlot.label,
                        startTime = slot.endTime,
                        endTime = nextSlot.startTime,
                        progress = breakProgress.coerceIn(0f, 1f),
                        remainingSeconds = remSecs,
                        remainingMinutes = remMins,
                        isBreak = true,
                        breakTitle = title
                    )
                }
            }
        }

        return null
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
