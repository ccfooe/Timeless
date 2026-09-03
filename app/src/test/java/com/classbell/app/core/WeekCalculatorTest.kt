package com.classbell.app.core

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class WeekCalculatorTest {

    private val week1Monday = LocalDate.of(2026, 8, 24)

    @Test
    fun testWeekNumberCalculation() {
        // Week 1: 2026-08-24 to 2026-08-30
        assertEquals(1, WeekCalculator.calculateWeekNumber(LocalDate.of(2026, 8, 24), week1Monday))
        assertEquals(1, WeekCalculator.calculateWeekNumber(LocalDate.of(2026, 8, 30), week1Monday))

        // Week 2: 2026-08-31 (first day of teaching)
        assertEquals(2, WeekCalculator.calculateWeekNumber(LocalDate.of(2026, 8, 31), week1Monday))
        assertEquals(2, WeekCalculator.calculateWeekNumber(LocalDate.of(2026, 9, 6), week1Monday))

        // Week 4: 2026-09-16 (Wednesday)
        assertEquals(4, WeekCalculator.calculateWeekNumber(LocalDate.of(2026, 9, 16), week1Monday))

        // Week 6: 2026-10-01 (National Day)
        assertEquals(6, WeekCalculator.calculateWeekNumber(LocalDate.of(2026, 10, 1), week1Monday))

        // Week 19: 2026-12-28 (Exam week)
        assertEquals(19, WeekCalculator.calculateWeekNumber(LocalDate.of(2026, 12, 28), week1Monday))

        // Week 21: 2027-01-11 (Practice week)
        assertEquals(21, WeekCalculator.calculateWeekNumber(LocalDate.of(2027, 1, 11), week1Monday))

        // Clamping: Dates prior to week1Monday should clamp to 1
        assertEquals(1, WeekCalculator.calculateWeekNumber(LocalDate.of(2026, 8, 1), week1Monday))
    }

    @Test
    fun testSemesterPhase() {
        // Week 1: 报到周
        assertEquals("报到周", WeekCalculator.getSemesterPhase(1))

        // Weeks 2 - 18: 教学周
        assertEquals("教学周", WeekCalculator.getSemesterPhase(2))
        assertEquals("教学周", WeekCalculator.getSemesterPhase(10))
        assertEquals("教学周", WeekCalculator.getSemesterPhase(18))

        // Weeks 19 - 20: 考试周
        assertEquals("考试周", WeekCalculator.getSemesterPhase(19))
        assertEquals("考试周", WeekCalculator.getSemesterPhase(20))

        // Week 21: 实践周
        assertEquals("实践周", WeekCalculator.getSemesterPhase(21))

        // Week 22+: 假期
        assertEquals("假期", WeekCalculator.getSemesterPhase(22))
    }

    @Test
    fun testWeekDateRange() {
        val (mon1, sun1) = WeekCalculator.getWeekDateRange(1, week1Monday)
        assertEquals(LocalDate.of(2026, 8, 24), mon1)
        assertEquals(LocalDate.of(2026, 8, 30), sun1)

        val (mon2, sun2) = WeekCalculator.getWeekDateRange(2, week1Monday)
        assertEquals(LocalDate.of(2026, 8, 31), mon2)
        assertEquals(LocalDate.of(2026, 9, 6), sun2)
    }
}
