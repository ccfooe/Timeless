package com.classbell.app.core

import com.classbell.app.data.model.Course
import com.classbell.app.data.model.PeriodSlot
import com.classbell.app.data.model.SubPeriodKind
import com.classbell.app.data.model.WeekPattern
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

class OccurrenceEngineSubPeriodTest {

    private val periodSlots = listOf(
        PeriodSlot(1, 1, "第1节", LocalTime.of(8, 30), LocalTime.of(9, 15)),
        PeriodSlot(2, 2, "第2节", LocalTime.of(9, 20), LocalTime.of(10, 5)),
        PeriodSlot(3, 3, "第3节", LocalTime.of(10, 25), LocalTime.of(11, 10)),
        PeriodSlot(4, 4, "第4节", LocalTime.of(11, 15), LocalTime.of(12, 0)),
        PeriodSlot(5, 5, "第5节", LocalTime.of(14, 30), LocalTime.of(15, 15)),
        PeriodSlot(6, 6, "第6节", LocalTime.of(15, 25), LocalTime.of(16, 10)),
        PeriodSlot(7, 7, "第7节", LocalTime.of(16, 20), LocalTime.of(17, 5)),
        PeriodSlot(8, 8, "第8节", LocalTime.of(17, 15), LocalTime.of(18, 0))
    )

    private val morningCourse = Course(
        id = 1L,
        semesterId = 1L,
        name = "影视声音创作★",
        code = "B090118",
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

    @Test
    fun testSubPeriod_InClass_Period1() {
        // At 08:45, it should be in class during period 1 (08:30 - 09:15)
        val state = OccurrenceEngine.resolveSubPeriodState(morningCourse, periodSlots, LocalTime.of(8, 45))
        assertNotNull(state)
        assertEquals(SubPeriodKind.IN_CLASS, state!!.kind)
        assertEquals(1, state.periodOrder)
        assertEquals(1, state.periodIndexInCourse)
        assertEquals(4, state.totalPeriodsInCourse)
        assertEquals("第1节", state.periodLabel)
        assertEquals(LocalTime.of(8, 30), state.startTime)
        assertEquals(LocalTime.of(9, 15), state.endTime)
        assertFalse(state.isBreak)
        assertEquals(30L, state.remainingMinutes) // 9:15 - 8:45 = 30 min
    }

    @Test
    fun testSubPeriod_NormalBreak_BetweenPeriod1And2() {
        // At 09:16, period 1 has finished at 09:15, period 2 starts at 09:20. It is normal recess (5 min)
        val state = OccurrenceEngine.resolveSubPeriodState(morningCourse, periodSlots, LocalTime.of(9, 16))
        assertNotNull(state)
        assertEquals(SubPeriodKind.BREAK, state!!.kind)
        assertTrue(state.isBreak)
        assertEquals("第2节", state.nextPeriodLabel)
        assertEquals("课间休息", state.breakTitle)
        assertEquals(LocalTime.of(9, 15), state.startTime)
        assertEquals(LocalTime.of(9, 20), state.endTime)
        assertEquals(4L, state.remainingMinutes) // 9:20 - 9:16 = 4 min
    }

    @Test
    fun testSubPeriod_BigBreak_BetweenPeriod2And3() {
        // Period 2 ends at 10:05, period 3 starts at 10:25 (20 min gap -> 大课间休息)
        val state = OccurrenceEngine.resolveSubPeriodState(morningCourse, periodSlots, LocalTime.of(10, 10))
        assertNotNull(state)
        assertEquals(SubPeriodKind.BREAK, state!!.kind)
        assertTrue(state.isBreak)
        assertEquals("第3节", state.nextPeriodLabel)
        assertEquals("大课间休息", state.breakTitle)
        assertEquals(LocalTime.of(10, 5), state.startTime)
        assertEquals(LocalTime.of(10, 25), state.endTime)
        assertEquals(15L, state.remainingMinutes) // 10:25 - 10:10 = 15 min
    }

    @Test
    fun testSubPeriod_Finished_AfterLastPeriod() {
        // Period 4 ends at 12:00. At 12:05, course should be finished.
        val state = OccurrenceEngine.resolveSubPeriodState(morningCourse, periodSlots, LocalTime.of(12, 5))
        assertNotNull(state)
        assertEquals(SubPeriodKind.FINISHED, state!!.kind)
        assertEquals(0L, state.remainingMinutes)
        assertEquals(1f, state.progress, 0.001f)
    }

    @Test
    fun testSubPeriod_PreClass_BeforeFirstPeriod() {
        // At 08:15 (15 min before 08:30 start), within 30 min preparation window
        val state = OccurrenceEngine.resolveSubPeriodState(morningCourse, periodSlots, LocalTime.of(8, 15))
        assertNotNull(state)
        assertEquals(SubPeriodKind.PRE_CLASS, state!!.kind)
        assertEquals(1, state.periodOrder)
        assertEquals(15L, state.remainingMinutes)
    }
}
