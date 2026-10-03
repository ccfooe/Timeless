package com.classbell.app.ui.calendar

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.classbell.app.core.OccurrenceEngine
import com.classbell.app.core.WeekCalculator
import com.classbell.app.data.model.Affair
import com.classbell.app.data.model.Holiday
import com.classbell.app.data.model.MeetingOccurrence
import com.classbell.app.data.model.Semester
import com.classbell.app.data.repository.AffairRepository
import com.classbell.app.data.repository.ScheduleRepository
import com.classbell.app.data.repository.SettingsRepository
import com.classbell.app.llm.ParsedAffair
import com.classbell.app.service.AlarmScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

data class CalendarUiState(
    val selectedYearMonth: YearMonth = YearMonth.now(),
    val selectedDate: LocalDate = LocalDate.now(),
    val activeSemester: Semester? = null,
    val selectedDateWeekNumber: Int = 1,
    val selectedDateMeetings: List<MeetingOccurrence> = emptyList(),
    val selectedDateAffairs: List<Affair> = emptyList(),
    val holidaysInMonth: List<Holiday> = emptyList(),
    val allAffairsInMonth: List<Affair> = emptyList(),
    val overridesInSemester: List<com.classbell.app.data.model.Override> = emptyList(),
    val holidaysInSemester: List<Holiday> = emptyList(),
    val allCoursesInSemester: List<com.classbell.app.data.model.Course> = emptyList(),
    val isEditingAffair: Boolean = false,
    val affairToEdit: Affair? = null,
    val isEditingShift: Boolean = false,
    val autoTimezoneConversion: Boolean = false
)

