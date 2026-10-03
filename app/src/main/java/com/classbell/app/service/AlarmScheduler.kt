package com.classbell.app.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.classbell.app.data.model.Affair
import com.classbell.app.data.model.MeetingOccurrence
import com.classbell.app.data.model.PeriodSlot
import com.classbell.app.data.model.ReminderChoice
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class AlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    companion object {
        const val ACTION_CLASS_START = "com.timeless.app.ACTION_CLASS_START"
        const val ACTION_CLASS_WARN = "com.timeless.app.ACTION_CLASS_WARN"
        const val ACTION_CLASS_END = "com.timeless.app.ACTION_CLASS_END"
        const val ACTION_PERIOD_TRANSITION = "com.timeless.app.ACTION_PERIOD_TRANSITION"
        const val ACTION_AFFAIR = "com.timeless.app.ACTION_AFFAIR"

        const val EXTRA_COURSE_NAME = "extra_course_name"
        const val EXTRA_CLASS_NAME = "extra_class_name"
        const val EXTRA_TEACHER = "extra_teacher"
        const val EXTRA_ROOM = "extra_room"
        const val EXTRA_END_TIME = "extra_end_time"
        const val EXTRA_START_TIME = "extra_start_time"
        const val EXTRA_WARN_MINUTES = "extra_warn_minutes"
        const val EXTRA_PERIOD_ORDER = "extra_period_order"
        const val EXTRA_PERIOD_LABEL = "extra_period_label"
        const val EXTRA_PERIOD_START = "extra_period_start"
        const val EXTRA_PERIOD_END = "extra_period_end"
        const val EXTRA_IS_BREAK = "extra_is_break"
        const val EXTRA_BREAK_TITLE = "extra_break_title"
        const val EXTRA_NEXT_PERIOD_LABEL = "extra_next_period_label"
        const val EXTRA_AFFAIR_TITLE = "extra_affair_title"
        const val EXTRA_AFFAIR_PLACE = "extra_affair_place"
        const val EXTRA_AFFAIR_TIME = "extra_affair_time"
        const val EXTRA_IS_MANUAL_SESSION = "extra_is_manual_session"
    }

    fun scheduleClassAlarms(
        meeting: MeetingOccurrence,
        warnMinutes: Int = 5,
        periodSlots: List<PeriodSlot> = emptyList()
    ) {
        if (meeting.isHolidaySuppressed) return

        val zone = ZoneId.systemDefault()
        val now = LocalDateTime.now()
        val courseKey = meeting.course.id.toInt() * 100

        val periodSlotMap = periodSlots.associateBy { it.order }
        val courseSlots = (meeting.course.periodStart..meeting.course.periodEnd).mapNotNull { periodSlotMap[it] }

        if (courseSlots.isNotEmpty()) {
            // Granular sub-period scheduling for each individual period and break
            for (i in courseSlots.indices) {
                val slot = courseSlots[i]
                val slotStart = LocalDateTime.of(meeting.date, slot.startTime)
                val slotEnd = LocalDateTime.of(meeting.date, slot.endTime)
                val slotWarn = slotEnd.minusMinutes(warnMinutes.toLong())

                // 1. Period Start Alarm (at slot.startTime)
                if (slotStart.isAfter(now)) {
                    val intent = Intent(context, AlarmReceiver::class.java).apply {
                        action = ACTION_CLASS_START
                        putExtra(EXTRA_COURSE_NAME, meeting.course.name)
                        putExtra(EXTRA_CLASS_NAME, meeting.course.className)
                        putExtra(EXTRA_TEACHER, meeting.course.teacher)
                        putExtra(EXTRA_ROOM, meeting.course.room)
                        putExtra(EXTRA_START_TIME, slot.startTime.toString())
                        putExtra(EXTRA_END_TIME, slot.endTime.toString())
                        putExtra(EXTRA_PERIOD_ORDER, slot.order)
                        putExtra(EXTRA_PERIOD_LABEL, slot.label)
                        putExtra(EXTRA_PERIOD_START, meeting.course.periodStart)
                        putExtra(EXTRA_PERIOD_END, meeting.course.periodEnd)
                    }
                    val pendingIntent = PendingIntent.getBroadcast(
                        context,
                        courseKey + i * 4 + 1,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    setExactAlarm(slotStart.atZone(zone).toInstant().toEpochMilli(), pendingIntent)
                }

                // 2. Period Ending Warning Alarm (e.g. 5 min before slot.endTime)
                if (slotWarn.isAfter(now)) {
                    val intent = Intent(context, AlarmReceiver::class.java).apply {
                        action = ACTION_CLASS_WARN
                        putExtra(EXTRA_COURSE_NAME, meeting.course.name)
                        putExtra(EXTRA_CLASS_NAME, meeting.course.className)
                        putExtra(EXTRA_ROOM, meeting.course.room)
                        putExtra(EXTRA_END_TIME, slot.endTime.toString())
                        putExtra(EXTRA_WARN_MINUTES, warnMinutes)
                        putExtra(EXTRA_PERIOD_LABEL, slot.label)
                    }
                    val pendingIntent = PendingIntent.getBroadcast(
                        context,
                        courseKey + i * 4 + 2,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    setExactAlarm(slotWarn.atZone(zone).toInstant().toEpochMilli(), pendingIntent)
                }

                // 3. Period End / Break Transition Alarm (at slot.endTime)
                if (slotEnd.isAfter(now)) {
                    if (i < courseSlots.size - 1) {
                        // Intermediate period end: Transition to Recess / Break
                        val nextSlot = courseSlots[i + 1]
                        val breakDuration = Duration.between(slot.endTime, nextSlot.startTime).toMinutes()
                        val breakTitle = if (breakDuration >= 15) "大课间休息" else "课间休息"

                        val intent = Intent(context, AlarmReceiver::class.java).apply {
                            action = ACTION_PERIOD_TRANSITION
                            putExtra(EXTRA_COURSE_NAME, meeting.course.name)
                            putExtra(EXTRA_CLASS_NAME, meeting.course.className)
                            putExtra(EXTRA_TEACHER, meeting.course.teacher)
                            putExtra(EXTRA_ROOM, meeting.course.room)
                            putExtra(EXTRA_PERIOD_ORDER, slot.order)
                            putExtra(EXTRA_PERIOD_LABEL, slot.label)
                            putExtra(EXTRA_IS_BREAK, true)
                            putExtra(EXTRA_BREAK_TITLE, breakTitle)
                            putExtra(EXTRA_NEXT_PERIOD_LABEL, nextSlot.label)
                            putExtra(EXTRA_START_TIME, slot.endTime.toString())
                            putExtra(EXTRA_END_TIME, nextSlot.startTime.toString())
                            putExtra(EXTRA_PERIOD_START, meeting.course.periodStart)
                            putExtra(EXTRA_PERIOD_END, meeting.course.periodEnd)
                        }
                        val pendingIntent = PendingIntent.getBroadcast(
                            context,
                            courseKey + i * 4 + 3,
                            intent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        setExactAlarm(slotEnd.atZone(zone).toInstant().toEpochMilli(), pendingIntent)
                    } else {
                        // Final period end: Whole course session ends
                        val intent = Intent(context, AlarmReceiver::class.java).apply {
                            action = ACTION_CLASS_END
                            putExtra(EXTRA_COURSE_NAME, meeting.course.name)
                            putExtra(EXTRA_PERIOD_LABEL, slot.label)
                        }
                        val pendingIntent = PendingIntent.getBroadcast(
                            context,
                            courseKey + i * 4 + 4,
                            intent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        setExactAlarm(slotEnd.atZone(zone).toInstant().toEpochMilli(), pendingIntent)
                    }
                }
            }
        } else {
            // Fallback for custom or unspecified slots
            val startDateTime = LocalDateTime.of(meeting.date, meeting.startTime)
            val endDateTime = LocalDateTime.of(meeting.date, meeting.endTime)
            val warnDateTime = endDateTime.minusMinutes(warnMinutes.toLong())

            if (startDateTime.isAfter(now)) {
                val intent = Intent(context, AlarmReceiver::class.java).apply {
                    action = ACTION_CLASS_START
                    putExtra(EXTRA_COURSE_NAME, meeting.course.name)
                    putExtra(EXTRA_CLASS_NAME, meeting.course.className)
                    putExtra(EXTRA_TEACHER, meeting.course.teacher)
                    putExtra(EXTRA_ROOM, meeting.course.room)
                    putExtra(EXTRA_START_TIME, meeting.startTime.toString())
                    putExtra(EXTRA_END_TIME, meeting.endTime.toString())
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    courseKey + 1,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                setExactAlarm(startDateTime.atZone(zone).toInstant().toEpochMilli(), pendingIntent)
            }

            if (warnDateTime.isAfter(now)) {
                val intent = Intent(context, AlarmReceiver::class.java).apply {
                    action = ACTION_CLASS_WARN
                    putExtra(EXTRA_COURSE_NAME, meeting.course.name)
                    putExtra(EXTRA_CLASS_NAME, meeting.course.className)
                    putExtra(EXTRA_ROOM, meeting.course.room)
                    putExtra(EXTRA_END_TIME, meeting.endTime.toString())
                    putExtra(EXTRA_WARN_MINUTES, warnMinutes)
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    courseKey + 2,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                setExactAlarm(warnDateTime.atZone(zone).toInstant().toEpochMilli(), pendingIntent)
            }

            if (endDateTime.isAfter(now)) {
                val intent = Intent(context, AlarmReceiver::class.java).apply {
                    action = ACTION_CLASS_END
                    putExtra(EXTRA_COURSE_NAME, meeting.course.name)
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    courseKey + 3,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                setExactAlarm(endDateTime.atZone(zone).toInstant().toEpochMilli(), pendingIntent)
            }
        }
    }

    fun cancelAffairAlarm(affairId: Long) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_AFFAIR
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            affairId.toInt() + 100000,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    fun scheduleAffairAlarm(affair: Affair) {
        if (affair.isDone || affair.reminder == ReminderChoice.NONE) {
            cancelAffairAlarm(affair.id)
            return
        }

        val zone = ZoneId.systemDefault()
        val eventTime = affair.time ?: LocalTime.of(9, 0)
        val eventDateTime = LocalDateTime.of(affair.date, eventTime)

        val alarmDateTime = when (affair.reminder) {
            ReminderChoice.NONE -> return
            ReminderChoice.AT_TIME -> eventDateTime
            ReminderChoice.MIN10 -> eventDateTime.minusMinutes(10)
            ReminderChoice.HOUR1 -> eventDateTime.minusHours(1)
            ReminderChoice.DAY1 -> eventDateTime.minusDays(1)
        }

        if (alarmDateTime.isAfter(LocalDateTime.now())) {
            val intent = Intent(context, AlarmReceiver::class.java).apply {
                action = ACTION_AFFAIR
                putExtra(EXTRA_AFFAIR_TITLE, affair.title)
                putExtra(EXTRA_AFFAIR_PLACE, affair.place)
                putExtra(EXTRA_AFFAIR_TIME, eventTime.toString())
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                affair.id.toInt() + 100000,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            setExactAlarm(alarmDateTime.atZone(zone).toInstant().toEpochMilli(), pendingIntent)
        } else {
            cancelAffairAlarm(affair.id)
        }
    }

    private fun setExactAlarm(triggerAtMillis: Long, pendingIntent: PendingIntent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }
    }
}
