package com.classbell.app.data

import com.classbell.app.data.model.Affair
import com.classbell.app.data.model.AffairCategory
import com.classbell.app.data.model.BackupData
import com.classbell.app.data.model.Course
import com.classbell.app.data.model.Holiday
import com.classbell.app.data.model.HolidayType
import com.classbell.app.data.model.PeriodSlot
import com.classbell.app.data.model.ReminderChoice
import com.classbell.app.data.model.Semester
import com.classbell.app.data.model.WeekPattern
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class BackupDataTest {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Test
    fun testBackupSerializationRoundTrip() {
        val original = BackupData(
            schemaVersion = 1,
            exportedAt = "2026-09-04T00:00:00",
            semesters = listOf(
                Semester(
                    id = 1L,
                    name = "2026-2027学年第一学期",
                    week1Monday = LocalDate.of(2026, 8, 24),
                    teachingFrom = 2,
                    teachingTo = 18,
                    isActive = true
                )
            ),
            courses = listOf(
                Course(
                    id = 1L,
                    semesterId = 1L,
                    name = "影视声音创作★",
                    dayOfWeek = 3,
                    periodStart = 1,
                    periodEnd = 4,
                    weekFrom = 2,
                    weekTo = 17,
                    weekPattern = WeekPattern.ALL,
                    room = "贡院401",
                    className = "25级广播电视编导03班",
                    studentCount = 49
                )
            ),
            periodSlots = listOf(
                PeriodSlot(1L, 1, "第1节", LocalTime.of(8, 30), LocalTime.of(9, 15))
            ),
            holidays = listOf(
                Holiday(1L, 1L, "国庆节", LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 7), HolidayType.REST)
            ),
            affairs = listOf(
                Affair(1L, "教研组会议", LocalDate.of(2026, 9, 10), LocalTime.of(14, 0), "学院会议室", "教学研讨", AffairCategory.MEETING, ReminderChoice.HOUR1)
            )
        )

        val jsonString = json.encodeToString(original)
        assertNotNull(jsonString)

        val restored = json.decodeFromString<BackupData>(jsonString)
        assertEquals(original.schemaVersion, restored.schemaVersion)
        assertEquals(original.semesters.size, restored.semesters.size)
        assertEquals(original.courses.size, restored.courses.size)
        assertEquals("影视声音创作★", restored.courses[0].name)
        assertEquals(original.periodSlots.size, restored.periodSlots.size)
        assertEquals(original.holidays.size, restored.holidays.size)
        assertEquals(original.affairs.size, restored.affairs.size)
        assertEquals(LocalTime.of(14, 0), restored.affairs[0].time)
        assertEquals(false, restored.affairs[0].isDone)
    }
}
