package com.classbell.app.core

import com.classbell.app.data.model.Course
import com.classbell.app.data.model.Holiday
import com.classbell.app.data.model.HolidayType
import com.classbell.app.data.model.MeetingStatus
import com.classbell.app.data.model.Override
import com.classbell.app.data.model.OverrideKind
import com.classbell.app.data.model.PeriodSlot
import com.classbell.app.data.model.Semester
import com.classbell.app.data.model.WeekPattern
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class OccurrenceEngineTest {

    private val semester = Semester(
        id = 1L,
        name = "2026-2027学年第一学期",
        week1Monday = LocalDate.of(2026, 8, 24),
        teachingFrom = 2,
        teachingTo = 18,
        examFrom = 19,
        examTo = 20,
        practiceFrom = 21,
        practiceTo = 21,
        isActive = true
    )

    private val periodSlots = listOf(
        PeriodSlot(1, 1, "第1节", LocalTime.of(8, 30), LocalTime.of(9, 15)),
        PeriodSlot(2, 2, "第2节", LocalTime.of(9, 20), LocalTime.of(10, 5)),
        PeriodSlot(3, 3, "第3节", LocalTime.of(10, 25), LocalTime.of(11, 10)),
        PeriodSlot(4, 4, "第4节", LocalTime.of(11, 15), LocalTime.of(12, 0)),
        PeriodSlot(5, 5, "第5节", LocalTime.of(14, 30), LocalTime.of(15, 15)),
        PeriodSlot(6, 6, "第6节", LocalTime.of(15, 25), LocalTime.of(16, 10)),
        PeriodSlot(7, 7, "第7节", LocalTime.of(16, 20), LocalTime.of(17, 5)),
        PeriodSlot(8, 8, "第8节", LocalTime.of(17, 15), LocalTime.of(18, 0)),
        PeriodSlot(9, 9, "第9节", LocalTime.of(19, 0), LocalTime.of(19, 45)),
        PeriodSlot(10, 10, "第10节", LocalTime.of(19, 55), LocalTime.of(20, 40))
    )

    private val courses = listOf(
        Course(
            id = 1L,
            semesterId = 1L,
            name = "影视声音创作★",
            code = "B090118",
            dayOfWeek = 3, // Wednesday
            periodStart = 1,
            periodEnd = 4,
            weekFrom = 2,
            weekTo = 17,
            weekPattern = WeekPattern.ALL,
            room = "贡院401",
            className = "25级广播电视编导03班",
            studentCount = 49
        ),
        Course(
            id = 2L,
            semesterId = 1L,
            name = "影视声音创作★",
            code = "2040109",
            dayOfWeek = 3, // Wednesday
            periodStart = 5,
            periodEnd = 8,
            weekFrom = 2,
            weekTo = 17,
            weekPattern = WeekPattern.ALL,
            room = "贡院401",
            className = "25级摄影摄像技术01班",
            studentCount = 47
        )
    )

    private val holidays = listOf(
        Holiday(
            id = 1L,
            semesterId = 1L,
            name = "国庆节",
            startDate = LocalDate.of(2026, 10, 1),
            endDate = LocalDate.of(2026, 10, 7),
            type = HolidayType.REST
        )
    )

    @Test
    fun testWednesdayExpansionOnRegularWeek() {
        val testWednesday = LocalDate.of(2026, 9, 2) // Week 2 Wednesday
        val meetings = OccurrenceEngine.meetingsOn(
            date = testWednesday,
            semester = semester,
            courses = courses,
            periodSlots = periodSlots,
            currentTime = LocalTime.of(10, 0),
            todayDate = testWednesday
        )

        assertEquals(2, meetings.size)

        val morningMeeting = meetings[0]
        assertEquals("影视声音创作★", morningMeeting.course.name)
        assertEquals("25级广播电视编导03班", morningMeeting.course.className)
        assertEquals(LocalTime.of(8, 30), morningMeeting.startTime)
        assertEquals(LocalTime.of(12, 0), morningMeeting.endTime)
        assertEquals(MeetingStatus.IN_PROGRESS, morningMeeting.status)
        assertFalse(morningMeeting.isHolidaySuppressed)
        assertTrue(morningMeeting.progress > 0f && morningMeeting.progress < 1f)
        assertEquals(120L, morningMeeting.remainingMinutes) // 10:00 to 12:00 = 120 mins

        val afternoonMeeting = meetings[1]
        assertEquals("25级摄影摄像技术01班", afternoonMeeting.course.className)
        assertEquals(LocalTime.of(14, 30), afternoonMeeting.startTime)
        assertEquals(LocalTime.of(18, 0), afternoonMeeting.endTime)
        assertEquals(MeetingStatus.UPCOMING, afternoonMeeting.status)
        assertEquals(0f, afternoonMeeting.progress, 0.01f)
    }

    @Test
    fun testHolidaySuppression() {
        val nationalDayWednesday = LocalDate.of(2026, 10, 7) // Week 7 Wednesday during National Day
        val meetings = OccurrenceEngine.meetingsOn(
            date = nationalDayWednesday,
            semester = semester,
            courses = courses,
            periodSlots = periodSlots,
            holidays = holidays,
            currentTime = LocalTime.of(10, 0),
            todayDate = nationalDayWednesday
        )

        assertEquals(2, meetings.size)
        assertTrue(meetings[0].isHolidaySuppressed)
        assertEquals("国庆节", meetings[0].holidayName)
        assertEquals(MeetingStatus.HOLIDAY, meetings[0].status)
    }

    @Test
    fun testNonClassDay() {
        val testThursday = LocalDate.of(2026, 9, 3) // Week 2 Thursday
        val meetings = OccurrenceEngine.meetingsOn(
            date = testThursday,
            semester = semester,
            courses = courses,
            periodSlots = periodSlots
        )

        assertTrue(meetings.isEmpty())
    }

    @Test
    fun testMakeupDayShift() {
        val makeupSunday = LocalDate.of(2026, 9, 20) // Makeup day with followsWeekday = 3 (Wednesday)
        val overrides = listOf(
            Override(
                id = 1L,
                semesterId = 1L,
                date = makeupSunday,
                kind = OverrideKind.SHIFT,
                followsWeekday = 3
            )
        )

        val meetings = OccurrenceEngine.meetingsOn(
            date = makeupSunday,
            semester = semester,
            courses = courses,
            periodSlots = periodSlots,
            overrides = overrides
        )

        assertEquals(2, meetings.size)
    }
}
