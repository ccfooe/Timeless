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
                val room = intent.getStringExtra(AlarmScheduler.EXTRA_ROOM) ?: ""
                val startTime = intent.getStringExtra(AlarmScheduler.EXTRA_START_TIME) ?: ""
                val endTime = intent.getStringExtra(AlarmScheduler.EXTRA_END_TIME) ?: ""

                InClassService.start(context, course, className, room, startTime, endTime)
            }

            AlarmScheduler.ACTION_CLASS_WARN -> {
                val course = intent.getStringExtra(AlarmScheduler.EXTRA_COURSE_NAME) ?: ""
                val endTime = intent.getStringExtra(AlarmScheduler.EXTRA_END_TIME) ?: ""
                val warnMinutes = intent.getIntExtra(AlarmScheduler.EXTRA_WARN_MINUTES, 5)

                val pendingIntent = PendingIntent.getActivity(
                    context,
                    0,
                    Intent(context, MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val notification = NotificationCompat.Builder(context, ClassBellApp.CHANNEL_WARNING_ID)
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setContentTitle("下课倒计时提醒 · $course")
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
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setContentTitle("下课时间到")
                    .setContentText("$course 课程已下课，辛苦了！")
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

                notificationManager.notify((System.currentTimeMillis() % 100000).toInt(), notification)
            }
        }
    }
}
