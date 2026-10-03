package com.classbell.app.core

import android.Manifest
import android.content.ContentProviderOperation
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import com.classbell.app.data.local.ClassBellDatabase
import com.classbell.app.data.model.Affair
import com.classbell.app.data.model.ReminderChoice
import com.classbell.app.data.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId
import java.util.TimeZone

/**
 * Manages seamless synchronization between Timeless and the system-native Calendar.
 *
 * Adheres to the Android CalendarProvider contract:
 * - Creates an isolated, dedicated "Timeless 课表与日程" local calendar account
 *   so user's personal events are NEVER touched or polluted.
 * - Supports instant cleanup when disabled (deletes only the Timeless calendar).
 * - Syncs full-semester courses (with holiday & shift override rules applied)
 *   and affairs/meetings with advance reminders.
 */
object CalendarSyncManager {

    private const val ACCOUNT_NAME = "com.timeless.app"
    private const val ACCOUNT_TYPE = CalendarContract.ACCOUNT_TYPE_LOCAL
    private const val CALENDAR_NAME = "Timeless 课表与日程"
    private const val CALENDAR_DISPLAY_NAME = "Timeless 课表与日程"
    private const val CALENDAR_COLOR = 0xFF5C6BC0.toInt() // Elegant soft indigo

    fun hasCalendarPermissions(context: Context): Boolean {
        val readGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
        val writeGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
        return readGranted && writeGranted
    }

