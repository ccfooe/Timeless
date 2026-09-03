package com.classbell.app.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.classbell.app.core.WeekCalculator
import com.classbell.app.data.model.Affair
import com.classbell.app.data.model.MeetingOccurrence
import com.classbell.app.data.model.MeetingStatus
import com.classbell.app.data.model.Semester
import com.classbell.app.data.repository.AffairRepository
import com.classbell.app.data.repository.ScheduleRepository
import com.classbell.app.data.repository.SettingsRepository
import com.classbell.app.service.AlarmScheduler
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

data class TodayUiState(
    val currentDate: LocalDate = LocalDate.now(),
    val currentTime: LocalTime = LocalTime.now(),
    val activeSemester: Semester? = null,
    val weekNumber: Int = 1,
    val semesterPhase: String = "教学周",
    val todayMeetings: List<MeetingOccurrence> = emptyList(),
    val currentMeeting: MeetingOccurrence? = null,
    val nextMeeting: MeetingOccurrence? = null,
    val todayAffairs: List<Affair> = emptyList(),
    val isHolidayToday: Boolean = false,
    val holidayName: String? = null
)

class TodayViewModel(
    private val scheduleRepository: ScheduleRepository,
    private val affairRepository: AffairRepository,
    private val settingsRepository: SettingsRepository,
    private val alarmScheduler: AlarmScheduler
) : ViewModel() {

    private val _currentTime = MutableStateFlow(LocalTime.now())
    val currentTime = _currentTime.asStateFlow()

    init {
        // Live second ticker for smooth progress and time updates
        viewModelScope.launch {
            while (isActive) {
                _currentTime.value = LocalTime.now()
                delay(1000L)
            }
        }

        // Auto schedule alarms for today
        viewModelScope.launch {
            val warnMin = settingsRepository.endWarnMinutesFlow.firstOrNull() ?: 5
            scheduleRepository.getMeetingsOnDate(LocalDate.now()).collect { meetings ->
                for (m in meetings) {
                    alarmScheduler.scheduleClassAlarms(m, warnMin)
                }
            }
        }
    }

    val uiState: StateFlow<TodayUiState> = combine(
        scheduleRepository.activeSemester,
        _currentTime,
        scheduleRepository.getMeetingsOnDate(LocalDate.now()),
        affairRepository.getAffairsForDate(LocalDate.now())
    ) { semester, time, meetings, affairs ->
        val today = LocalDate.now()
        val weekNum = if (semester != null) {
            WeekCalculator.calculateWeekNumber(today, semester.week1Monday)
        } else 1

        val phase = if (semester != null) {
            WeekCalculator.getSemesterPhase(
                weekNum,
                semester.teachingFrom,
                semester.teachingTo,
                semester.examFrom,
                semester.examTo,
                semester.practiceFrom,
                semester.practiceTo
            )
        } else "教学周"

        val current = meetings.firstOrNull { it.status == MeetingStatus.IN_PROGRESS }
        val next = meetings.firstOrNull { it.status == MeetingStatus.UPCOMING && it.startTime.isAfter(time) }
        val holidayMeeting = meetings.firstOrNull { it.isHolidaySuppressed }

        TodayUiState(
            currentDate = today,
            currentTime = time,
            activeSemester = semester,
            weekNumber = weekNum,
            semesterPhase = phase,
            todayMeetings = meetings,
            currentMeeting = current,
            nextMeeting = next,
            todayAffairs = affairs,
            isHolidayToday = holidayMeeting != null,
            holidayName = holidayMeeting?.holidayName
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TodayUiState()
    )

    class Factory(
        private val scheduleRepository: ScheduleRepository,
        private val affairRepository: AffairRepository,
        private val settingsRepository: SettingsRepository,
        private val alarmScheduler: AlarmScheduler
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TodayViewModel(
                scheduleRepository,
                affairRepository,
                settingsRepository,
                alarmScheduler
            ) as T
        }
    }
}
