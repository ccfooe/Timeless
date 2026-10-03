package com.classbell.app.llm

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class LlmParserTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
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
    fun testParseTimetableJsonWithTeacher() {
        val rawJson = """
            [
              {
                "name": "大学英语(一)",
                "code": "ENG101",
                "teacher": "张教授",
                "dayOfWeek": 1,
                "periodStart": 3,
                "periodEnd": 4,
                "weeks": { "from": 1, "to": 16, "pattern": "all" },
                "campus": "主校区",
                "room": "第一教学楼302",
                "className": "2026级计算机科学1班",
                "studentCount": 60,
                "nature": "必修",
                "examType": "考试"
              }
            ]
        """.trimIndent()

        val list = json.decodeFromString<List<ParsedCourseItem>>(rawJson)
        assertEquals(1, list.size)
        val course = list[0]
        assertEquals("大学英语(一)", course.name)
        assertEquals("张教授", course.teacher)
        assertEquals("第一教学楼302", course.room)
        assertEquals("2026级计算机科学1班", course.className)
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

    @Test
    fun testParseAffairJsonComplete() {
        val rawJson = """
            {
              "title": "2026年秋季学期本科教学研讨会",
              "date": "2026-09-18",
              "time": "14:30",
              "place": "行政楼401会议室",
              "category": "MEETING",
              "note": "请带上教学大纲初稿，提前10分钟签到"
            }
        """.trimIndent()

        val affair = json.decodeFromString<ParsedAffair>(rawJson)
        assertEquals("2026年秋季学期本科教学研讨会", affair.title)
        assertEquals("2026-09-18", affair.date)
        assertEquals("14:30", affair.time)
        assertEquals("行政楼401会议室", affair.place)
        assertEquals("MEETING", affair.category)
        assertEquals("请带上教学大纲初稿，提前10分钟签到", affair.note)
    }

    @Test
    fun testParseAffairJsonPartial() {
        val rawJson = """
            {
              "title": "提交学期教学进度表",
              "date": null,
              "time": null,
              "place": null,
              "category": "AFFAIR",
              "note": null
            }
        """.trimIndent()

        val affair = json.decodeFromString<ParsedAffair>(rawJson)
        assertEquals("提交学期教学进度表", affair.title)
        assertEquals(null, affair.date)
        assertEquals(null, affair.time)
        assertEquals(null, affair.place)
        assertEquals("AFFAIR", affair.category)
        assertEquals(null, affair.note)
    }

    @Test
    fun testAffairPromptContainsReferenceDate() {
        val testDate = java.time.LocalDate.of(2026, 9, 15) // Tuesday
        val prompt = LlmPrompts.getAffairExtractionPrompt(testDate)
        assertNotNull(prompt)
        org.junit.Assert.assertTrue(prompt.contains("2026-09-15"))
        org.junit.Assert.assertTrue(prompt.contains("星期二"))
        org.junit.Assert.assertTrue(prompt.contains("MEETING"))
        org.junit.Assert.assertTrue(prompt.contains("EXAM"))
        org.junit.Assert.assertTrue(prompt.contains("AFFAIR"))
    }

    @Test
    fun testParseCalendarDirectHolidayArrayFallback() {
        // Test when model outputs an array of holidays instead of object
        val rawHolidayArray = """
            [
              { "name": "中秋节", "startDate": "2026-09-25", "endDate": "2026-09-27", "type": "rest" },
              { "name": "国定假日", "startDate": "2026-10-01", "endDate": "2026-10-07", "type": "rest" }
            ]
        """.trimIndent()

        val holidays = json.decodeFromString<List<ParsedHolidayItem>>(rawHolidayArray)
        assertEquals(2, holidays.size)
        assertEquals("中秋节", holidays[0].name)
        val wrapped = ParsedCalendarResult(
            semesterName = "校历与假期安排",
            week1Monday = "2026-08-24",
            teachingWeeks = ParsedWeekRange(1, 18),
            holidays = holidays
        )
        assertEquals(2, wrapped.holidays.size)
    }

    @Test
    fun testParseTimetableJsonWithNullFields() {
        val rawJson = """
            [
              {
                "name": "AI网站应用开发",
                "dayOfWeek": 1,
                "periodStart": 3,
                "periodEnd": 5,
                "weeks": { "from": 1, "to": 18, "pattern": "all" },
                "campus": null,
                "room": "G601",
                "className": null,
                "studentCount": null,
                "teacher": "郑老师",
                "nature": null,
                "examType": null
              }
            ]
        """.trimIndent()

        val list = json.decodeFromString<List<ParsedCourseItem>>(rawJson)
        assertEquals(1, list.size)
        val course = list[0]
        assertEquals("AI网站应用开发", course.name)
        assertEquals("G601", course.room)
        assertEquals(null, course.className)
        assertEquals(null, course.studentCount)
        assertEquals("郑老师", course.teacher)
    }

    @Test
    fun testParseUniversalIngestWithMultiShifts() {
        val rawJson = """
            {
              "primaryIntent": "SHIFT_OVERRIDE",
              "summary": "已识别到2条调课通知：9月27日上周三课，10月10日上周四课",
              "shiftOverrides": [
                {
                  "targetDate": "2026-09-27",
                  "kind": "SHIFT",
                  "followsWeekday": 3,
                  "note": "补9月30日(周三)课程"
                },
                {
                  "targetDate": "2026-10-10",
                  "kind": "SHIFT",
                  "followsWeekday": 4,
                  "note": "补10月8日(周四)课程"
                }
              ]
            }
        """.trimIndent()

        val parsed = json.decodeFromString<UniversalIngestResult>(rawJson)
        assertEquals("SHIFT_OVERRIDE", parsed.primaryIntent)
        assertEquals(2, parsed.shiftOverrides.size)
        val resolved = parsed.resolvedShiftOverrides()
        assertEquals(2, resolved.size)
        assertEquals("2026-09-27", resolved[0].targetDate)
        assertEquals(3, resolved[0].followsWeekday)
        assertEquals("2026-10-10", resolved[1].targetDate)
        assertEquals(4, resolved[1].followsWeekday)
    }

    @Test
    fun testUniversalIngestMultipleAffairsCap3() {
        val rawJson = """
            {
              "primaryIntent": "AFFAIR",
              "summary": "各部门负责人需在国庆前提交工作目标、内部管理、考勤规范及违规草案",
              "affairs": [
                {
                  "title": "提交部门工作目标与管理规范",
                  "date": "2026-09-30",
                  "time": null,
                  "place": null,
                  "category": "AFFAIR",
                  "note": "销售部、产品部、管理部"
                },
                {
                  "title": "优化规范考勤与请假监督流程",
                  "date": "2026-09-30",
                  "time": null,
                  "place": null,
                  "category": "AFFAIR",
                  "note": "全员考勤红线"
                },
                {
                  "title": "起草严重违反规章制度行为草案",
                  "date": "2026-09-30",
                  "time": null,
                  "place": null,
                  "category": "AFFAIR",
                  "note": "由智哥负责起草"
                },
                {
                  "title": "第四个多余事项（应被最多3项规则截断）",
                  "date": "2026-09-30",
                  "category": "AFFAIR"
                }
              ]
            }
        """.trimIndent()

        val parsed = json.decodeFromString<UniversalIngestResult>(rawJson)
        assertEquals("AFFAIR", parsed.primaryIntent)
        assertEquals(4, parsed.affairs.size)

        // resolvedAffairs() must cap at 3 items
        val resolved = parsed.resolvedAffairs()
        assertEquals(3, resolved.size)
        assertEquals("提交部门工作目标与管理规范", resolved[0].title)
        assertEquals("优化规范考勤与请假监督流程", resolved[1].title)
        assertEquals("起草严重违反规章制度行为草案", resolved[2].title)
        assertEquals("2026-09-30", resolved[0].date)
        assertEquals("AFFAIR", resolved[0].category)
    }

    @Test
    fun testUniversalIngestPromptContainsActionOrientedAndMultiAffairsRules() {
        val prompt = LlmPrompts.getUniversalIngestPrompt(java.time.LocalDate.of(2026, 9, 30))
        assertNotNull(prompt)
        // Must contain action-oriented directives
        org.junit.Assert.assertTrue("Prompt must guide action-oriented titles", prompt.contains("行动终点优先") || prompt.contains("动词+核心对象"))
        // Must guide multi-affair extraction up to 3 items
        org.junit.Assert.assertTrue("Prompt must instruct up to 3 affairs", prompt.contains("最多不超过3个") || prompt.contains("不超过3个待办"))
        // Must forbid generic 'meeting' for submission tasks
        org.junit.Assert.assertTrue("Prompt must discourage generic meeting", prompt.contains("严禁") || prompt.contains("不要笼统"))
    }

    @Test
    fun testRealWorldNoticeMultiAffairsIngestion() {
        val userNoticeJson = """
            {
              "primaryIntent": "AFFAIR",
              "summary": "各部门负责人需在国庆前提交工作目标、内部管理规范及违规行为草案",
              "affairs": [
                {
                  "title": "提交部门工作目标与管理规范",
                  "date": "2026-09-30",
                  "time": null,
                  "place": null,
                  "category": "AFFAIR",
                  "note": "销售部由杨姐负责/产品部由李超负责/管理部由刘智负责，国庆前提交总经办"
                },
                {
                  "title": "规范与优化考勤请假流程",
                  "date": "2026-09-30",
                  "time": null,
                  "place": null,
                  "category": "AFFAIR",
                  "note": "设为部门管理红线，优化考勤请假监督流程"
                },
                {
                  "title": "起草严重违反规章制度行为草案",
                  "date": "2026-09-30",
                  "time": null,
                  "place": null,
                  "category": "AFFAIR",
                  "note": "由智哥负责起草，国庆前文字版提交总经办"
                }
              ]
            }
        """.trimIndent()

        val parsed = json.decodeFromString<UniversalIngestResult>(userNoticeJson)
        assertEquals("AFFAIR", parsed.primaryIntent)
        val affairs = parsed.resolvedAffairs()
        assertEquals(3, affairs.size)
        // All categories must be AFFAIR (not generic MEETING)
        affairs.forEach {
            assertEquals("AFFAIR", it.category)
            assertEquals("2026-09-30", it.date)
            org.junit.Assert.assertTrue("Title must not be generic meeting", !it.title.contains("部门会议"))
        }
        assertEquals("提交部门工作目标与管理规范", affairs[0].title)
        assertEquals("起草严重违反规章制度行为草案", affairs[2].title)
    }

    @Test
    fun testPromptIncludesTimezoneAndNoiseReductionDirectives() {
        val testDate = java.time.LocalDate.of(2026, 10, 2)
        val shanghaiZone = java.time.ZoneId.of("Asia/Shanghai")
        val prompt = LlmPrompts.getUniversalIngestPrompt(testDate, shanghaiZone)

        // 1. Must include user timezone context
        org.junit.Assert.assertTrue("Prompt must include user timezone", prompt.contains("Asia/Shanghai"))
        org.junit.Assert.assertTrue("Prompt must guide timezone conversion", prompt.contains("时区自适应自动换算") || prompt.contains("时区"))

        // 2. Must include noise reduction guidelines
        org.junit.Assert.assertTrue("Prompt must guide noise reduction", prompt.contains("降噪") || prompt.contains("信噪比"))
        org.junit.Assert.assertTrue("Prompt must filter greeting & calendar links", prompt.contains("Add to Calendar") || prompt.contains("模板噪声"))

        // 3. Must prioritize absolute calendar dates over confusing terms
        org.junit.Assert.assertTrue("Prompt must prioritize absolute dates", prompt.contains("绝对公历日期优先") || prompt.contains("Feb"))

        // 4. Must guide online meeting platform extraction
        org.junit.Assert.assertTrue("Prompt must guide online meeting place", prompt.contains("Zoom") || prompt.contains("腾讯会议"))
    }

    @Test
    fun testEnglishEmailZoomMeetingJsonParsing() {
        val rawJson = """
            {
              "primaryIntent": "AFFAIR",
              "summary": "艺术治疗干预第5天会议，伦敦时间换算至北京时间2月6日18:00",
              "affairs": [
                {
                  "title": "Art Therapy Interventions - Day 5 (Autumn 2026)",
                  "date": "2027-02-06",
                  "time": "18:00",
                  "place": "Zoom (会议ID: 857 4509 3820)",
                  "category": "MEETING",
                  "note": "原时间: 2027-02-06 10:00 AM (London，已自动换算为北京时间)\nPasscode: 576394\n入会链接: https://us02web.zoom.us/w/85745093820?tk=..."
                }
              ]
            }
        """.trimIndent()

        val parsed = json.decodeFromString<UniversalIngestResult>(rawJson)
        val affairs = parsed.resolvedAffairs()
        assertEquals(1, affairs.size)
        val affair = affairs[0]
        assertEquals("2027-02-06", affair.date)
        assertEquals("18:00", affair.time)
        org.junit.Assert.assertTrue("Place must indicate Zoom", affair.place?.contains("Zoom") == true)
        org.junit.Assert.assertTrue("Note must have passcode", affair.note?.contains("576394") == true)
        org.junit.Assert.assertTrue("Note must have timezone conversion note", affair.note?.contains("已自动换算") == true)
    }

    @Test
    fun testTimeUtilsInternationalZoneConversion() {
        val londonZone = com.classbell.app.core.TimeUtils.resolveZoneByName("London")
        org.junit.Assert.assertNotNull(londonZone)
        val beijingZone = com.classbell.app.core.TimeUtils.resolveZoneByName("Asia/Shanghai")
        org.junit.Assert.assertNotNull(beijingZone)

        // 2027-02-06 10:00 London (GMT/UTC+0) to Shanghai (UTC+8) -> 2027-02-06 18:00
        val (convertedDate, convertedTime) = com.classbell.app.core.TimeUtils.convertBetweenZones(
            date = java.time.LocalDate.of(2027, 2, 6),
            time = java.time.LocalTime.of(10, 0),
            fromZone = londonZone!!,
            toZone = beijingZone!!
        )
        assertEquals(java.time.LocalDate.of(2027, 2, 6), convertedDate)
        assertEquals(java.time.LocalTime.of(18, 0), convertedTime)
    }

    @Test
    fun testSummerVsWinterLondonTimeConversion() {
        val londonZone = com.classbell.app.core.TimeUtils.resolveZoneByName("London")!!
        val beijingZone = com.classbell.app.core.TimeUtils.resolveZoneByName("Asia/Shanghai")!!

        // 1. Winter (February): London is GMT (UTC+0), difference is 8 hours
        val winterDate = java.time.LocalDate.of(2027, 2, 6)
        val winterSeason = com.classbell.app.core.TimeUtils.getZoneSeasonName(londonZone, winterDate)
        org.junit.Assert.assertTrue("Winter must indicate GMT", winterSeason.contains("GMT") || winterSeason.contains("冬令时"))
        val (wDate, wTime) = com.classbell.app.core.TimeUtils.convertBetweenZones(
            date = winterDate,
            time = java.time.LocalTime.of(10, 0),
            fromZone = londonZone,
            toZone = beijingZone
        )
        assertEquals(java.time.LocalTime.of(18, 0), wTime) // 10:00 + 8h = 18:00

        // 2. Summer (July): London is BST (UTC+1), difference is 7 hours
        val summerDate = java.time.LocalDate.of(2027, 7, 15)
        val summerSeason = com.classbell.app.core.TimeUtils.getZoneSeasonName(londonZone, summerDate)
        org.junit.Assert.assertTrue("Summer must indicate BST", summerSeason.contains("BST") || summerSeason.contains("夏令时"))
        val (sDate, sTime) = com.classbell.app.core.TimeUtils.convertBetweenZones(
            date = summerDate,
            time = java.time.LocalTime.of(10, 0),
            fromZone = londonZone,
            toZone = beijingZone
        )
        assertEquals(java.time.LocalTime.of(17, 0), sTime) // 10:00 + 7h = 17:00
    }

    @Test
    fun testPromptCoversAllSixHighFrequencyScenarios() {
        val prompt = LlmPrompts.getUniversalIngestPrompt(
            referenceDate = java.time.LocalDate.of(2026, 10, 2),
            userZone = java.time.ZoneId.of("Asia/Shanghai")
        )
        // 1. 线上会议
        org.junit.Assert.assertTrue(prompt.contains("Zoom") || prompt.contains("腾讯会议"))
        // 2. 差旅交通
        org.junit.Assert.assertTrue(prompt.contains("交通") || prompt.contains("车次") || prompt.contains("航班"))
        // 3. 医疗健康
        org.junit.Assert.assertTrue(prompt.contains("挂号") || prompt.contains("就诊") || prompt.contains("体检"))
        // 4. 高校教务与长公文
        org.junit.Assert.assertTrue(prompt.contains("公文") || prompt.contains("行动终点优先"))
        // 5. 学术讲座
        org.junit.Assert.assertTrue(prompt.contains("讲座") || prompt.contains("研讨会"))
        // 6. 生活履约/缴费
        org.junit.Assert.assertTrue(prompt.contains("缴费") || prompt.contains("还款"))
        // 7. 夏令时/冬令时指引
        org.junit.Assert.assertTrue(prompt.contains("夏令时") || prompt.contains("冬令时"))
    }

    @Test
    fun testTravelAndMedicalNoticeJsonParsing() {
        // Test Travel Notice (12306 Train)
        val travelJson = """
            {
              "primaryIntent": "AFFAIR",
              "summary": "乘G1234次列车由上海虹桥前往北京南",
              "affairs": [
                {
                  "title": "乘 G1234 次列车赴北京南",
                  "date": "2026-10-15",
                  "time": "08:15",
                  "place": "上海虹桥站 检票口12A",
                  "category": "AFFAIR",
                  "note": "车厢座位: 08车12F (二等座)\n到达时间: 12:48 北京南站"
                }
              ]
            }
        """.trimIndent()
        val parsedTravel = json.decodeFromString<UniversalIngestResult>(travelJson)
        val travelAffair = parsedTravel.resolvedAffairs()[0]
        assertEquals("乘 G1234 次列车赴北京南", travelAffair.title)
        assertEquals("08:15", travelAffair.time)
        org.junit.Assert.assertTrue(travelAffair.note?.contains("08车12F") == true)

        // Test Medical Notice (Hospital Appointment)
        val medicalJson = """
            {
              "primaryIntent": "AFFAIR",
              "summary": "北京协和医院皮肤科普通门诊预约就诊",
              "affairs": [
                {
                  "title": "北京协和医院 皮肤科门诊",
                  "date": "2026-10-20",
                  "time": "09:30",
                  "place": "东单院区门诊楼3层 诊区A",
                  "category": "AFFAIR",
                  "note": "就诊序号: 38号\n注意事项: 请提前30分钟到达分诊台取号"
                }
              ]
            }
        """.trimIndent()
        val parsedMedical = json.decodeFromString<UniversalIngestResult>(medicalJson)
        val medicalAffair = parsedMedical.resolvedAffairs()[0]
        assertEquals("北京协和医院 皮肤科门诊", medicalAffair.title)
        assertEquals("09:30", medicalAffair.time)
        org.junit.Assert.assertTrue(medicalAffair.note?.contains("提前30分钟") == true)
    }
}





