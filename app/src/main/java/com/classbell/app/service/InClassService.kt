package com.classbell.app.service

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    private var isManualSession: Boolean = false
    private var startTime: LocalTime = LocalTime.of(8, 30)
    private var endTime: LocalTime = LocalTime.of(12, 0)
    @Volatile private var periodSlots: List<PeriodSlot> = emptyList()
    private var screenReceiverRegistered = false

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_SCREEN_ON) {
                // Instantly update notification to newest second when user turns on screen
                runCatching {
                    val manager = getSystemService(NOTIFICATION_SERVICE) as? NotificationManager
                    manager?.notify(NOTIFICATION_ID, buildProgressNotification())
                }
            }
        }
    }

    companion object {
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = ClassBellApp.CHANNEL_IN_CLASS_ID

        const val ACTION_START_IN_CLASS = "com.timeless.app.START_IN_CLASS"
        const val ACTION_STOP_IN_CLASS = "com.timeless.app.STOP_IN_CLASS"
        const val ACTION_EXTEND_IN_CLASS = "com.timeless.app.EXTEND_IN_CLASS"

        @Volatile
        var isRunning: Boolean = false
            private set

        @Volatile
        var currentCourseName: String = ""
            private set

        fun reconcile(
            context: Context,
            courseName: String,
            className: String,
            room: String,
            startTime: String,
            endTime: String,
            periodStart: Int = 1,
            periodEnd: Int = 1,
            isManualSession: Boolean = false
        ) {
            if (!isRunning || currentCourseName != courseName) {
                start(context, courseName, className, room, startTime, endTime, periodStart, periodEnd, isManualSession)
            }
        }

        fun start(
            context: Context,
            courseName: String,
            className: String,
            room: String,
            startTime: String,
            endTime: String,
            periodStart: Int = 1,
            periodEnd: Int = 1,
            isManualSession: Boolean = false
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
                putExtra(AlarmScheduler.EXTRA_IS_MANUAL_SESSION, isManualSession)
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

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        runCatching {
            registerReceiver(screenReceiver, IntentFilter(Intent.ACTION_SCREEN_ON))
            screenReceiverRegistered = true
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_IN_CLASS -> {
                isRunning = false
                currentCourseName = ""
                cancelExactStopAlarm()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_EXTEND_IN_CLASS -> {
                val mins = intent.getIntExtra("extra_extend_minutes", 5)
                endTime = endTime.plusMinutes(mins.toLong())
                scheduleExactStopAlarm(calculateTargetWhen(endTime))
                val notification = buildProgressNotification()
                val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                manager.notify(NOTIFICATION_ID, notification)
                return START_STICKY
            }
            ACTION_START_IN_CLASS -> {
                courseName = intent.getStringExtra(AlarmScheduler.EXTRA_COURSE_NAME) ?: ""
                currentCourseName = courseName
                className = intent.getStringExtra(AlarmScheduler.EXTRA_CLASS_NAME) ?: ""
                room = intent.getStringExtra(AlarmScheduler.EXTRA_ROOM) ?: ""
                periodStart = intent.getIntExtra(AlarmScheduler.EXTRA_PERIOD_START, 1)
                periodEnd = intent.getIntExtra(AlarmScheduler.EXTRA_PERIOD_END, 1)
                isManualSession = intent.getBooleanExtra(AlarmScheduler.EXTRA_IS_MANUAL_SESSION, false)

                intent.getStringExtra(AlarmScheduler.EXTRA_START_TIME)?.let {
                    runCatching { startTime = LocalTime.parse(it) }
                }
                intent.getStringExtra(AlarmScheduler.EXTRA_END_TIME)?.let {
                    runCatching { endTime = LocalTime.parse(it) }
                }

                // Schedule hardware wakeup alarm to kill service at exact end time (even in Doze deep sleep)
                scheduleExactStopAlarm(calculateTargetWhen(endTime))

                // Load period slots from memory cache or database if scheduled
                if (!isManualSession) {
                    if (ClassBellApp.cachedPeriodSlots.isNotEmpty()) {
                        periodSlots = ClassBellApp.cachedPeriodSlots
                    } else {
                        serviceScope.launch(Dispatchers.IO) {
                            val app = application as? ClassBellApp
                            if (app != null) {
                                val slots = app.database.periodSlotDao().getAllPeriodSlotsSync()
                                ClassBellApp.cachedPeriodSlots = slots
                                periodSlots = slots
                                withContext(Dispatchers.Main) {
                                    val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                                    manager.notify(NOTIFICATION_ID, buildProgressNotification())
                                }
                            }
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
                    cancelExactStopAlarm()
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                    break
                }

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
                val subPeriod = if (!isManualSession && periodSlots.isNotEmpty() && periodEnd >= periodStart) {
                    OccurrenceEngine.resolveSubPeriodState(dummyCourse, periodSlots, now)
                } else null

                if (subPeriod != null && subPeriod.kind == SubPeriodKind.FINISHED) {
                    cancelExactStopAlarm()
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                    break
                }

                val notification = buildProgressNotification()
                val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                manager.notify(NOTIFICATION_ID, notification)

                delay(5_000L) // Refresh every 5 seconds to keep countdown and progress strictly in sync
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

        // Only resolve sub-period state from university timetable if this is NOT a manual ad-hoc session
        val subPeriod = if (!isManualSession && periodSlots.isNotEmpty() && periodEnd >= periodStart) {
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

        fun createExtras(capsuleTitle: String, whenMillis: Long, progressPercent: Int): android.os.Bundle {
            val bundle = android.os.Bundle()
            MultiOemDynamicIslandContract.populateBundle(
                bundle = bundle,
                capsuleTitle = capsuleTitle,
                whenMillis = whenMillis,
                progressPercent = progressPercent,
                appName = getString(R.string.app_name)
            )
            return bundle
        }

        val flagPromoted = 0x00010000

        if (subPeriod != null && subPeriod.isBreak) {
            // Case A: Recess / Break
            val targetWhen = calculateTargetWhen(subPeriod.endTime)
            val progressPercent = (subPeriod.progress * 100).toInt().coerceIn(0, 100)
            val breakTitle = subPeriod.breakTitle ?: getString(R.string.status_recess)
            val nextPeriod = subPeriod.nextPeriodLabel ?: ""

            val capsuleTitle = getString(R.string.capsule_recess)
            val startClock = String.format("%02d:%02d", subPeriod.startTime.hour, subPeriod.startTime.minute)
            val endClock = String.format("%02d:%02d", subPeriod.endTime.hour, subPeriod.endTime.minute)
            val bigTitle = if (room.isNotBlank()) "☕ $breakTitle · $room" else "☕ $breakTitle"
            val subText = InClassNotificationFormatter.formatRecessSubText(breakTitle, progressPercent, room = room)
            val lockscreenSubText = InClassNotificationFormatter.formatLockscreenSubText(breakTitle, progressPercent, barLength = 10)
            val contentText = InClassNotificationFormatter.formatRecessContentText(
                nextPeriod = nextPeriod,
                remainingMinutes = subPeriod.remainingMinutes.toInt(),
                room = room
            )
            val bigTextContent = InClassNotificationFormatter.formatBauhausRecessBigText(
                remainingMinutes = subPeriod.remainingMinutes.toInt(),
                nextStartTimeStr = endClock,
                nextPeriod = nextPeriod
            )
            val lockscreenBigText = InClassNotificationFormatter.formatLockscreenRecessBigText(
                startTimeStr = startClock,
                endTimeStr = endClock,
                nextPeriod = nextPeriod,
                remainingMinutes = subPeriod.remainingMinutes.toInt(),
                progressPercent = progressPercent,
                barLength = 10
            )
            val extras = createExtras(capsuleTitle, targetWhen, progressPercent)

            val publicNotification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_in_class)
                .setContentTitle(capsuleTitle)
                .setContentText(contentText)
                .setSubText(lockscreenSubText)
                .setUsesChronometer(true)
                .setChronometerCountDown(true)
                .setWhen(targetWhen)
                .setShowWhen(true)
                .setProgress(100, progressPercent, false)
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .setBigContentTitle(bigTitle)
                        .setSummaryText(getString(R.string.recess_progress_format, progressPercent))
                        .bigText(lockscreenBigText)
                )
                .setOngoing(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
                .setContentIntent(contentIntent)
                .addAction(0, getString(R.string.btn_start_next_period_now), stopIntent)
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
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .addExtras(extras)
                .build()

            notification.flags = notification.flags or flagPromoted
            publicNotification.flags = publicNotification.flags or flagPromoted
            notification.extras.putAll(extras)
            return notification

        } else if (subPeriod != null && subPeriod.kind == SubPeriodKind.PRE_CLASS) {
            // Case B: Pre-Class Preparation
            val targetWhen = calculateTargetWhen(subPeriod.endTime)
            val capsuleTitle = getString(R.string.capsule_pre_class)
            val bigTitle = if (room.isNotBlank()) "⏰ $courseName · 课前准备 · $room" else "⏰ $courseName · 课前准备"
            val startClock = String.format("%02d:%02d", subPeriod.endTime.hour, subPeriod.endTime.minute)
            val subText = "即将上课 · $startClock 开始"
            val contentText = "$courseName · 距上课 ${subPeriod.remainingMinutes} 分钟"
            val bigTextContent = "课程: $courseName\n地点: $room · 上课时间: $startClock\n距上课还有 ${subPeriod.remainingMinutes} 分钟"
            val extras = createExtras(capsuleTitle, targetWhen, 0)

            val publicNotification = NotificationCompat.Builder(this, CHANNEL_ID)
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
                        .setSummaryText("课前准备 · $startClock")
                        .bigText(bigTextContent)
                )
                .setOngoing(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
                .setContentIntent(contentIntent)
                .addAction(0, getString(R.string.btn_cancel_pre_class), stopIntent)
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
                        .setSummaryText("课前准备 · $startClock")
                        .bigText(bigTextContent)
                )
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setPublicVersion(publicNotification)
                .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
                .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
                .setContentIntent(contentIntent)
                .addAction(0, getString(R.string.btn_cancel_pre_class), stopIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .addExtras(extras)
                .build()

            notification.flags = notification.flags or flagPromoted
            publicNotification.flags = publicNotification.flags or flagPromoted
            notification.extras.putAll(extras)
            return notification

        } else if (subPeriod != null && subPeriod.kind == SubPeriodKind.IN_CLASS) {
            // Case C: Scheduled Sub-Period In Class
            val targetWhen = calculateTargetWhen(subPeriod.endTime)
            val progressPercent = (subPeriod.progress * 100).toInt().coerceIn(0, 100)
            val statusLabel = getString(R.string.status_in_progress)

            val capsuleTitle = getString(R.string.capsule_period_format, subPeriod.periodOrder)
            val startClock = String.format("%02d:%02d", subPeriod.startTime.hour, subPeriod.startTime.minute)
            val endClock = String.format("%02d:%02d", subPeriod.endTime.hour, subPeriod.endTime.minute)
            val bigTitle = if (room.isNotBlank()) "📕 $courseName · ${subPeriod.periodLabel} · $room" else "📕 $courseName · ${subPeriod.periodLabel}"
            val subText = InClassNotificationFormatter.formatInClassSubText(
                periodLabel = subPeriod.periodLabel,
                progressPercent = progressPercent,
                room = room
            )
            val lockscreenSubText = InClassNotificationFormatter.formatLockscreenSubText(
                label = subPeriod.periodLabel,
                progressPercent = progressPercent,
                barLength = 10
            )
            val contentText = InClassNotificationFormatter.formatInClassContentText(
                remainingMinutes = subPeriod.remainingMinutes.toInt(),
                room = room
            )
            val bigTextContent = InClassNotificationFormatter.formatBauhausInClassBigText(
                remainingMinutes = subPeriod.remainingMinutes.toInt(),
                endTimeStr = endClock
            )
            val lockscreenBigText = InClassNotificationFormatter.formatLockscreenInClassBigText(
                startTimeStr = startClock,
                endTimeStr = endClock,
                remainingMinutes = subPeriod.remainingMinutes.toInt(),
                progressPercent = progressPercent,
                barLength = 10
            )
            val extras = createExtras(capsuleTitle, targetWhen, progressPercent)

            val publicNotification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_in_class)
                .setContentTitle(capsuleTitle)
                .setContentText(contentText)
                .setSubText(lockscreenSubText)
                .setUsesChronometer(true)
                .setChronometerCountDown(true)
                .setWhen(targetWhen)
                .setShowWhen(true)
                .setProgress(100, progressPercent, false)
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .setBigContentTitle(bigTitle)
                        .setSummaryText("$statusLabel · $progressPercent%")
                        .bigText(lockscreenBigText)
                )
                .setOngoing(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
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
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .addExtras(extras)
                .build()

            notification.flags = notification.flags or flagPromoted
            publicNotification.flags = publicNotification.flags or flagPromoted
            notification.extras.putAll(extras)
            return notification

        } else {
            // Case D: Manual Session or fallback
            val totalSecs = Duration.between(startTime, endTime).seconds.coerceAtLeast(1)
            val elapsedSecs = Duration.between(startTime, now).seconds.coerceIn(0, totalSecs)
            val progressPercent = ((elapsedSecs * 100) / totalSecs).toInt().coerceIn(0, 100)
            val remainingMinutes = ChronoUnit.MINUTES.between(now, endTime).coerceAtLeast(0)
            val targetWhen = calculateTargetWhen(endTime)
            val statusLabel = getString(R.string.status_in_progress)

            val capsuleTitle = if (periodStart > 0) {
                getString(R.string.capsule_period_format, periodStart)
            } else {
                getString(R.string.capsule_in_class)
            }
            val startClock = String.format("%02d:%02d", startTime.hour, startTime.minute)
            val endClock = String.format("%02d:%02d", endTime.hour, endTime.minute)
            val bigTitle = if (room.isNotBlank()) "📕 $courseName · $room" else "📕 $courseName"
            val subText = InClassNotificationFormatter.formatInClassSubText(
                periodLabel = statusLabel,
                progressPercent = progressPercent,
                room = room
            )
            val lockscreenSubText = InClassNotificationFormatter.formatLockscreenSubText(
                label = statusLabel,
                progressPercent = progressPercent,
                barLength = 10
            )
            val contentText = InClassNotificationFormatter.formatInClassContentText(
                remainingMinutes = remainingMinutes.toInt(),
                room = room
            )
            val bigTextContent = InClassNotificationFormatter.formatBauhausInClassBigText(
                remainingMinutes = remainingMinutes.toInt(),
                endTimeStr = endClock
            )
            val lockscreenBigText = InClassNotificationFormatter.formatLockscreenInClassBigText(
                startTimeStr = startClock,
                endTimeStr = endClock,
                remainingMinutes = remainingMinutes.toInt(),
                progressPercent = progressPercent,
                barLength = 10
            )
            val extras = createExtras(capsuleTitle, targetWhen, progressPercent)

            val publicNotification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_in_class)
                .setContentTitle(capsuleTitle)
                .setContentText(contentText)
                .setSubText(lockscreenSubText)
                .setUsesChronometer(true)
                .setChronometerCountDown(true)
                .setWhen(targetWhen)
                .setShowWhen(true)
                .setProgress(100, progressPercent, false)
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .setBigContentTitle(bigTitle)
                        .setSummaryText("$statusLabel · $progressPercent%")
                        .bigText(lockscreenBigText)
                )
                .setOngoing(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
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
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .addExtras(extras)
                .build()

            notification.flags = notification.flags or flagPromoted
            publicNotification.flags = publicNotification.flags or flagPromoted
            notification.extras.putAll(extras)
            return notification
        }
    }

    private fun calculateTargetWhen(targetTime: LocalTime): Long {
        val now = LocalTime.now()
        val today = LocalDate.now()
        val zonedDateTime = if (targetTime.isBefore(now) && startTime.isAfter(targetTime)) {
            targetTime.atDate(today.plusDays(1)).atZone(ZoneId.systemDefault())
        } else {
            targetTime.atDate(today).atZone(ZoneId.systemDefault())
        }
        return zonedDateTime.toInstant().toEpochMilli()
    }

    private fun scheduleExactStopAlarm(targetMillis: Long) {
        cancelExactStopAlarm()
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(this, InClassService::class.java).apply {
            action = ACTION_STOP_IN_CLASS
        }
        val pendingIntent = PendingIntent.getService(
            this,
            1001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, targetMillis, pendingIntent)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, targetMillis, pendingIntent)
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, targetMillis, pendingIntent)
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, targetMillis, pendingIntent)
        }
    }

    private fun cancelExactStopAlarm() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(this, InClassService::class.java).apply {
            action = ACTION_STOP_IN_CLASS
        }
        val pendingIntent = PendingIntent.getService(
            this,
            1001,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        currentCourseName = ""
        tickerJob?.cancel()
        serviceScope.cancel()
        cancelExactStopAlarm()
        if (screenReceiverRegistered) {
            runCatching { unregisterReceiver(screenReceiver) }
            screenReceiverRegistered = false
        }
    }
}