class CalendarViewModel(
    private val context: Context? = null,
    private val scheduleRepository: ScheduleRepository,
    private val affairRepository: AffairRepository,
    private val settingsRepository: SettingsRepository,
    private val alarmScheduler: AlarmScheduler
) : ViewModel() {

    constructor(
        scheduleRepository: ScheduleRepository,
        affairRepository: AffairRepository,
        settingsRepository: SettingsRepository,
        alarmScheduler: AlarmScheduler
    ) : this(null, scheduleRepository, affairRepository, settingsRepository, alarmScheduler)

    private val _selectedYearMonth = MutableStateFlow(YearMonth.now())
    val selectedYearMonth = _selectedYearMonth.asStateFlow()

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate = _selectedDate.asStateFlow()

    private val _isEditingAffair = MutableStateFlow(false)
    val isEditingAffair = _isEditingAffair.asStateFlow()

    private val _affairToEdit = MutableStateFlow<Affair?>(null)
    val affairToEdit = _affairToEdit.asStateFlow()

    private val _isEditingShift = MutableStateFlow(false)
    val isEditingShift = _isEditingShift.asStateFlow()

    val uiState: StateFlow<CalendarUiState> = combine(
        scheduleRepository.activeSemester,
        _selectedYearMonth,
        _selectedDate,
        _isEditingAffair,
        _affairToEdit,
        _isEditingShift
    ) { args: Array<Any?> ->
        val semester = args[0] as Semester?
        val yearMonth = args[1] as YearMonth
        val date = args[2] as LocalDate
        val isEditing = args[3] as Boolean
        val editAffair = args[4] as Affair?
        val isShift = args[5] as Boolean

        val weekNum = if (semester != null) {
            WeekCalculator.calculateWeekNumber(date, semester.week1Monday)
        } else 1

        CalendarUiState(
            selectedYearMonth = yearMonth,
            selectedDate = date,
            activeSemester = semester,
            selectedDateWeekNumber = weekNum,
            isEditingAffair = isEditing,
            affairToEdit = editAffair,
            isEditingShift = isShift
        )
    }.flatMapLatest { baseState ->
        val semId = baseState.activeSemester?.id ?: 0L
        val startDate = baseState.selectedYearMonth.atDay(1).minusDays(2)
        val endDate = baseState.selectedYearMonth.atEndOfMonth().plusDays(2)

        combine(
            scheduleRepository.getMeetingsOnDate(baseState.selectedDate),
            affairRepository.getAffairsBetweenDates(startDate, endDate),
            settingsRepository.autoTimezoneConversionFlow,
            scheduleRepository.getOverridesForSemester(semId),
            scheduleRepository.getHolidaysForSemester(semId),
            scheduleRepository.getCoursesForSemester(semId)
        ) { args: Array<Any?> ->
            @Suppress("UNCHECKED_CAST")
            val meetings = args[0] as List<MeetingOccurrence>
            @Suppress("UNCHECKED_CAST")
            val affairsRange = args[1] as List<Affair>
            val autoTz = args[2] as Boolean
            @Suppress("UNCHECKED_CAST")
            val overrides = args[3] as List<com.classbell.app.data.model.Override>
            @Suppress("UNCHECKED_CAST")
            val holidays = args[4] as List<Holiday>
            @Suppress("UNCHECKED_CAST")
            val courses = args[5] as List<com.classbell.app.data.model.Course>

            val dayAffairs = affairsRange.filter { affair ->
                val conv = com.classbell.app.core.TimeUtils.convertAffairDateTime(affair.date, affair.time, autoTz)
                conv.effectiveDate == baseState.selectedDate
            }
            baseState.copy(
                selectedDateMeetings = meetings,
                selectedDateAffairs = dayAffairs,
                allAffairsInMonth = affairsRange,
                autoTimezoneConversion = autoTz,
                overridesInSemester = overrides,
                holidaysInSemester = holidays,
                allCoursesInSemester = courses
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CalendarUiState()
    )

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
        if (YearMonth.from(date) != _selectedYearMonth.value) {
            _selectedYearMonth.value = YearMonth.from(date)
        }
    }

    fun nextMonth() {
        _selectedYearMonth.value = _selectedYearMonth.value.plusMonths(1)
    }

    fun previousMonth() {
        _selectedYearMonth.value = _selectedYearMonth.value.minusMonths(1)
    }

    fun openAddAffair() {
        _affairToEdit.value = null
        _isEditingAffair.value = true
    }

    fun openShiftDialog() {
        _isEditingShift.value = true
    }

    fun closeShiftDialog() {
        _isEditingShift.value = false
    }

    fun saveWholeDayShift(date: LocalDate, followsWeekday: Int, note: String?) {
        viewModelScope.launch {
            val semId = uiState.value.activeSemester?.id ?: 1L
            scheduleRepository.clearOverridesForDate(semId, date)
            scheduleRepository.insertOverride(
                com.classbell.app.data.model.Override(
                    semesterId = semId,
                    date = date,
                    kind = com.classbell.app.data.model.OverrideKind.SHIFT,
                    followsWeekday = followsWeekday,
                    note = note
                )
            )
            context?.let { ctx ->
                val app = ctx.applicationContext as? com.classbell.app.ClassBellApp
                app?.database?.let { db ->
                    com.classbell.app.core.CalendarSyncManager.syncIfEnabled(ctx, db, settingsRepository)
                }
            }
        }
    }

    fun saveWholeDayCancel(date: LocalDate, note: String?) {
        viewModelScope.launch {
            val semId = uiState.value.activeSemester?.id ?: 1L
            scheduleRepository.clearOverridesForDate(semId, date)
            scheduleRepository.insertOverride(
                com.classbell.app.data.model.Override(
                    semesterId = semId,
                    date = date,
                    kind = com.classbell.app.data.model.OverrideKind.CANCEL,
                    note = note
                )
            )
            context?.let { ctx ->
                val app = ctx.applicationContext as? com.classbell.app.ClassBellApp
                app?.database?.let { db ->
                    com.classbell.app.core.CalendarSyncManager.syncIfEnabled(ctx, db, settingsRepository)
                }
            }
        }
    }

    fun saveSingleAdjustment(date: LocalDate, courseId: Long, pStart: Int, pEnd: Int, room: String?, note: String?) {
        viewModelScope.launch {
            val semId = uiState.value.activeSemester?.id ?: 1L
            scheduleRepository.insertOverride(
                com.classbell.app.data.model.Override(
                    semesterId = semId,
                    date = date,
                    kind = com.classbell.app.data.model.OverrideKind.COURSE_ADJUST,
                    courseId = courseId,
                    targetPeriodStart = pStart,
                    targetPeriodEnd = pEnd,
                    targetRoom = room,
                    note = note
                )
            )
            context?.let { ctx ->
                val app = ctx.applicationContext as? com.classbell.app.ClassBellApp
                app?.database?.let { db ->
                    com.classbell.app.core.CalendarSyncManager.syncIfEnabled(ctx, db, settingsRepository)
                }
            }
        }
    }

    fun saveSingleCancel(date: LocalDate, courseId: Long, note: String?) {
        viewModelScope.launch {
            val semId = uiState.value.activeSemester?.id ?: 1L
            scheduleRepository.insertOverride(
                com.classbell.app.data.model.Override(
                    semesterId = semId,
                    date = date,
                    kind = com.classbell.app.data.model.OverrideKind.CANCEL,
                    courseId = courseId,
                    note = note
                )
            )
            context?.let { ctx ->
                val app = ctx.applicationContext as? com.classbell.app.ClassBellApp
                app?.database?.let { db ->
                    com.classbell.app.core.CalendarSyncManager.syncIfEnabled(ctx, db, settingsRepository)
                }
            }
        }
    }

    fun clearOverridesForDate(date: LocalDate) {
        viewModelScope.launch {
            val semId = uiState.value.activeSemester?.id ?: 1L
            scheduleRepository.clearOverridesForDate(semId, date)
            context?.let { ctx ->
                val app = ctx.applicationContext as? com.classbell.app.ClassBellApp
                app?.database?.let { db ->
                    com.classbell.app.core.CalendarSyncManager.syncIfEnabled(ctx, db, settingsRepository)
                }
            }
        }
    }

    fun openEditAffair(affair: Affair) {
        _affairToEdit.value = affair
        _isEditingAffair.value = true
    }

    fun closeEditAffair() {
        _isEditingAffair.value = false
        _affairToEdit.value = null
    }

    fun saveAffair(affair: Affair) {
        viewModelScope.launch {
            if (affair.id == 0L) {
                val newId = affairRepository.insertAffair(affair)
                alarmScheduler.scheduleAffairAlarm(affair.copy(id = newId))
            } else {
                affairRepository.updateAffair(affair)
                alarmScheduler.scheduleAffairAlarm(affair)
            }
            closeEditAffair()
            context?.let { ctx ->
                val app = ctx.applicationContext as? com.classbell.app.ClassBellApp
                app?.database?.let { db ->
                    com.classbell.app.core.CalendarSyncManager.syncIfEnabled(ctx, db, settingsRepository)
                }
            }
        }
    }

    fun toggleAffair(affair: Affair) {
        viewModelScope.launch {
            val updated = affair.copy(isDone = !affair.isDone)
            affairRepository.updateAffair(updated)
            if (updated.isDone) {
                alarmScheduler.cancelAffairAlarm(updated.id)
            } else {
                alarmScheduler.scheduleAffairAlarm(updated)
            }
            context?.let { ctx ->
                val app = ctx.applicationContext as? com.classbell.app.ClassBellApp
                app?.database?.let { db ->
                    com.classbell.app.core.CalendarSyncManager.syncIfEnabled(ctx, db, settingsRepository)
                }
            }
        }
    }

    fun deleteAffair(affair: Affair) {
        viewModelScope.launch {
            alarmScheduler.cancelAffairAlarm(affair.id)
            affairRepository.deleteAffair(affair)
            context?.let { ctx ->
                val app = ctx.applicationContext as? com.classbell.app.ClassBellApp
                app?.database?.let { db ->
                    com.classbell.app.core.CalendarSyncManager.syncIfEnabled(ctx, db, settingsRepository)
                }
            }
        }
    }

    suspend fun parseAffair(text: String): Result<ParsedAffair> {
        val fastResult = com.classbell.app.core.FastLocalParser.tryParse(text, LocalDate.now())
        if (fastResult != null && fastResult.affairs.isNotEmpty()) {
            return Result.success(fastResult.affairs.first())
        }
        val clientResult = settingsRepository.createLlmClient()
        if (clientResult.isFailure) {
            return Result.failure(clientResult.exceptionOrNull() ?: Exception("无法获取 AI 配置"))
        }
        val client = clientResult.getOrThrow()
        return client.extractAffair(text, LocalDate.now())
    }

    class Factory(
        private val context: Context? = null,
        private val scheduleRepository: ScheduleRepository,
        private val affairRepository: AffairRepository,
        private val settingsRepository: SettingsRepository,
        private val alarmScheduler: AlarmScheduler
    ) : ViewModelProvider.Factory {

        constructor(
            scheduleRepository: ScheduleRepository,
            affairRepository: AffairRepository,
            settingsRepository: SettingsRepository,
            alarmScheduler: AlarmScheduler
        ) : this(null, scheduleRepository, affairRepository, settingsRepository, alarmScheduler)

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CalendarViewModel(context, scheduleRepository, affairRepository, settingsRepository, alarmScheduler) as T
        }
    }
}
