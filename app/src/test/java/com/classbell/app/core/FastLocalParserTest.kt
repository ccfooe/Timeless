package com.classbell.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class FastLocalParserTest {

    private val refDate = LocalDate.of(2026, 9, 29) // Tuesday

    @Test
    fun testParseSimpleMeeting() {
        val result = FastLocalParser.tryParse("明天下午3点开会", refDate)
        assertNotNull(result)
        assertEquals("AFFAIR", result!!.primaryIntent)
        val affair = result.affairs.first()
        assertEquals("2026-09-30", affair.date)
        assertEquals("15:00", affair.time)
        assertEquals("MEETING", affair.category)
    }

    @Test
    fun testParseMeetingWithLocation() {
        val result = FastLocalParser.tryParse("明天下午3点系里开教学研讨会，在知行楼302", refDate)
        assertNotNull(result)
        val affair = result!!.affairs.first()
        assertEquals("2026-09-30", affair.date)
        assertEquals("15:00", affair.time)
        assertEquals("知行楼302", affair.place)
        assertEquals("MEETING", affair.category)
    }

    @Test
    fun testParseFridayAffair() {
        val result = FastLocalParser.tryParse("周五上午10点去教研室交大纲", refDate)
        assertNotNull(result)
        val affair = result!!.affairs.first()
        assertEquals("2026-10-02", affair.date) // 2026-09-29 Tuesday -> Friday is 10-02
        assertEquals("10:00", affair.time)
        assertEquals("教研室", affair.place)
    }

    @Test
    fun testParseEveningTask() {
        val result = FastLocalParser.tryParse("今晚8点交作业", refDate)
        assertNotNull(result)
        val affair = result!!.affairs.first()
        assertEquals("2026-09-29", affair.date)
        assertEquals("20:00", affair.time)
    }

    @Test
    fun testParseShiftOverride() {
        val result = FastLocalParser.tryParse("10月4日（周日）按原周五课表行课", refDate)
        assertNotNull(result)
        assertEquals("SHIFT_OVERRIDE", result!!.primaryIntent)
        val shift = result.shiftOverride
        assertNotNull(shift)
        assertEquals("SHIFT", shift!!.kind)
        assertEquals("2026-10-04", shift.targetDate)
        assertEquals(5, shift.followsWeekday)
    }

    @Test
    fun testParseCancelClass() {
        val result = FastLocalParser.tryParse("明天停课一天", refDate)
        assertNotNull(result)
        assertEquals("SHIFT_OVERRIDE", result!!.primaryIntent)
        val shift = result.shiftOverride
        assertNotNull(shift)
        assertEquals("CANCEL", shift!!.kind)
        assertEquals("2026-09-30", shift.targetDate)
    }

    @Test
    fun testParseUniversityShiftAdjustment() {
        val text = "9月30日（周三）的课程调整至9月27日（周日）"
        val result = FastLocalParser.tryParse(text, refDate)
        assertNotNull(result)
        assertEquals("SHIFT_OVERRIDE", result!!.primaryIntent)
        val shift = result.shiftOverride
        assertNotNull(shift)
        assertEquals("SHIFT", shift!!.kind)
        // 9月27日 is the execution day
        assertEquals("2026-09-27", shift.targetDate)
        // follows Wednesday (3)
        assertEquals(3, shift.followsWeekday)
        assertEquals(2, result.shiftOverrides.size)
        // 目标日 (9月27日) 执行周三课表
        assertEquals("2026-09-27", result.shiftOverrides[0].targetDate)
        assertEquals("SHIFT", result.shiftOverrides[0].kind)
        assertEquals(3, result.shiftOverrides[0].followsWeekday)
        // 原调离日 (9月30日) 停课避让
        assertEquals("2026-09-30", result.shiftOverrides[1].targetDate)
        assertEquals("CANCEL", result.shiftOverrides[1].kind)
    }

    @Test
    fun testFallbackOnComplexTable() {
        val tableText = "| 星期一 | 星期二 | 星期三 |\n| 高等数学 | 大学英语 | 线性代数 |"
        val result = FastLocalParser.tryParse(tableText, refDate)
        assertNull(result) // Fallback to LLM
    }

    @Test
    fun testComplexNoticeWithLinkAndSurveyFallsBackToLlm() {
        val notice = """
            @所有人 请各位老师，本周周六20:00之前完成参培统计表，统计时间区间为：2025年9月1日-2026年8月31日。
            【金山文档 | WPS云文档】 对内开展培训明细统计调查表20261015
            https://www.kdocs.cn/l/cirH1TTFchqg
        """.trimIndent()
        // Today is Thursday 2026-10-15
        val today = LocalDate.of(2026, 10, 15)
        val result = FastLocalParser.tryParse(notice, today)
        // Complex notice with external link, WPS doc and statistical range MUST fall back to LLM
        assertNull("Complex notice with URL/survey must fall back to LLM", result)
    }

    @Test
    fun testLocalParserCleansAtEveryonePrefix() {
        val text = "@所有人 请各位老师，周五上午10点去教研室交大纲"
        val result = FastLocalParser.tryParse(text, refDate)
        assertNotNull(result)
        val affair = result!!.affairs.first()
        assertEquals("2026-10-02", affair.date)
        assertEquals("10:00", affair.time)
        assertEquals("教研室", affair.place)
        // Title MUST NOT be "@所有人" or "@所有人请各位老师"
        org.junit.Assert.assertFalse("Title must not contain @所有人", affair.title.contains("@所有人"))
        org.junit.Assert.assertFalse("Title must not contain 各位老师", affair.title.contains("各位老师"))
        org.junit.Assert.assertTrue("Title should contain core action", affair.title.contains("交大纲"))
    }
}
