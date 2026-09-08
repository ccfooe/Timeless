package com.classbell.app.data.repository

import com.classbell.app.core.OccurrenceEngine
import com.classbell.app.core.WeekCalculator
import com.classbell.app.data.local.CourseDao
import com.classbell.app.data.local.HolidayDao
import com.classbell.app.data.local.OverrideDao
import com.classbell.app.data.local.PeriodSlotDao
import com.classbell.app.data.local.SemesterDao
import com.classbell.app.data.model.Course
import com.classbell.app.data.model.Holiday
import com.classbell.app.data.model.MeetingOccurrence
import com.classbell.app.data.model.MeetingStatus
import com.classbell.app.data.model.Override
import com.classbell.app.data.model.PeriodSlot
import com.classbell.app.data.model.Semester
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import java.time.LocalDate
import java.time.LocalTime

data class CourseStats(
    val course: Course,
    val meetingsHeld: Int,
    val totalPlannedMeetings: Int,
    val accumulatedHours: Int,
    val totalStudents: Int
)

data class TeachingStats(
    val semesterName: String,
    val thisWeekMeetingsCount: Int,
    val thisWeekHours: Int,
    val totalAccumulatedHours: Int,
    val totalMeetingsHeld: Int,
    val totalStudentsReached: Int,
    val courseStatsList: List<CourseStats>
)

