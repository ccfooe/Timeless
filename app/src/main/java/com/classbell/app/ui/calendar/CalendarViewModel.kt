package com.classbell.app.ui.calendar

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
    val isEditingAffair: Boolean = false,
    val affairToEdit: Affair? = null
)

class CalendarViewModel(
    private val scheduleRepository: ScheduleRepository,
    private val affairRepository: AffairRepository,
    private val alarmScheduler: AlarmScheduler
) : ViewModel() {

    private val _selectedYearMonth = MutableStateFlow(YearMonth.now())
    val selectedYearMonth = _selectedYearMonth.asStateFlow()

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate = _selectedDate.asStateFlow()

    private val _isEditingAffair = MutableStateFlow(false)
    val isEditingAffair = _isEditingAffair.asStateFlow()

    private val _affairToEdit = MutableStateFlow<Affair?>(null)
    val affairToEdit = _affairToEdit.asStateFlow()

    val uiState: StateFlow<CalendarUiState> = combine(
        scheduleRepository.activeSemester,
        _selectedYearMonth,
        _selectedDate,
        _isEditingAffair,
        _affairToEdit
    ) { semester, yearMonth, date, isEditing, editAffair ->
        val weekNum = if (semester != null) {
            WeekCalculator.calculateWeekNumber(date, semester.week1Monday)
        } else 1

        CalendarUiState(
            selectedYearMonth = yearMonth,
            selectedDate = date,
            activeSemester = semester,
            selectedDateWeekNumber = weekNum,
            isEditingAffair = isEditing,
            affairToEdit = editAffair
        )
    }.flatMapLatest { baseState ->
        val startDate = baseState.selectedYearMonth.atDay(1)
        val endDate = baseState.selectedYearMonth.atEndOfMonth()

        combine(
            scheduleRepository.getMeetingsOnDate(baseState.selectedDate),
            affairRepository.getAffairsForDate(baseState.selectedDate),
            affairRepository.getAffairsBetweenDates(startDate, endDate)
        ) { meetings, affairsForDay, affairsInMonth ->
            baseState.copy(
                selectedDateMeetings = meetings,
                selectedDateAffairs = affairsForDay,
                allAffairsInMonth = affairsInMonth
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
        }
    }

    fun toggleAffair(affair: Affair) {
        viewModelScope.launch {
            affairRepository.updateAffair(affair.copy(isDone = !affair.isDone))
        }
    }

    fun deleteAffair(affair: Affair) {
        viewModelScope.launch {
            affairRepository.deleteAffair(affair)
        }
    }

    class Factory(
        private val scheduleRepository: ScheduleRepository,
        private val affairRepository: AffairRepository,
        private val alarmScheduler: AlarmScheduler
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CalendarViewModel(scheduleRepository, affairRepository, alarmScheduler) as T
        }
    }
}
