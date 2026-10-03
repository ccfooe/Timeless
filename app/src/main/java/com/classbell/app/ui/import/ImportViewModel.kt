package com.classbell.app.ui.import

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.classbell.app.R
import com.classbell.app.data.model.Course
import com.classbell.app.data.model.Holiday
import com.classbell.app.data.model.HolidayType
import com.classbell.app.data.model.PeriodSlot
import com.classbell.app.data.model.Semester
import com.classbell.app.data.model.WeekPattern
import com.classbell.app.data.repository.CourseRepository
import com.classbell.app.data.repository.PeriodRepository
import com.classbell.app.data.repository.ScheduleRepository
import com.classbell.app.data.repository.SemesterRepository
import com.classbell.app.data.repository.SettingsRepository
import com.classbell.app.llm.LlmClient
import com.classbell.app.llm.ParsedCalendarResult
import com.classbell.app.llm.ParsedCourseItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.io.InputStream
import java.time.LocalDate
import java.time.LocalTime

enum class ImportType {
    TIMETABLE,
    CALENDAR,
    PERIOD_TABLE
}

sealed class ImportStage {
    object Idle : ImportStage()
    data class Parsing(val type: ImportType = ImportType.TIMETABLE) : ImportStage()
    data class ConfirmTimetable(val courses: List<Course>) : ImportStage()
    data class ConfirmCalendar(val semester: Semester, val holidays: List<Holiday>) : ImportStage()
    data class ConfirmPeriodTable(val periodSlots: List<PeriodSlot>) : ImportStage()
    data class Error(val message: String, val canSwitchToGlm: Boolean = false) : ImportStage()
    object Success : ImportStage()
}

