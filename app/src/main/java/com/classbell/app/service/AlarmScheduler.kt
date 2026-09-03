package com.classbell.app.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.classbell.app.data.model.Affair
import com.classbell.app.data.model.MeetingOccurrence
import com.classbell.app.data.model.ReminderChoice
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class AlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    companion object {
        const val ACTION_CLASS_START = "com.classbell.app.ACTION_CLASS_START"
        const val ACTION_CLASS_WARN = "com.classbell.app.ACTION_CLASS_WARN"
        const val ACTION_CLASS_END = "com.classbell.app.ACTION_CLASS_END"
        const val ACTION_AFFAIR = "com.classbell.app.ACTION_AFFAIR"

        const val EXTRA_COURSE_NAME = "extra_course_name"
        const val EXTRA_CLASS_NAME = "extra_class_name"
        const val EXTRA_ROOM = "extra_room"
        const val EXTRA_END_TIME = "extra_end_time"
        const val EXTRA_START_TIME = "extra_start_time"
        const val EXTRA_WARN_MINUTES = "extra_warn_minutes"
        const val EXTRA_AFFAIR_TITLE = "extra_affair_title"
        const val EXTRA_AFFAIR_PLACE = "extra_affair_place"
        const val EXTRA_AFFAIR_TIME = "extra_affair_time"
    }

    fun scheduleClassAlarms(meeting: MeetingOccurrence, warnMinutes: Int = 5) {
        if (meeting.isHolidaySuppressed) return

        val zone = ZoneId.systemDefault()
        val now = LocalDateTime.now()

        val startDateTime = LocalDateTime.of(meeting.date, meeting.startTime)
        val endDateTime = LocalDateTime.of(meeting.date, meeting.endTime)
        val warnDateTime = endDateTime.minusMinutes(warnMinutes.toLong())

        val courseKey = meeting.course.id.toInt() * 10

        // 1. Class Start Alarm
        if (startDateTime.isAfter(now)) {
            val intent = Intent(context, AlarmReceiver::class.java).apply {
                action = ACTION_CLASS_START
                putExtra(EXTRA_COURSE_NAME, meeting.course.name)
                putExtra(EXTRA_CLASS_NAME, meeting.course.className)
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

        // 2. Class End Warning Alarm (e.g. 5 minutes before end)
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

        // 3. Class End Alarm
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

    fun scheduleAffairAlarm(affair: Affair) {
        if (affair.reminder == ReminderChoice.NONE) return

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
