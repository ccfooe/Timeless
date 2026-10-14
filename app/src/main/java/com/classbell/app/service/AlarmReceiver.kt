package com.classbell.app.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.classbell.app.ClassBellApp
import com.classbell.app.MainActivity
import com.classbell.app.R

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        when (intent.action) {
            AlarmScheduler.ACTION_CLASS_START -> {
                val course = intent.getStringExtra(AlarmScheduler.EXTRA_COURSE_NAME) ?: ""
                val className = intent.getStringExtra(AlarmScheduler.EXTRA_CLASS_NAME) ?: ""
                val teacher = intent.getStringExtra(AlarmScheduler.EXTRA_TEACHER)
                val room = intent.getStringExtra(AlarmScheduler.EXTRA_ROOM) ?: ""
                val startTime = intent.getStringExtra(AlarmScheduler.EXTRA_START_TIME) ?: ""
                val endTime = intent.getStringExtra(AlarmScheduler.EXTRA_END_TIME) ?: ""
                val periodLabel = intent.getStringExtra(AlarmScheduler.EXTRA_PERIOD_LABEL) ?: ""
                val periodStart = intent.getIntExtra(AlarmScheduler.EXTRA_PERIOD_START, 1)
                val periodEnd = intent.getIntExtra(AlarmScheduler.EXTRA_PERIOD_END, 1)

                val displayClass = teacher?.takeIf { it.isNotBlank() } ?: className
                InClassService.start(context, course, displayClass, room, startTime, endTime, periodStart, periodEnd)

                val pendingIntent = PendingIntent.getActivity(
                    context,
                    0,
                    Intent(context, MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val title = if (periodLabel.isNotBlank()) "🔔 $periodLabel 上课时间到 · $course" else "🔔 上课时间到 · $course"
                val teacherOrClass = if (!teacher.isNullOrBlank()) "教师: $teacher" else "授课班级: $className"
                val body = "地点: $room · $teacherOrClass"

                val notification = NotificationCompat.Builder(context, ClassBellApp.CHANNEL_WARNING_ID)
                    .setSmallIcon(R.drawable.ic_stat_in_class)
                    .setContentTitle(title)
                    .setContentText(body)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setDefaults(NotificationCompat.DEFAULT_ALL)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent)
                    .build()

                notificationManager.notify(2000, notification)
            }

            AlarmScheduler.ACTION_PERIOD_TRANSITION -> {
                val course = intent.getStringExtra(AlarmScheduler.EXTRA_COURSE_NAME) ?: ""
                val className = intent.getStringExtra(AlarmScheduler.EXTRA_CLASS_NAME) ?: ""
                val teacher = intent.getStringExtra(AlarmScheduler.EXTRA_TEACHER)
                val room = intent.getStringExtra(AlarmScheduler.EXTRA_ROOM) ?: ""
                val startTime = intent.getStringExtra(AlarmScheduler.EXTRA_START_TIME) ?: ""
                val endTime = intent.getStringExtra(AlarmScheduler.EXTRA_END_TIME) ?: ""
                val periodLabel = intent.getStringExtra(AlarmScheduler.EXTRA_PERIOD_LABEL) ?: ""
                val breakTitle = intent.getStringExtra(AlarmScheduler.EXTRA_BREAK_TITLE) ?: "课间休息"
                val nextPeriodLabel = intent.getStringExtra(AlarmScheduler.EXTRA_NEXT_PERIOD_LABEL) ?: ""
                val periodStart = intent.getIntExtra(AlarmScheduler.EXTRA_PERIOD_START, 1)
                val periodEnd = intent.getIntExtra(AlarmScheduler.EXTRA_PERIOD_END, 1)

                val displayClass = teacher?.takeIf { it.isNotBlank() } ?: className
                InClassService.start(context, course, displayClass, room, startTime, endTime, periodStart, periodEnd)

                val pendingIntent = PendingIntent.getActivity(
                    context,
                    0,
                    Intent(context, MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val notification = NotificationCompat.Builder(context, ClassBellApp.CHANNEL_WARNING_ID)
                    .setSmallIcon(R.drawable.ic_stat_in_class)
                    .setContentTitle("☕ $periodLabel 已下课 · 进入$breakTitle")
                    .setContentText("下节课 $nextPeriodLabel 即将开始，适当休息放松一下吧")
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setDefaults(NotificationCompat.DEFAULT_ALL)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent)
                    .build()

                notificationManager.notify(2003, notification)
            }

            AlarmScheduler.ACTION_CLASS_WARN -> {
                val course = intent.getStringExtra(AlarmScheduler.EXTRA_COURSE_NAME) ?: ""
                val endTime = intent.getStringExtra(AlarmScheduler.EXTRA_END_TIME) ?: ""
                val warnMinutes = intent.getIntExtra(AlarmScheduler.EXTRA_WARN_MINUTES, 5)
                val periodLabel = intent.getStringExtra(AlarmScheduler.EXTRA_PERIOD_LABEL) ?: ""

                val pendingIntent = PendingIntent.getActivity(
                    context,
                    0,
                    Intent(context, MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val title = if (periodLabel.isNotBlank()) "⏰ $periodLabel 下课提醒 · $course" else "⏰ 下课提醒 · $course"

                val notification = NotificationCompat.Builder(context, ClassBellApp.CHANNEL_WARNING_ID)
                    .setSmallIcon(R.drawable.ic_stat_in_class)
                    .setContentTitle(title)
                    .setContentText("还有 $warnMinutes 分钟下课（预计 $endTime 下课）")
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setDefaults(NotificationCompat.DEFAULT_ALL)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent)
                    .build()

                notificationManager.notify(2001, notification)
            }

            AlarmScheduler.ACTION_CLASS_END -> {
                val course = intent.getStringExtra(AlarmScheduler.EXTRA_COURSE_NAME) ?: ""
                InClassService.stop(context)

                val pendingIntent = PendingIntent.getActivity(
                    context,
                    0,
                    Intent(context, MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val notification = NotificationCompat.Builder(context, ClassBellApp.CHANNEL_WARNING_ID)
                    .setSmallIcon(R.drawable.ic_stat_in_class)
                    .setContentTitle("🎉 课程已全部下课")
                    .setContentText("$course 今日课程已全部下课，辛苦了！")
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent)
                    .build()

                notificationManager.notify(2002, notification)
            }

            AlarmScheduler.ACTION_AFFAIR -> {
                val title = intent.getStringExtra(AlarmScheduler.EXTRA_AFFAIR_TITLE) ?: "校务日程提醒"
                val place = intent.getStringExtra(AlarmScheduler.EXTRA_AFFAIR_PLACE)
                val time = intent.getStringExtra(AlarmScheduler.EXTRA_AFFAIR_TIME) ?: ""

                val body = buildString {
                    append("时间: $time")
                    if (!place.isNullOrBlank()) {
                        append(" · 地点: $place")
                    }
                }

                val pendingIntent = PendingIntent.getActivity(
                    context,
                    0,
                    Intent(context, MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val notification = NotificationCompat.Builder(context, ClassBellApp.CHANNEL_AFFAIRS_ID)
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setContentTitle(title)
                    .setContentText(body)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent)
                    .build()

                val affairId = intent.getLongExtra(AlarmScheduler.EXTRA_AFFAIR_ID, 0L)
                val notificationId = if (affairId > 0) affairId.toInt() + 100000 else (System.currentTimeMillis() % 100000).toInt()
                notificationManager.notify(notificationId, notification)
            }
        }
    }
}
