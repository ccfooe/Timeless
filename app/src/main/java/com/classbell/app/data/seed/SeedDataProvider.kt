package com.classbell.app.data.seed

import com.classbell.app.data.model.Course
import com.classbell.app.data.model.Holiday
import com.classbell.app.data.model.HolidayType
import com.classbell.app.data.model.PeriodSlot
import com.classbell.app.data.model.Semester
import com.classbell.app.data.model.WeekPattern
import java.time.LocalDate
import java.time.LocalTime

object SeedDataProvider {

    fun getDefaultSemester(): Semester {
        return Semester(
            id = 1L,
            name = "2026–2027学年第一学期",
            week1Monday = LocalDate.of(2026, 8, 24),
            teachingFrom = 2,
            teachingTo = 18,
            examFrom = 19,
            examTo = 20,
            practiceFrom = 21,
            practiceTo = 21,
            notes = "",
            isActive = true
        )
    }

    fun getDefaultHolidays(semesterId: Long = 1L): List<Holiday> {
        return listOf(
            Holiday(
                id = 1L,
                semesterId = semesterId,
                name = "中秋节",
                startDate = LocalDate.of(2026, 9, 25),
                endDate = LocalDate.of(2026, 9, 27),
                type = HolidayType.REST
            ),
            Holiday(
                id = 2L,
                semesterId = semesterId,
                name = "补班 (中秋)",
                startDate = LocalDate.of(2026, 9, 20),
                endDate = LocalDate.of(2026, 9, 20),
                type = HolidayType.MAKEUP,
                followsWeekday = null
            ),
            Holiday(
                id = 3L,
                semesterId = semesterId,
                name = "国庆节",
                startDate = LocalDate.of(2026, 10, 1),
                endDate = LocalDate.of(2026, 10, 7),
                type = HolidayType.REST
            ),
            Holiday(
                id = 4L,
                semesterId = semesterId,
                name = "补班 (国庆)",
                startDate = LocalDate.of(2026, 10, 10),
                endDate = LocalDate.of(2026, 10, 10),
                type = HolidayType.MAKEUP,
                followsWeekday = null
            ),
            Holiday(
                id = 5L,
                semesterId = semesterId,
                name = "元旦节",
                startDate = LocalDate.of(2027, 1, 1),
                endDate = LocalDate.of(2027, 1, 3),
                type = HolidayType.REST
            )
        )
    }

    fun getDefaultPeriodSlots(): List<PeriodSlot> {
        return listOf(
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
    }

    fun getDefaultCourses(semesterId: Long = 1L): List<Course> {
        return emptyList()
    }
}
