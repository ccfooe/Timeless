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
}
