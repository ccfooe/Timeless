package com.classbell.app.llm

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class LlmParserTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @Test
    fun testParseTimetableJson() {
        val rawJson = """
            [
              {
                "name": "影视声音创作★",
                "code": "B090118",
                "dayOfWeek": 3,
                "periodStart": 1,
                "periodEnd": 4,
                "weeks": { "from": 2, "to": 17, "pattern": "all" },
                "campus": "金牛校区",
                "room": "贡院401",
                "className": "25级广播电视编导03班",
                "studentCount": 49,
                "nature": "必修",
                "examType": "考试"
              }
            ]
        """.trimIndent()

        val list = json.decodeFromString<List<ParsedCourseItem>>(rawJson)
        assertEquals(1, list.size)
        val course = list[0]
        assertEquals("影视声音创作★", course.name)
        assertEquals("B090118", course.code)
        assertEquals(3, course.dayOfWeek)
        assertEquals(1, course.periodStart)
        assertEquals(4, course.periodEnd)
        assertEquals(49, course.studentCount)
    }

    @Test
    fun testParseCalendarJson() {
        val rawJson = """
            {
              "semesterName": "2026-2027学年度第一学期",
              "week1Monday": "2026-08-24",
              "teachingWeeks": { "from": 2, "to": 18 },
              "examWeeks": { "from": 19, "to": 20 },
              "practiceWeeks": { "from": 21, "to": 21 },
              "holidays": [
                { "name": "国庆节", "startDate": "2026-10-01", "endDate": "2026-10-07", "type": "rest" }
              ],
              "makeupWorkdays": [
                { "date": "2026-10-10", "followsWeekday": null }
              ],
              "notes": [ "正式行课: 8月31日" ]
            }
        """.trimIndent()

        val cal = json.decodeFromString<ParsedCalendarResult>(rawJson)
        assertEquals("2026-2027学年度第一学期", cal.semesterName)
        assertEquals("2026-08-24", cal.week1Monday)
        assertEquals(2, cal.teachingWeeks.from)
        assertEquals(18, cal.teachingWeeks.to)
        assertEquals(1, cal.holidays.size)
        assertEquals("国庆节", cal.holidays[0].name)
        assertEquals(1, cal.makeupWorkdays.size)
    }
}
