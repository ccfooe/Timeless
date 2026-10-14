package com.classbell.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.classbell.app.data.backup.BackupManager
import com.classbell.app.data.local.ClassBellDatabase
import com.classbell.app.data.repository.AffairRepository
import com.classbell.app.data.repository.CourseRepository
import com.classbell.app.data.repository.PeriodRepository
import com.classbell.app.data.repository.ScheduleRepository
import com.classbell.app.data.repository.SemesterRepository
import com.classbell.app.data.repository.SettingsRepository
import com.classbell.app.service.AlarmScheduler

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ClassBellApp : Application() {

    companion object {
        const val CHANNEL_IN_CLASS_ID = "classbell_in_class_v6"
        const val CHANNEL_WARNING_ID = "classbell_warning"
        const val CHANNEL_AFFAIRS_ID = "classbell_affairs"

        @Volatile
        var cachedPeriodSlots: List<com.classbell.app.data.model.PeriodSlot> = emptyList()
    }

    val database by lazy { ClassBellDatabase.getDatabase(this) }
    val settingsRepository by lazy { SettingsRepository(this) }
    val semesterRepository by lazy { SemesterRepository(database.semesterDao()) }
    val courseRepository by lazy { CourseRepository(database.courseDao()) }
    val periodRepository by lazy { PeriodRepository(database.periodSlotDao()) }
    val affairRepository by lazy { AffairRepository(database.affairDao()) }
    val scheduleRepository by lazy {
        ScheduleRepository(
            database.semesterDao(),
            database.courseDao(),
            database.periodSlotDao(),
            database.holidayDao(),
            database.overrideDao()
        )
    }
    val backupManager by lazy { BackupManager(database) }
    val alarmScheduler by lazy { AlarmScheduler(this) }

    override fun onCreate() {
        super.onCreate()
        setupCrashHandler()
        createNotificationChannels()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                cachedPeriodSlots = database.periodSlotDao().getAllPeriodSlotsSync()
            } catch (e: Throwable) {
                android.util.Log.e("ClassBellApp", "Failed to cache period slots", e)
            }
        }
    }

    private fun setupCrashHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            android.util.Log.e("TimelessCrash", "Fatal exception on thread ${thread.name}", throwable)
            try {
                val dir = getExternalFilesDir(null) ?: filesDir
                val file = java.io.File(dir, "crash_log.txt")
                val sw = java.io.StringWriter()
                val pw = java.io.PrintWriter(sw)
                throwable.printStackTrace(pw)
                file.writeText("Time: ${java.util.Date()}\nThread: ${thread.name}\nException:\n$sw")
            } catch (e: Throwable) {
                e.printStackTrace()
            }
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Purge obsolete channels to force recreation with optimal ColorOS Fluid Cloud, HyperOS Focus & lockscreen settings
            runCatching {
                manager.deleteNotificationChannel("classbell_in_class")
                manager.deleteNotificationChannel("classbell_in_class_v2")
                manager.deleteNotificationChannel("classbell_in_class_v3")
                manager.deleteNotificationChannel("classbell_in_class_v4")
                manager.deleteNotificationChannel("classbell_in_class_v5")
            }

            // 1. In-Class Progress Channel (IMPORTANCE_HIGH required by ColorOS 15/16 Fluid Cloud, HyperOS & OriginOS to qualify as Live Capsule, kept completely silent)
            val inClassChannel = NotificationChannel(
                CHANNEL_IN_CLASS_ID,
                getString(R.string.notification_channel_in_class),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = getString(R.string.notification_channel_in_class_desc)
                setShowBadge(false)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                enableLights(false)
                setSound(null, null)
                enableVibration(false)
                runCatching { setBypassDnd(true) }
            }

            // 2. Class End Warning Channel (High importance for vibration and heads-up)
            val warningChannel = NotificationChannel(
                CHANNEL_WARNING_ID,
                getString(R.string.notification_channel_warning),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = getString(R.string.notification_channel_warning_desc)
                enableVibration(true)
                setShowBadge(true)
            }

            // 3. Affairs Reminder Channel
            val affairsChannel = NotificationChannel(
                CHANNEL_AFFAIRS_ID,
                getString(R.string.notification_channel_affairs),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = getString(R.string.notification_channel_affairs_desc)
                enableVibration(true)
                setShowBadge(true)
            }

            manager.createNotificationChannels(listOf(inClassChannel, warningChannel, affairsChannel))
        }
    }
}