class ImportViewModel(
    private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val courseRepository: CourseRepository,
    private val semesterRepository: SemesterRepository,
    private val scheduleRepository: ScheduleRepository,
    private val periodRepository: PeriodRepository
) : ViewModel() {

    private val _importStage = MutableStateFlow<ImportStage>(ImportStage.Idle)
    val importStage: StateFlow<ImportStage> = _importStage.asStateFlow()

    var lastImageUri: Uri? = null
        private set
    var lastImportType: ImportType? = null
        private set

    private suspend fun checkCanSwitchToGlm(errorMsg: String?): Boolean {
        val model = settingsRepository.officialModelFlow.firstOrNull() ?: ""
        val isOfficial = settingsRepository.aiChannelModeFlow.firstOrNull() == SettingsRepository.MODE_OFFICIAL
        return isOfficial && (model.contains("gemini", ignoreCase = true) || (errorMsg?.contains("Google") == true))
    }

    fun switchToGlmAndRetry() {
        viewModelScope.launch {
            settingsRepository.updateOfficialModel(SettingsRepository.OFFICIAL_MODEL_GLM)
            settingsRepository.updateOfficialEndpoint(SettingsRepository.DEFAULT_OFFICIAL_ENDPOINT)
            val uri = lastImageUri
            val type = lastImportType
            if (uri != null) {
                when (type) {
                    ImportType.TIMETABLE -> parseTimetableImage(uri)
                    ImportType.CALENDAR -> parseCalendarImage(uri)
                    ImportType.PERIOD_TABLE -> parsePeriodTableImage(uri)
                    null -> parseTimetableImage(uri)
                }
            } else {
                _importStage.value = ImportStage.Idle
            }
        }
    }

    fun parseTimetableImage(imageUri: Uri) {
        lastImageUri = imageUri
        lastImportType = ImportType.TIMETABLE
        viewModelScope.launch {
            val clientResult = settingsRepository.createLlmClient()
            if (clientResult.isFailure) {
                val err = clientResult.exceptionOrNull()?.message ?: context.getString(R.string.error_no_api_key)
                _importStage.value = ImportStage.Error(err, canSwitchToGlm = checkCanSwitchToGlm(err))
                return@launch
            }
            val client = clientResult.getOrThrow()

            _importStage.value = ImportStage.Parsing(ImportType.TIMETABLE)

            try {
                val stream: InputStream? = context.contentResolver.openInputStream(imageUri)
                if (stream == null) {
                    val err = context.getString(R.string.error_cannot_open_image)
                    _importStage.value = ImportStage.Error(err, canSwitchToGlm = checkCanSwitchToGlm(err))
                    return@launch
                }

                val result = client.extractTimetable(stream)

                if (result.isSuccess) {
                    val parsedCourses = result.getOrThrow()
                    val activeSemesterId = semesterRepository.getActiveSemesterSync()?.id ?: 1L

                    val converted = parsedCourses.mapIndexed { index, item ->
                        val weekFrom = (item.weeks?.from ?: 1).coerceAtLeast(1)
                        val weekTo = (item.weeks?.to ?: 18).coerceAtLeast(weekFrom)
                        val pattern = when (item.weeks?.pattern?.lowercase()) {
                            "odd" -> WeekPattern.ODD
                            "even" -> WeekPattern.EVEN
                            else -> WeekPattern.ALL
                        }
                        Course(
                            id = 0L,
                            semesterId = activeSemesterId,
                            name = item.name.ifBlank { "未命名课程" },
                            code = item.code,
                            dayOfWeek = item.dayOfWeek.coerceIn(1, 7),
                            periodStart = item.periodStart.coerceIn(1, 10),
                            periodEnd = item.periodEnd.coerceIn(item.periodStart, 10),
                            weekFrom = weekFrom,
                            weekTo = weekTo,
                            weekPattern = pattern,
                            campus = item.campus,
                            room = item.room?.trim() ?: "",
                            className = item.className?.trim() ?: "",
                            studentCount = item.studentCount ?: 0,
                            teacher = item.teacher?.trim()?.ifEmpty { null },
                            nature = item.nature?.trim()?.ifEmpty { "必修" } ?: "必修",
                            examType = item.examType?.trim()?.ifEmpty { "考试" } ?: "考试",
                            color = com.classbell.app.ui.theme.CourseColors[index % com.classbell.app.ui.theme.CourseColors.size]
                        )
                    }

                    if (converted.isEmpty()) {
                        val err = context.getString(R.string.error_no_courses_found)
                        _importStage.value = ImportStage.Error(err, canSwitchToGlm = checkCanSwitchToGlm(err))
                        return@launch
                    }

                    _importStage.value = ImportStage.ConfirmTimetable(converted)
                } else {
                    val err = result.exceptionOrNull()?.message ?: context.getString(R.string.error_parse_failed)
                    _importStage.value = ImportStage.Error(err, canSwitchToGlm = checkCanSwitchToGlm(err))
                }
            } catch (e: Exception) {
                val err = context.getString(R.string.error_parse_exception, e.message ?: "")
                _importStage.value = ImportStage.Error(err, canSwitchToGlm = checkCanSwitchToGlm(err))
            }
        }
    }

    fun parseCalendarImage(imageUri: Uri) {
        lastImageUri = imageUri
        lastImportType = ImportType.CALENDAR
        viewModelScope.launch {
            val clientResult = settingsRepository.createLlmClient()
            if (clientResult.isFailure) {
                val err = clientResult.exceptionOrNull()?.message ?: context.getString(R.string.error_no_api_key)
                _importStage.value = ImportStage.Error(err, canSwitchToGlm = checkCanSwitchToGlm(err))
                return@launch
            }
            val client = clientResult.getOrThrow()

            _importStage.value = ImportStage.Parsing(ImportType.CALENDAR)

            try {
                val stream: InputStream? = context.contentResolver.openInputStream(imageUri)
                if (stream == null) {
                    val err = context.getString(R.string.error_cannot_open_image)
                    _importStage.value = ImportStage.Error(err, canSwitchToGlm = checkCanSwitchToGlm(err))
                    return@launch
                }

                val result = client.extractCalendar(stream)

                if (result.isSuccess) {
                    val parsed = result.getOrThrow()
                    val week1Mon = runCatching { LocalDate.parse(parsed.week1Monday) }.getOrDefault(LocalDate.of(2026, 8, 24))

                    val semester = Semester(
                        id = 0L,
                        name = parsed.semesterName.ifEmpty { context.getString(R.string.default_imported_calendar_name) },
                        week1Monday = week1Mon,
                        teachingFrom = parsed.teachingWeeks.from,
                        teachingTo = parsed.teachingWeeks.to,
                        examFrom = parsed.examWeeks?.from,
                        examTo = parsed.examWeeks?.to,
                        practiceFrom = parsed.practiceWeeks?.from,
                        practiceTo = parsed.practiceWeeks?.to,
                        notes = parsed.notes.joinToString("; "),
                        isActive = true
                    )

                    val holidays = parsed.holidays.map {
                        Holiday(
                            id = 0L,
                            semesterId = 0L,
                            name = it.name,
                            startDate = runCatching { LocalDate.parse(it.startDate) }.getOrDefault(LocalDate.now()),
                            endDate = runCatching { LocalDate.parse(it.endDate) }.getOrDefault(LocalDate.now()),
                            type = if (it.type == "makeup") HolidayType.MAKEUP else HolidayType.REST
                        )
                    }

                    _importStage.value = ImportStage.ConfirmCalendar(semester, holidays)
                } else {
                    val err = result.exceptionOrNull()?.message ?: context.getString(R.string.error_calendar_parse_failed)
                    _importStage.value = ImportStage.Error(err, canSwitchToGlm = checkCanSwitchToGlm(err))
                }
            } catch (e: Exception) {
                val err = context.getString(R.string.error_calendar_parse_exception, e.message ?: "")
                _importStage.value = ImportStage.Error(err, canSwitchToGlm = checkCanSwitchToGlm(err))
            }
        }
    }

    fun saveConfirmedCourses(courses: List<Course>) {
        viewModelScope.launch {
            courseRepository.insertAll(courses)
            context?.let { ctx ->
                val app = ctx.applicationContext as? com.classbell.app.ClassBellApp
                app?.database?.let { db ->
                    com.classbell.app.core.CalendarSyncManager.syncIfEnabled(ctx, db, settingsRepository)
                }
            }
            _importStage.value = ImportStage.Success
        }
    }

    fun saveConfirmedCalendar(semester: Semester, holidays: List<Holiday>) {
        viewModelScope.launch {
            val semId = semesterRepository.insertSemester(semester)
            val updatedHolidays = holidays.map { it.copy(semesterId = semId) }
            scheduleRepository.insertHoliday(updatedHolidays.firstOrNull() ?: return@launch)
            for (h in updatedHolidays.drop(1)) {
                scheduleRepository.insertHoliday(h)
            }
            context?.let { ctx ->
                val app = ctx.applicationContext as? com.classbell.app.ClassBellApp
                app?.database?.let { db ->
                    com.classbell.app.core.CalendarSyncManager.syncIfEnabled(ctx, db, settingsRepository)
                }
            }
            _importStage.value = ImportStage.Success
        }
    }

    fun parsePeriodTableImage(imageUri: Uri) {
        lastImageUri = imageUri
        lastImportType = ImportType.PERIOD_TABLE
        viewModelScope.launch {
            val clientResult = settingsRepository.createLlmClient()
            if (clientResult.isFailure) {
                val err = clientResult.exceptionOrNull()?.message ?: context.getString(R.string.error_no_api_key)
                _importStage.value = ImportStage.Error(err, canSwitchToGlm = checkCanSwitchToGlm(err))
                return@launch
            }
            val client = clientResult.getOrThrow()

            _importStage.value = ImportStage.Parsing(ImportType.PERIOD_TABLE)

            try {
                val stream: InputStream? = context.contentResolver.openInputStream(imageUri)
                if (stream == null) {
                    val err = context.getString(R.string.error_cannot_open_image)
                    _importStage.value = ImportStage.Error(err, canSwitchToGlm = checkCanSwitchToGlm(err))
                    return@launch
                }

                val result = client.extractPeriodTable(stream)

                if (result.isSuccess) {
                    val parsedSlots = result.getOrThrow()
                    if (parsedSlots.isEmpty()) {
                        val err = "未在图片中识别到作息时间数据，请确保作息时间表清晰可见"
                        _importStage.value = ImportStage.Error(err, canSwitchToGlm = checkCanSwitchToGlm(err))
                        return@launch
                    }
                    val converted = parsedSlots.map { item ->
                        val parsedStart = runCatching { LocalTime.parse(item.startTime.trim()) }.getOrDefault(LocalTime.of(8, 30))
                        val parsedEnd = runCatching { LocalTime.parse(item.endTime.trim()) }.getOrDefault(LocalTime.of(9, 15))
                        PeriodSlot(
                            id = 0L,
                            order = item.order,
                            label = item.label?.ifBlank { "第${item.order}节" } ?: "第${item.order}节",
                            startTime = parsedStart,
                            endTime = parsedEnd
                        )
                    }.sortedBy { it.order }

                    _importStage.value = ImportStage.ConfirmPeriodTable(converted)
                } else {
                    val err = result.exceptionOrNull()?.message ?: context.getString(R.string.error_parse_failed)
                    _importStage.value = ImportStage.Error(err, canSwitchToGlm = checkCanSwitchToGlm(err))
                }
            } catch (e: Exception) {
                val err = e.message ?: context.getString(R.string.error_parse_failed)
                _importStage.value = ImportStage.Error(err, canSwitchToGlm = checkCanSwitchToGlm(err))
            }
        }
    }

    fun saveConfirmedPeriodTable(slots: List<PeriodSlot>) {
        viewModelScope.launch {
            if (slots.isNotEmpty()) {
                periodRepository.replaceAll(slots)
            }
            context?.let { ctx ->
                val app = ctx.applicationContext as? com.classbell.app.ClassBellApp
                app?.database?.let { db ->
                    com.classbell.app.core.CalendarSyncManager.syncIfEnabled(ctx, db, settingsRepository)
                }
            }
            _importStage.value = ImportStage.Success
        }
    }

    fun resetStage() {
        _importStage.value = ImportStage.Idle
    }

    class Factory(
        private val context: Context,
        private val settingsRepository: SettingsRepository,
        private val courseRepository: CourseRepository,
        private val semesterRepository: SemesterRepository,
        private val scheduleRepository: ScheduleRepository,
        private val periodRepository: PeriodRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ImportViewModel(
                context,
                settingsRepository,
                courseRepository,
                semesterRepository,
                scheduleRepository,
                periodRepository
            ) as T
        }
    }
}
