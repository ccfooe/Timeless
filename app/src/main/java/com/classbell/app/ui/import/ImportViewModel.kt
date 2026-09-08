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
import com.classbell.app.data.model.Semester
import com.classbell.app.data.model.WeekPattern
import com.classbell.app.data.repository.CourseRepository
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

sealed class ImportStage {
    object Idle : ImportStage()
    object Parsing : ImportStage()
    data class ConfirmTimetable(val courses: List<Course>) : ImportStage()
    data class ConfirmCalendar(val semester: Semester, val holidays: List<Holiday>) : ImportStage()
    data class Error(val message: String) : ImportStage()
    object Success : ImportStage()
}

class ImportViewModel(
    private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val courseRepository: CourseRepository,
    private val semesterRepository: SemesterRepository,
    private val scheduleRepository: ScheduleRepository
) : ViewModel() {

    private val _importStage = MutableStateFlow<ImportStage>(ImportStage.Idle)
    val importStage: StateFlow<ImportStage> = _importStage.asStateFlow()

    fun parseTimetableImage(imageUri: Uri) {
        viewModelScope.launch {
            val apiKey = settingsRepository.apiKeyFlow.firstOrNull() ?: ""
            val baseUrl = settingsRepository.baseUrlFlow.firstOrNull() ?: ""
            val model = settingsRepository.modelFlow.firstOrNull() ?: ""

            if (apiKey.isBlank()) {
                _importStage.value = ImportStage.Error(context.getString(R.string.error_no_api_key))
                return@launch
            }

            _importStage.value = ImportStage.Parsing

            try {
                val stream: InputStream? = context.contentResolver.openInputStream(imageUri)
                if (stream == null) {
                    _importStage.value = ImportStage.Error(context.getString(R.string.error_cannot_open_image))
                    return@launch
                }

                val client = LlmClient(baseUrl, apiKey, model)
                val result = client.extractTimetable(stream)

                if (result.isSuccess) {
                    val parsedCourses = result.getOrThrow()
                    val activeSemesterId = semesterRepository.getActiveSemesterSync()?.id ?: 1L

                    val converted = parsedCourses.mapIndexed { index, item ->
                        Course(
                            id = 0L,
                            semesterId = activeSemesterId,
                            name = item.name,
                            code = item.code,
                            dayOfWeek = item.dayOfWeek.coerceIn(1, 7),
                            periodStart = item.periodStart.coerceIn(1, 10),
                            periodEnd = item.periodEnd.coerceIn(item.periodStart, 10),
                            weekFrom = item.weeks.from.coerceAtLeast(1),
                            weekTo = item.weeks.to.coerceAtLeast(item.weeks.from),
                            weekPattern = when (item.weeks.pattern.lowercase()) {
                                "odd" -> WeekPattern.ODD
                                "even" -> WeekPattern.EVEN
                                else -> WeekPattern.ALL
                            },
                            campus = item.campus,
                            room = item.room,
                            className = item.className,
                            studentCount = item.studentCount,
                            nature = item.nature,
                            examType = item.examType,
                            color = com.classbell.app.ui.theme.CourseColors[index % com.classbell.app.ui.theme.CourseColors.size]
                        )
                    }

                    _importStage.value = ImportStage.ConfirmTimetable(converted)
                } else {
                    _importStage.value = ImportStage.Error(
                        result.exceptionOrNull()?.message ?: context.getString(R.string.error_parse_failed)
                    )
                }
            } catch (e: Exception) {
                _importStage.value = ImportStage.Error(
                    context.getString(R.string.error_parse_exception, e.message ?: "")
                )
            }
        }
    }

    fun parseCalendarImage(imageUri: Uri) {
        viewModelScope.launch {
            val apiKey = settingsRepository.apiKeyFlow.firstOrNull() ?: ""
            val baseUrl = settingsRepository.baseUrlFlow.firstOrNull() ?: ""
            val model = settingsRepository.modelFlow.firstOrNull() ?: ""

            if (apiKey.isBlank()) {
                _importStage.value = ImportStage.Error(context.getString(R.string.error_no_api_key))
                return@launch
            }

            _importStage.value = ImportStage.Parsing

            try {
                val stream: InputStream? = context.contentResolver.openInputStream(imageUri)
                if (stream == null) {
                    _importStage.value = ImportStage.Error(context.getString(R.string.error_cannot_open_image))
                    return@launch
                }

                val client = LlmClient(baseUrl, apiKey, model)
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
                    _importStage.value = ImportStage.Error(
                        result.exceptionOrNull()?.message ?: context.getString(R.string.error_calendar_parse_failed)
                    )
                }
            } catch (e: Exception) {
                _importStage.value = ImportStage.Error(
                    context.getString(R.string.error_calendar_parse_exception, e.message ?: "")
                )
            }
        }
    }

    fun saveConfirmedCourses(courses: List<Course>) {
        viewModelScope.launch {
            courseRepository.insertAll(courses)
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
        private val scheduleRepository: ScheduleRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ImportViewModel(
                context,
                settingsRepository,
                courseRepository,
                semesterRepository,
                scheduleRepository
            ) as T
        }
    }
}
