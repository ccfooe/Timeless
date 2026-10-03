package com.classbell.app.core

import com.classbell.app.data.model.Affair
import com.classbell.app.data.model.AffairCategory
import com.classbell.app.data.model.ReminderChoice
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class CalendarSyncManagerTest {

    @Test
    fun testFormatAffairEventTitle_whenNotDone() {
        val title = "提交部门管理规范"
        val formatted = CalendarSyncManager.formatAffairEventTitle(title, isDone = false)
        assertEquals("提交部门管理规范", formatted)
    }

    @Test
    fun testFormatAffairEventTitle_whenDone() {
        val title = "提交部门管理规范"
        val formatted = CalendarSyncManager.formatAffairEventTitle(title, isDone = true)
        assertEquals("[已完成] 提交部门管理规范", formatted)
    }

    @Test
    fun testShouldHaveAlarm_whenNotDone() {
        val affairWithReminder = Affair(
            id = 1L,
            title = "英语四级报名",
            category = AffairCategory.AFFAIR,
            date = LocalDate.of(2026, 10, 15),
            time = LocalTime.of(10, 0),
            reminder = ReminderChoice.MIN10,
            isDone = false
        )
        assertTrue(CalendarSyncManager.shouldHaveAlarm(affairWithReminder))

        val affairNoReminder = affairWithReminder.copy(reminder = ReminderChoice.NONE)
        assertFalse(CalendarSyncManager.shouldHaveAlarm(affairNoReminder))
    }

    @Test
    fun testShouldHaveAlarm_whenDone_suppressesAlarm() {
        val completedAffair = Affair(
            id = 2L,
            title = "实验课报告提交",
            category = AffairCategory.AFFAIR,
            date = LocalDate.of(2026, 10, 15),
            time = LocalTime.of(14, 0),
            reminder = ReminderChoice.HOUR1,
            isDone = true
        )
        // Even though reminder is HOUR1, since isDone is true, alarm must be suppressed
        assertFalse(CalendarSyncManager.shouldHaveAlarm(completedAffair))
    }

    @Test
    fun testBuildAffairDescription() {
        val affair = Affair(
            id = 3L,
            title = "班委会讨论",
            category = AffairCategory.MEETING,
            date = LocalDate.of(2026, 10, 16),
            time = LocalTime.of(19, 0),
            place = "主楼302",
            note = "讨论换届选举",
            reminder = ReminderChoice.AT_TIME,
            isDone = true
        )
        val desc = CalendarSyncManager.buildAffairDescription(affair)
        assertTrue(desc.contains("讨论换届选举"))
        assertTrue(desc.contains("类别: MEETING"))
        assertTrue(desc.contains("状态: 已完成"))
        assertTrue(desc.contains("来源: Timeless 日程事项"))

        val incompleteAffair = affair.copy(isDone = false)
        val incompleteDesc = CalendarSyncManager.buildAffairDescription(incompleteAffair)
        assertFalse(incompleteDesc.contains("状态: 已完成"))
    }

    @Test
    fun testGetReminderMinutes() {
        assertEquals(0, CalendarSyncManager.getReminderMinutes(ReminderChoice.AT_TIME))
        assertEquals(10, CalendarSyncManager.getReminderMinutes(ReminderChoice.MIN10))
        assertEquals(60, CalendarSyncManager.getReminderMinutes(ReminderChoice.HOUR1))
        assertEquals(1440, CalendarSyncManager.getReminderMinutes(ReminderChoice.DAY1))
    }
}
