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
import com.classbell.app.core.OccurrenceEngine
import com.classbell.app.data.model.Course
import com.classbell.app.data.model.PeriodSlot
import com.classbell.app.data.model.SubPeriodKind
import com.classbell.app.data.model.SubPeriodState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

class InClassService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var tickerJob: Job? = null

    private var courseName: String = ""
    private var className: String = ""
    private var room: String = ""
    private var periodStart: Int = 1
    private var periodEnd: Int = 1
    private var startTime: LocalTime = LocalTime.of(8, 30)
    private var endTime: LocalTime = LocalTime.of(12, 0)
    private var periodSlots: List<PeriodSlot> = emptyList()

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
            endTime: String,
            periodStart: Int = 1,
            periodEnd: Int = 1
        ) {
            val intent = Intent(context, InClassService::class.java).apply {
                action = ACTION_START_IN_CLASS
                putExtra(AlarmScheduler.EXTRA_COURSE_NAME, courseName)
                putExtra(AlarmScheduler.EXTRA_CLASS_NAME, className)
                putExtra(AlarmScheduler.EXTRA_ROOM, room)
                putExtra(AlarmScheduler.EXTRA_START_TIME, startTime)
                putExtra(AlarmScheduler.EXTRA_END_TIME, endTime)
                putExtra(AlarmScheduler.EXTRA_PERIOD_START, periodStart)
                putExtra(AlarmScheduler.EXTRA_PERIOD_END, periodEnd)
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
                periodStart = intent.getIntExtra(AlarmScheduler.EXTRA_PERIOD_START, 1)
                periodEnd = intent.getIntExtra(AlarmScheduler.EXTRA_PERIOD_END, 1)
                intent.getStringExtra(AlarmScheduler.EXTRA_START_TIME)?.let {
                    runCatching { startTime = LocalTime.parse(it) }
                }
                intent.getStringExtra(AlarmScheduler.EXTRA_END_TIME)?.let {
                    runCatching { endTime = LocalTime.parse(it) }
                }

                // Load period slots from memory cache or database
                if (ClassBellApp.cachedPeriodSlots.isNotEmpty()) {
                    periodSlots = ClassBellApp.cachedPeriodSlots
                } else {
                    serviceScope.launch(Dispatchers.IO) {
                        val app = application as? ClassBellApp
                        if (app != null) {
                            val slots = app.database.periodSlotDao().getAllPeriodSlotsSync()
                            ClassBellApp.cachedPeriodSlots = slots
                            periodSlots = slots
                            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                            manager.notify(NOTIFICATION_ID, buildProgressNotification())
                        }
                    }
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

                delay(5_000L) // Refresh every 5 seconds to keep progress and text precisely in sync with UI
            }
        }
    }

    private fun buildProgressNotification(): Notification {
        val now = LocalTime.now()

        val dummyCourse = Course(
            id = 0L,
            semesterId = 0L,
            name = courseName,
            className = className,
            room = room,
            periodStart = periodStart,
            periodEnd = periodEnd,
            dayOfWeek = LocalDate.now().dayOfWeek.value,
            weekFrom = 1,
            weekTo = 25
        )

        val subPeriod = if (periodSlots.isNotEmpty() && periodEnd >= periodStart) {
            OccurrenceEngine.resolveSubPeriodState(dummyCourse, periodSlots, now)
        } else null

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

        val today = LocalDate.now()
        fun calculateTargetWhen(targetTime: LocalTime): Long {
            val zonedDateTime = if (targetTime.isBefore(now) && startTime.isAfter(targetTime)) {
                targetTime.atDate(today.plusDays(1)).atZone(ZoneId.systemDefault())
            } else {
                targetTime.atDate(today).atZone(ZoneId.systemDefault())
            }
            return zonedDateTime.toInstant().toEpochMilli()
        }

        fun makeProgressBar(percent: Int, length: Int = 10): String {
            val clamped = percent.coerceIn(0, 100)
            val filled = (clamped * length) / 100
            val empty = (length - filled).coerceAtLeast(0)
            return "▰".repeat(filled) + "▱".repeat(empty)
        }

        fun createExtras(capsuleTitle: String, whenMillis: Long, progressPercent: Int): android.os.Bundle {
            return android.os.Bundle().apply {
                putBoolean("android.requestPromotedOngoing", true)
                putBoolean("coloros.fluid_cloud", true)
                putBoolean("coloros_capsule", true)
                putBoolean("oplus.fluid_cloud", true)
                putBoolean("oplus_capsule", true)
                putInt("oplus_notification_type", 1)
                putInt("oplus_view_type", 0) // 0: Capsule only in status bar; NO heads-up popup banner over app!
                putInt("coloros_notification_type", 1)
                putInt("coloros_view_type", 0)
                putInt("coloros_progress", progressPercent)
                putInt("oplus_progress", progressPercent)
                putInt("coloros_progress_max", 100)
                putInt("oplus_progress_max", 100)
                putInt("coloros_progress_current", progressPercent)
                putInt("oplus_progress_current", progressPercent)
                putString("android.substName", getString(R.string.app_name))
                putString("coloros_capsule_icon", "ic_stat_in_class")
                putString("oplus_capsule_icon", "ic_stat_in_class")
                putString("coloros_capsule_title", capsuleTitle)
                putString("oplus_capsule_title", capsuleTitle)
                putLong("coloros_capsule_time", whenMillis)
                putLong("oplus_capsule_time", whenMillis)
            }
        }

        val flagPromoted = 0x00010000

        if (subPeriod != null && subPeriod.isBreak) {
            // Recess / Break Notification
            val targetWhen = calculateTargetWhen(subPeriod.endTime)
            val progressPercent = (subPeriod.progress * 100).toInt().coerceIn(0, 100)
            val breakTitle = subPeriod.breakTitle ?: getString(R.string.status_recess)
            val nextPeriod = subPeriod.nextPeriodLabel ?: ""
            val progressBar = makeProgressBar(progressPercent)

            // Ultra-compact title for punch-hole capsule (2/3 width: e.g. "课间" or "Rest")
            val capsuleTitle = getString(R.string.capsule_recess)
            // Rich expanded title for floating card & notifications
            val bigTitle = "☕ $breakTitle"
            // High-visibility progress bar in subText directly below the chronometer
            val subText = "$breakTitle $progressPercent%  $progressBar  ·  📍 $room"
            val contentText = if (nextPeriod.isNotEmpty()) {
                getString(R.string.recess_remaining_format, nextPeriod, subPeriod.remainingMinutes.toInt())
            } else {
                "$breakTitle · ${subPeriod.remainingMinutes}m"
            }
            val bigTextContent = getString(
                R.string.notification_recess_bigtext_format,
                breakTitle,
                room,
                nextPeriod,
                "${subPeriod.endTime.hour}:${subPeriod.endTime.minute.toString().padStart(2, '0')}",
                getString(R.string.time_remaining_format, subPeriod.remainingMinutes.toInt()),
                progressPercent,
                progressBar
            )
            val extras = createExtras(capsuleTitle, targetWhen, progressPercent)

            val publicNotification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_in_class)
                .setContentTitle(capsuleTitle)
                .setContentText(contentText)
                .setSubText(subText)
                .setUsesChronometer(true)
                .setChronometerCountDown(true)
                .setWhen(targetWhen)
                .setShowWhen(true)
                .setProgress(100, progressPercent, false)
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .setBigContentTitle(bigTitle)
                        .setSummaryText(getString(R.string.recess_progress_format, progressPercent))
                        .bigText(bigTextContent)
                )
                .setOngoing(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
                .setContentIntent(contentIntent)
                .addAction(0, getString(R.string.btn_start_next_period_now), stopIntent)
                .addAction(0, getString(R.string.btn_extend_break_5min), extendIntent)
                .addExtras(extras)
                .build()

            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_in_class)
                .setContentTitle(capsuleTitle)
                .setContentText(contentText)
                .setSubText(subText)
                .setUsesChronometer(true)
                .setChronometerCountDown(true)
                .setWhen(targetWhen)
                .setShowWhen(true)
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .setBigContentTitle(bigTitle)
                        .setSummaryText(getString(R.string.recess_progress_format, progressPercent))
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
                .addAction(0, getString(R.string.btn_start_next_period_now), stopIntent)
                .addAction(0, getString(R.string.btn_extend_break_5min), extendIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .addExtras(extras)
                .build()

            notification.flags = notification.flags or flagPromoted
            publicNotification.flags = publicNotification.flags or flagPromoted
            notification.extras.putAll(extras)
            return notification
        } else if (subPeriod != null && subPeriod.kind == SubPeriodKind.IN_CLASS) {
            // In-Class Sub-Period Notification
            val targetWhen = calculateTargetWhen(subPeriod.endTime)
            val progressPercent = (subPeriod.progress * 100).toInt().coerceIn(0, 100)
            val statusLabel = getString(R.string.status_in_progress)
            val progressBar = makeProgressBar(progressPercent)

            // Ultra-compact title for punch-hole capsule (2/3 width: e.g. "1节" or "P1")
            val capsuleTitle = getString(R.string.capsule_period_format, subPeriod.periodOrder)
            // Rich expanded title for floating card & notifications
            val bigTitle = "📕 $courseName · $className"
            // High-visibility progress bar in subText directly below the chronometer
            val subText = "$statusLabel $progressPercent%  $progressBar  ·  📍 $room"
            val contentText = "📍 $room · ${subPeriod.periodLabel} · $progressPercent% $progressBar · ${getString(R.string.time_remaining_format, subPeriod.remainingMinutes.toInt())}"
            val bigTextContent = getString(
                R.string.notification_inclass_bigtext_format,
                room,
                subPeriod.periodLabel,
                "${subPeriod.endTime.hour}:${subPeriod.endTime.minute.toString().padStart(2, '0')}",
                getString(R.string.time_remaining_format, subPeriod.remainingMinutes.toInt()),
                progressPercent,
                progressBar,
                subPeriod.periodIndexInCourse,
                subPeriod.totalPeriodsInCourse
            )
            val extras = createExtras(capsuleTitle, targetWhen, progressPercent)

            val publicNotification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_in_class)
                .setContentTitle(capsuleTitle)
                .setContentText(contentText)
                .setSubText(subText)
                .setUsesChronometer(true)
                .setChronometerCountDown(true)
                .setWhen(targetWhen)
                .setShowWhen(true)
                .setProgress(100, progressPercent, false)
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .setBigContentTitle(bigTitle)
                        .setSummaryText("$statusLabel · $progressPercent%")
                        .bigText(bigTextContent)
                )
                .setOngoing(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
                .setContentIntent(contentIntent)
                .addAction(0, getString(R.string.btn_end_class), stopIntent)
                .addAction(0, getString(R.string.btn_extend_5min), extendIntent)
                .addExtras(extras)
                .build()

            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_in_class)
                .setContentTitle(capsuleTitle)
                .setContentText(contentText)
                .setSubText(subText)
                .setUsesChronometer(true)
                .setChronometerCountDown(true)
                .setWhen(targetWhen)
                .setShowWhen(true)
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .setBigContentTitle(bigTitle)
                        .setSummaryText("$statusLabel · $progressPercent%")
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
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .addExtras(extras)
                .build()

            notification.flags = notification.flags or flagPromoted
            publicNotification.flags = publicNotification.flags or flagPromoted
            notification.extras.putAll(extras)
            return notification
        } else {
            // Fallback for ad-hoc custom sessions or before slots loaded
            val totalSecs = Duration.between(startTime, endTime).seconds.coerceAtLeast(1)
            val elapsedSecs = Duration.between(startTime, now).seconds.coerceIn(0, totalSecs)
            val progressPercent = ((elapsedSecs * 100) / totalSecs).toInt().coerceIn(0, 100)
            val remainingMinutes = ChronoUnit.MINUTES.between(now, endTime).coerceAtLeast(0)
            val targetWhen = calculateTargetWhen(endTime)
            val statusLabel = getString(R.string.status_in_progress)
            val progressBar = makeProgressBar(progressPercent)

            // Ultra-compact title for punch-hole capsule (2/3 width: e.g. "1节" / "P1" or "上课" / "Class")
            val capsuleTitle = if (periodStart > 0) {
                getString(R.string.capsule_period_format, periodStart)
            } else {
                getString(R.string.capsule_in_class)
            }
            // Rich expanded title for floating card & notifications
            val bigTitle = "📕 $courseName · $className"
            // High-visibility progress bar in subText directly below the chronometer
            val subText = "$statusLabel $progressPercent%  $progressBar  ·  📍 $room"
            val contentText = "📍 $room · $progressPercent% $progressBar · ${getString(R.string.time_remaining_format, remainingMinutes.toInt())}"
            val bigTextContent = getString(
                R.string.notification_fallback_bigtext_format,
                room,
                "${endTime.hour}:${endTime.minute.toString().padStart(2, '0')}",
                getString(R.string.time_remaining_format, remainingMinutes.toInt()),
                progressPercent,
                progressBar
            )
            val extras = createExtras(capsuleTitle, targetWhen, progressPercent)

            val publicNotification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_in_class)
                .setContentTitle(capsuleTitle)
                .setContentText(contentText)
                .setSubText(subText)
                .setUsesChronometer(true)
                .setChronometerCountDown(true)
                .setWhen(targetWhen)
                .setShowWhen(true)
                .setProgress(100, progressPercent, false)
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .setBigContentTitle(bigTitle)
                        .setSummaryText("$statusLabel · $progressPercent%")
                        .bigText(bigTextContent)
                )
                .setOngoing(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
                .setContentIntent(contentIntent)
                .addAction(0, getString(R.string.btn_end_class), stopIntent)
                .addAction(0, getString(R.string.btn_extend_5min), extendIntent)
                .addExtras(extras)
                .build()

            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_in_class)
                .setContentTitle(capsuleTitle)
                .setContentText(contentText)
                .setSubText(subText)
                .setUsesChronometer(true)
                .setChronometerCountDown(true)
                .setWhen(targetWhen)
                .setShowWhen(true)
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .setBigContentTitle(bigTitle)
                        .setSummaryText("$statusLabel · $progressPercent%")
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
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .addExtras(extras)
                .build()

            notification.flags = notification.flags or flagPromoted
            publicNotification.flags = publicNotification.flags or flagPromoted
            notification.extras.putAll(extras)
            return notification
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        tickerJob?.cancel()
    }
}