    private fun syncCalendarUri(): Uri {
        return CalendarContract.Calendars.CONTENT_URI.buildUpon()
            .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
            .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME, ACCOUNT_NAME)
            .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_TYPE, ACCOUNT_TYPE)
            .build()
    }

    fun getOrCreateCalendarId(context: Context): Long? {
        if (!hasCalendarPermissions(context)) return null

        val projection = arrayOf(CalendarContract.Calendars._ID)
        val selection = "(${CalendarContract.Calendars.ACCOUNT_NAME} = ? AND ${CalendarContract.Calendars.ACCOUNT_TYPE} = ?)"
        val selectionArgs = arrayOf(ACCOUNT_NAME, ACCOUNT_TYPE)

        try {
            val cursor = context.contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
            )
            cursor?.use {
                if (it.moveToFirst()) {
                    return it.getLong(0)
                }
            }

            // Create dedicated isolated calendar
            val values = ContentValues().apply {
                put(CalendarContract.Calendars.ACCOUNT_NAME, ACCOUNT_NAME)
                put(CalendarContract.Calendars.ACCOUNT_TYPE, ACCOUNT_TYPE)
                put(CalendarContract.Calendars.NAME, CALENDAR_NAME)
                put(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME, CALENDAR_DISPLAY_NAME)
                put(CalendarContract.Calendars.CALENDAR_COLOR, CALENDAR_COLOR)
                put(CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL, CalendarContract.Calendars.CAL_ACCESS_OWNER)
                put(CalendarContract.Calendars.OWNER_ACCOUNT, ACCOUNT_NAME)
                put(CalendarContract.Calendars.CALENDAR_TIME_ZONE, TimeZone.getDefault().id)
                put(CalendarContract.Calendars.VISIBLE, 1)
                put(CalendarContract.Calendars.SYNC_EVENTS, 1)
            }
            val uri = context.contentResolver.insert(syncCalendarUri(), values)
            return uri?.lastPathSegment?.toLongOrNull()
        } catch (e: Exception) {
            return null
        }
    }

    fun clearCalendar(context: Context) {
        if (!hasCalendarPermissions(context)) return
        runCatching {
            context.contentResolver.delete(
                syncCalendarUri(),
                "${CalendarContract.Calendars.ACCOUNT_NAME} = ? AND ${CalendarContract.Calendars.ACCOUNT_TYPE} = ?",
                arrayOf(ACCOUNT_NAME, ACCOUNT_TYPE)
            )
        }
    }

    suspend fun syncAll(
        context: Context,
        database: ClassBellDatabase
    ): Boolean = withContext(Dispatchers.IO) {
        if (!hasCalendarPermissions(context)) return@withContext false

        val calendarId = getOrCreateCalendarId(context) ?: return@withContext false

        try {
            // 1. Wipe existing events in our dedicated calendar account
            context.contentResolver.delete(
                CalendarContract.Events.CONTENT_URI,
                "${CalendarContract.Events.CALENDAR_ID} = ?",
                arrayOf(calendarId.toString())
            )

            val semester = database.semesterDao().getActiveSemesterSync()
            val courses = if (semester != null) database.courseDao().getCoursesForSemesterSync(semester.id) else emptyList()
            val periodSlots = database.periodSlotDao().getAllPeriodSlotsSync()
            val holidays = if (semester != null) database.holidayDao().getHolidaysForSemesterSync(semester.id) else emptyList()
            val overrides = if (semester != null) database.overrideDao().getOverridesForSemesterSync(semester.id) else emptyList()
            val affairs = database.affairDao().getAllAffairsSync()

            val operations = ArrayList<ContentProviderOperation>()
            val zoneId = ZoneId.systemDefault()
            val timeZoneStr = TimeZone.getDefault().id
            val today = LocalDate.now()

            // 2. Expand & sync courses for active semester
            if (semester != null && courses.isNotEmpty() && periodSlots.isNotEmpty()) {
                val startDate = if (today.isBefore(semester.week1Monday)) semester.week1Monday else today.minusDays(7)
                val endDate = semester.week1Monday.plusWeeks(semester.totalWeeks.toLong())

                var curDate = startDate
                while (!curDate.isAfter(endDate)) {
                    val meetings = OccurrenceEngine.meetingsOn(
                        date = curDate,
                        semester = semester,
                        courses = courses,
                        periodSlots = periodSlots,
                        holidays = holidays,
                        overrides = overrides,
                        todayDate = today
                    )

                    for (meeting in meetings) {
                        val startZdt = curDate.atTime(meeting.startTime).atZone(zoneId)
                        val endZdt = curDate.atTime(meeting.endTime).atZone(zoneId)
                        val startMillis = startZdt.toInstant().toEpochMilli()
                        val endMillis = endZdt.toInstant().toEpochMilli()

                        val desc = buildString {
                            append("教室: ${meeting.course.room ?: "未指定"}\n")
                            meeting.course.teacher?.let { if (it.isNotBlank()) append("任课教师: $it\n") }
                            append("节次: 第${meeting.course.periodStart}-${meeting.course.periodEnd}节\n")
                            meeting.shiftLabel?.let { append("[$it]\n") }
                            append("来源: Timeless 智能课表")
                        }

                        val eventIndex = operations.size
                        operations.add(
                            ContentProviderOperation.newInsert(CalendarContract.Events.CONTENT_URI)
                                .withValue(CalendarContract.Events.CALENDAR_ID, calendarId)
                                .withValue(CalendarContract.Events.TITLE, meeting.course.name)
                                .withValue(CalendarContract.Events.DESCRIPTION, desc)
                                .withValue(CalendarContract.Events.EVENT_LOCATION, meeting.course.room ?: "")
                                .withValue(CalendarContract.Events.DTSTART, startMillis)
                                .withValue(CalendarContract.Events.DTEND, endMillis)
                                .withValue(CalendarContract.Events.EVENT_TIMEZONE, timeZoneStr)
                                .withValue(CalendarContract.Events.HAS_ALARM, 1)
                                .build()
                        )

                        // 15-minute advance reminder before class
                        operations.add(
                            ContentProviderOperation.newInsert(CalendarContract.Reminders.CONTENT_URI)
                                .withValueBackReference(CalendarContract.Reminders.EVENT_ID, eventIndex)
                                .withValue(CalendarContract.Reminders.MINUTES, 15)
                                .withValue(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT)
                                .build()
                        )
                    }
                    curDate = curDate.plusDays(1)
                }
            }

            // 3. Sync affairs / meetings
            for (affair in affairs) {
                val isAllDay = affair.time == null
                val startMillis = if (isAllDay) {
                    affair.date.atStartOfDay(zoneId).toInstant().toEpochMilli()
                } else {
                    affair.date.atTime(affair.time!!).atZone(zoneId).toInstant().toEpochMilli()
                }
                val endMillis = if (isAllDay) {
                    affair.date.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
                } else {
                    affair.date.atTime(affair.time!!.plusHours(1)).atZone(zoneId).toInstant().toEpochMilli()
                }

                val title = formatAffairEventTitle(affair.title, affair.isDone)
                val desc = buildAffairDescription(affair)
                val hasAlarm = shouldHaveAlarm(affair)

                val eventIndex = operations.size
                operations.add(
                    ContentProviderOperation.newInsert(CalendarContract.Events.CONTENT_URI)
                        .withValue(CalendarContract.Events.CALENDAR_ID, calendarId)
                        .withValue(CalendarContract.Events.TITLE, title)
                        .withValue(CalendarContract.Events.DESCRIPTION, desc)
                        .withValue(CalendarContract.Events.EVENT_LOCATION, affair.place ?: "")
                        .withValue(CalendarContract.Events.DTSTART, startMillis)
                        .withValue(CalendarContract.Events.DTEND, endMillis)
                        .withValue(CalendarContract.Events.ALL_DAY, if (isAllDay) 1 else 0)
                        .withValue(CalendarContract.Events.EVENT_TIMEZONE, timeZoneStr)
                        .withValue(CalendarContract.Events.HAS_ALARM, if (hasAlarm) 1 else 0)
                        .build()
                )

                if (hasAlarm) {
                    val minutes = getReminderMinutes(affair.reminder)
                    operations.add(
                        ContentProviderOperation.newInsert(CalendarContract.Reminders.CONTENT_URI)
                            .withValueBackReference(CalendarContract.Reminders.EVENT_ID, eventIndex)
                            .withValue(CalendarContract.Reminders.MINUTES, minutes)
                            .withValue(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT)
                            .build()
                    )
                }
            }

            // 4. Batch commit in chunks of 200 operations
            operations.chunked(200).forEach { chunk ->
                context.contentResolver.applyBatch(CalendarContract.AUTHORITY, ArrayList(chunk))
            }

            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun syncIfEnabled(
        context: Context,
        database: ClassBellDatabase,
        settingsRepository: SettingsRepository
    ) {
        val isEnabled = settingsRepository.syncSystemCalendarFlow.firstOrNull() ?: false
        if (isEnabled && hasCalendarPermissions(context)) {
            syncAll(context, database)
        }
    }

    fun formatAffairEventTitle(title: String, isDone: Boolean): String {
        return if (isDone) "[已完成] $title" else title
    }

    fun buildAffairDescription(affair: Affair): String = buildString {
        affair.note?.let { if (it.isNotBlank()) append("$it\n") }
        append("类别: ${affair.category.name}\n")
        if (affair.isDone) append("状态: 已完成\n")
        append("来源: Timeless 日程事项")
    }

    fun shouldHaveAlarm(affair: Affair): Boolean {
        return !affair.isDone && affair.reminder != ReminderChoice.NONE
    }

    fun getReminderMinutes(choice: ReminderChoice): Int {
        return when (choice) {
            ReminderChoice.AT_TIME -> 0
            ReminderChoice.MIN10 -> 10
            ReminderChoice.HOUR1 -> 60
            ReminderChoice.DAY1 -> 1440
            else -> 15
        }
    }
}
