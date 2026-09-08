package com.classbell.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.classbell.app.ClassBellApp
import com.classbell.app.MainActivity
import com.classbell.app.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalTime
import java.time.temporal.ChronoUnit

class InClassService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var tickerJob: Job? = null

    private var courseName: String = ""
    private var className: String = ""
    private var room: String = ""
    private var startTime: LocalTime = LocalTime.of(8, 30)
    private var endTime: LocalTime = LocalTime.of(12, 0)

    companion object {
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = ClassBellApp.CHANNEL_IN_CLASS_ID

        const val ACTION_START_IN_CLASS = "com.classbell.app.START_IN_CLASS"
        const val ACTION_STOP_IN_CLASS = "com.classbell.app.STOP_IN_CLASS"
        const val ACTION_EXTEND_IN_CLASS = "com.classbell.app.EXTEND_IN_CLASS"

        fun start(
            context: Context,
            courseName: String,
            className: String,
            room: String,
            startTime: String,
            endTime: String
        ) {
            val intent = Intent(context, InClassService::class.java).apply {
                action = ACTION_START_IN_CLASS
                putExtra(AlarmScheduler.EXTRA_COURSE_NAME, courseName)
                putExtra(AlarmScheduler.EXTRA_CLASS_NAME, className)
                putExtra(AlarmScheduler.EXTRA_ROOM, room)
                putExtra(AlarmScheduler.EXTRA_START_TIME, startTime)
                putExtra(AlarmScheduler.EXTRA_END_TIME, endTime)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, InClassService::class.java).apply {
                action = ACTION_STOP_IN_CLASS
            }
            context.startService(intent)
        }

        fun extend(context: Context, extraMinutes: Int = 5) {
            val intent = Intent(context, InClassService::class.java).apply {
                action = ACTION_EXTEND_IN_CLASS
                putExtra("extra_extend_minutes", extraMinutes)
            }
            context.startService(intent)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_IN_CLASS -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_EXTEND_IN_CLASS -> {
                val mins = intent.getIntExtra("extra_extend_minutes", 5)
                endTime = endTime.plusMinutes(mins.toLong())
                val notification = buildProgressNotification()
                val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                manager.notify(NOTIFICATION_ID, notification)
                return START_STICKY
            }
            ACTION_START_IN_CLASS -> {
                courseName = intent.getStringExtra(AlarmScheduler.EXTRA_COURSE_NAME) ?: ""
                className = intent.getStringExtra(AlarmScheduler.EXTRA_CLASS_NAME) ?: ""
                room = intent.getStringExtra(AlarmScheduler.EXTRA_ROOM) ?: ""
                intent.getStringExtra(AlarmScheduler.EXTRA_START_TIME)?.let {
                    runCatching { startTime = LocalTime.parse(it) }
                }
                intent.getStringExtra(AlarmScheduler.EXTRA_END_TIME)?.let {
                    runCatching { endTime = LocalTime.parse(it) }
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    startForeground(
                        NOTIFICATION_ID,
                        buildProgressNotification(),
                        android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                    )
                } else {
                    startForeground(NOTIFICATION_ID, buildProgressNotification())
                }
                startTicker()
            }
        }
        return START_STICKY
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = serviceScope.launch {
            while (isActive) {
                val now = LocalTime.now()
                if (now.isAfter(endTime) || now == endTime) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                    break
                }
                val notification = buildProgressNotification()
                val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                manager.notify(NOTIFICATION_ID, notification)

                delay(60_000L) // Update every minute
            }
        }
    }

    private fun buildProgressNotification(): Notification {
        val now = LocalTime.now()
        val totalSecs = Duration.between(startTime, endTime).seconds.coerceAtLeast(1)
        val elapsedSecs = Duration.between(startTime, now).seconds.coerceIn(0, totalSecs)
        val progressPercent = ((elapsedSecs * 100) / totalSecs).toInt()
        val remainingMinutes = ChronoUnit.MINUTES.between(now, endTime).coerceAtLeast(0)
        val remainingSeconds = ChronoUnit.SECONDS.between(now, endTime).coerceAtLeast(0)
        val targetWhen = System.currentTimeMillis() + (remainingSeconds * 1000L)

        val title = "$courseName · $className"
        val subtitle = "$room · 下课 ${endTime.hour}:${endTime.minute.toString().padStart(2, '0')} (余${remainingMinutes}分)"

        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, InClassService::class.java).apply { action = ACTION_STOP_IN_CLASS },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val extendIntent = PendingIntent.getService(
            this,
            2,
            Intent(this, InClassService::class.java).apply {
                action = ACTION_EXTEND_IN_CLASS
                putExtra("extra_extend_minutes", 5)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val bigTextContent = "📍 教室: $room\n⏰ 下课时间: ${endTime.hour}:${endTime.minute.toString().padStart(2, '0')} (剩余 ${remainingMinutes} 分钟)\n⏳ 课堂进度: $progressPercent%"

        // Public-safe notification version for Lock Screen when device has privacy / hide sensitive content enabled
        val publicNotification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_in_class)
            .setContentTitle("📕 $courseName · $className")
            .setContentText("📍 $room · 下课 ${endTime.hour}:${endTime.minute.toString().padStart(2, '0')}")
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setWhen(targetWhen)
            .setShowWhen(true)
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setContentIntent(contentIntent)
            .build()

        // Android 16 Live Updates / ColorOS 16+ Pantanal Fluid Cloud Promotion Extras
        val extras = android.os.Bundle().apply {
            // Android 16 Promoted Ongoing Notification (Live Updates status bar chip / pill)
            putBoolean("android.requestPromotedOngoing", true)
            // ColorOS / OxygenOS / realme UI Fluid Cloud & Capsule Keys
            putBoolean("coloros.fluid_cloud", true)
            putBoolean("coloros_capsule", true)
            putBoolean("oplus.fluid_cloud", true)
            putBoolean("oplus_capsule", true)
            putInt("oplus_notification_type", 1) // 1: countdown / timer capsule
            putString("android.substName", getString(R.string.app_name))
        }

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_in_class)
            .setContentTitle("📕 $courseName · $className")
            .setContentText("📍 $room · 下课 ${endTime.hour}:${endTime.minute.toString().padStart(2, '0')} (余${remainingMinutes}分)")
            .setSubText("授课进行中")
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setWhen(targetWhen)
            .setShowWhen(true)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle("📕 $courseName · $className")
                    .setSummaryText("正在上课 · $progressPercent%")
                    .bigText(bigTextContent)
            )
            .setProgress(100, progressPercent, false)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPublicVersion(publicNotification)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setContentIntent(contentIntent)
            .addAction(0, getString(R.string.btn_end_class), stopIntent)
            .addAction(0, getString(R.string.btn_extend_5min), extendIntent)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .addExtras(extras)
            .build()

        notification.extras.putAll(extras)
        return notification
    }

    override fun onDestroy() {
        super.onDestroy()
        tickerJob?.cancel()
    }
}