class ScheduleRepository(
    private val semesterDao: SemesterDao,
    private val courseDao: CourseDao,
    private val periodSlotDao: PeriodSlotDao,
    private val holidayDao: HolidayDao,
    private val overrideDao: OverrideDao
) {

    val activeSemester: Flow<Semester?> = semesterDao.getActiveSemester()
    val periodSlots: Flow<List<PeriodSlot>> = periodSlotDao.getAllPeriodSlots()

    private val _manualSession = MutableStateFlow<MeetingOccurrence?>(null)
    val manualSession: StateFlow<MeetingOccurrence?> = _manualSession.asStateFlow()

    fun startManualSession(
        course: Course,
        durationMinutes: Int = 45,
        startTime: LocalTime = LocalTime.now(),
        todayDate: LocalDate = LocalDate.now()
    ): MeetingOccurrence {
        val endTime = startTime.plusMinutes(durationMinutes.toLong())
        val meeting = MeetingOccurrence(
            course = course,
            date = todayDate,
            periodStart = course.periodStart,
            periodEnd = course.periodEnd,
            startTime = startTime,
            endTime = endTime,
            status = MeetingStatus.IN_PROGRESS,
            progress = 0f,
            remainingMinutes = durationMinutes.toLong()
        )
        _manualSession.value = meeting
        return meeting
    }

    fun endManualSession() {
        _manualSession.value = null
    }

    fun extendManualSession(extraMinutes: Int = 5) {
        val current = _manualSession.value ?: return
        val newEndTime = current.endTime.plusMinutes(extraMinutes.toLong())
        val newRemaining = java.time.temporal.ChronoUnit.MINUTES.between(LocalTime.now(), newEndTime).coerceAtLeast(0)
        _manualSession.value = current.copy(
            endTime = newEndTime,
            remainingMinutes = newRemaining
        )
    }

    fun getMeetingsOnDate(
        targetDate: LocalDate,
        currentTime: LocalTime = LocalTime.now(),
        todayDate: LocalDate = LocalDate.now()
    ): Flow<List<MeetingOccurrence>> {
        return activeSemester.flatMapLatest { semester ->
            if (semester == null) {
                flowOf(emptyList())
            } else {
                combine(
                    courseDao.getCoursesForSemester(semester.id),
                    periodSlotDao.getAllPeriodSlots(),
                    holidayDao.getHolidaysForSemester(semester.id),
                    overrideDao.getOverridesForSemester(semester.id)
                ) { courses, periods, holidays, overrides ->
                    OccurrenceEngine.meetingsOn(
                        date = targetDate,
                        semester = semester,
                        courses = courses,
                        periodSlots = periods,
                        holidays = holidays,
                        overrides = overrides,
                        currentTime = currentTime,
                        todayDate = todayDate
                    )
                }
            }
        }
    }

    fun getWeeklyMeetings(
        weekNumber: Int,
        todayDate: LocalDate = LocalDate.now(),
        currentTime: LocalTime = LocalTime.now()
    ): Flow<Map<Int, List<MeetingOccurrence>>> {
        return activeSemester.flatMapLatest { semester ->
            if (semester == null) {
                flowOf(emptyMap())
            } else {
                val (monday, _) = WeekCalculator.getWeekDateRange(weekNumber, semester.week1Monday)
                combine(
                    courseDao.getCoursesForSemester(semester.id),
                    periodSlotDao.getAllPeriodSlots(),
                    holidayDao.getHolidaysForSemester(semester.id),
                    overrideDao.getOverridesForSemester(semester.id)
                ) { courses, periods, holidays, overrides ->
                    val map = mutableMapOf<Int, List<MeetingOccurrence>>()
                    for (i in 0 until 7) {
                        val dayDate = monday.plusDays(i.toLong())
                        val dayOfWeek = dayDate.dayOfWeek.value
                        val dayMeetings = OccurrenceEngine.meetingsOn(
                            date = dayDate,
                            semester = semester,
                            courses = courses,
                            periodSlots = periods,
                            holidays = holidays,
                            overrides = overrides,
                            currentTime = currentTime,
                            todayDate = todayDate
                        )
                        map[dayOfWeek] = dayMeetings
                    }
                    map
                }
            }
        }
    }

    fun getTeachingStats(
        todayDate: LocalDate = LocalDate.now(),
        currentTime: LocalTime = LocalTime.now()
    ): Flow<TeachingStats?> {
        return activeSemester.flatMapLatest { semester ->
            if (semester == null) {
                flowOf(null)
            } else {
                val currentWeek = WeekCalculator.calculateWeekNumber(todayDate, semester.week1Monday)
                combine(
                    courseDao.getCoursesForSemester(semester.id),
                    periodSlotDao.getAllPeriodSlots(),
                    holidayDao.getHolidaysForSemester(semester.id),
                    overrideDao.getOverridesForSemester(semester.id)
                ) { courses, periods, holidays, overrides ->
                    val courseStatsList = mutableListOf<CourseStats>()
                    var totalHeldMeetings = 0
                    var totalAccumHours = 0
                    var thisWeekMeetings = 0
                    var thisWeekHours = 0

                    val (thisWeekMonday, thisWeekSunday) = WeekCalculator.getWeekDateRange(currentWeek, semester.week1Monday)

                    for (course in courses) {
                        var heldForCourse = 0
                        var totalPlannedForCourse = 0

                        val periodsInCourse = (course.periodEnd - course.periodStart + 1).coerceAtLeast(1)

                        // Traverse weeks from course.weekFrom to course.weekTo
                        for (w in course.weekFrom..course.weekTo) {
                            if (!OccurrenceEngine.matchesPattern(w, course.weekPattern)) continue
                            if (w in course.weekExclusions) continue

                            val (weekMonday, _) = WeekCalculator.getWeekDateRange(w, semester.week1Monday)
                            val courseDate = weekMonday.plusDays((course.dayOfWeek - 1).toLong())

                            val dayMeetings = OccurrenceEngine.meetingsOn(
                                date = courseDate,
                                semester = semester,
                                courses = listOf(course),
                                periodSlots = periods,
                                holidays = holidays,
                                overrides = overrides,
                                currentTime = currentTime,
                                todayDate = todayDate
                            )

                            if (dayMeetings.isNotEmpty()) {
                                totalPlannedForCourse++
                                val meeting = dayMeetings.first()
                                if (!meeting.isHolidaySuppressed && (courseDate.isBefore(todayDate) || (courseDate == todayDate && currentTime.isAfter(meeting.endTime)))) {
                                    heldForCourse++
                                }
                            }
                        }

                        val hoursForCourse = heldForCourse * periodsInCourse
                        totalHeldMeetings += heldForCourse
                        totalAccumHours += hoursForCourse

                        courseStatsList.add(
                            CourseStats(
                                course = course,
                                meetingsHeld = heldForCourse,
                                totalPlannedMeetings = totalPlannedForCourse,
                                accumulatedHours = hoursForCourse,
                                totalStudents = course.studentCount
                            )
                        )
                    }

                    // Compute this week stats
                    for (dayIndex in 0 until 7) {
                        val d = thisWeekMonday.plusDays(dayIndex.toLong())
                        val meetings = OccurrenceEngine.meetingsOn(
                            date = d,
                            semester = semester,
                            courses = courses,
                            periodSlots = periods,
                            holidays = holidays,
                            overrides = overrides,
                            currentTime = currentTime,
                            todayDate = todayDate
                        )
                        for (m in meetings) {
                            if (!m.isHolidaySuppressed) {
                                val pCount = (m.periodEnd - m.periodStart + 1).coerceAtLeast(1)
                                thisWeekMeetings++
                                thisWeekHours += pCount
                            }
                        }
                    }

                    val totalStudents = courses.sumOf { it.studentCount }

                    TeachingStats(
                        semesterName = semester.name,
                        thisWeekMeetingsCount = thisWeekMeetings,
                        thisWeekHours = thisWeekHours,
                        totalAccumulatedHours = totalAccumHours,
                        totalMeetingsHeld = totalHeldMeetings,
                        totalStudentsReached = totalStudents,
                        courseStatsList = courseStatsList
                    )
                }
            }
        }
    }

    suspend fun getHolidaysForSemesterSync(semesterId: Long): List<Holiday> =
        holidayDao.getHolidaysForSemesterSync(semesterId)

    suspend fun insertHoliday(holiday: Holiday) = holidayDao.insert(holiday)
    suspend fun deleteHoliday(holiday: Holiday) = holidayDao.delete(holiday)

    suspend fun getOverridesForSemesterSync(semesterId: Long): List<Override> =
        overrideDao.getOverridesForSemesterSync(semesterId)

    suspend fun insertOverride(override: Override) = overrideDao.insert(override)
    suspend fun deleteOverride(override: Override) = overrideDao.delete(override)
}
