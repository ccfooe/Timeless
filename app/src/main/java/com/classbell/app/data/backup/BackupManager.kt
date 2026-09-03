package com.classbell.app.data.backup

import com.classbell.app.data.local.ClassBellDatabase
import com.classbell.app.data.model.BackupData
import com.classbell.app.data.seed.SeedDataProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

enum class ImportMode {
    REPLACE,
    MERGE
}

class BackupManager(private val database: ClassBellDatabase) {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    suspend fun exportToJson(): String = withContext(Dispatchers.IO) {
        val semesters = database.semesterDao().getActiveSemesterSync()?.let { listOf(it) }
            ?: SeedDataProvider.getDefaultSemester().let { listOf(it) }
        val activeSemId = semesters.firstOrNull()?.id ?: 1L
        val courses = database.courseDao().getCoursesForSemesterSync(activeSemId)
        val periodSlots = database.periodSlotDao().getAllPeriodSlotsSync()
        val holidays = database.holidayDao().getHolidaysForSemesterSync(activeSemId)
        val affairs = database.affairDao().getAllAffairsSync()
        val overrides = database.overrideDao().getOverridesForSemesterSync(activeSemId)

        val backupData = BackupData(
            schemaVersion = 1,
            exportedAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            semesters = semesters,
            courses = courses,
            periodSlots = periodSlots,
            holidays = holidays,
            affairs = affairs,
            overrides = overrides
        )

        json.encodeToString(backupData)
    }

    suspend fun importFromJson(jsonString: String, mode: ImportMode): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val backupData = json.decodeFromString<BackupData>(jsonString)

            if (mode == ImportMode.REPLACE) {
                database.semesterDao().deleteAll()
                database.courseDao().deleteAll()
                database.periodSlotDao().deleteAll()
                database.holidayDao().deleteAll()
                database.affairDao().deleteAll()
                database.overrideDao().deleteAll()
            }

            if (backupData.semesters.isNotEmpty()) {
                database.semesterDao().insertAll(backupData.semesters)
            }
            if (backupData.periodSlots.isNotEmpty()) {
                database.periodSlotDao().insertAll(backupData.periodSlots)
            }
            if (backupData.courses.isNotEmpty()) {
                database.courseDao().insertAll(backupData.courses)
            }
            if (backupData.holidays.isNotEmpty()) {
                database.holidayDao().insertAll(backupData.holidays)
            }
            if (backupData.affairs.isNotEmpty()) {
                database.affairDao().insertAll(backupData.affairs)
            }
            if (backupData.overrides.isNotEmpty()) {
                database.overrideDao().insertAll(backupData.overrides)
            }
        }
    }

    suspend fun clearAllData(): Unit = withContext(Dispatchers.IO) {
        database.semesterDao().deleteAll()
        database.courseDao().deleteAll()
        database.periodSlotDao().deleteAll()
        database.holidayDao().deleteAll()
        database.affairDao().deleteAll()
        database.overrideDao().deleteAll()
    }
}
