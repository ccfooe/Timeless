package com.classbell.app.ui.today

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.classbell.app.core.OccurrenceEngine
import com.classbell.app.core.WeekCalculator
import com.classbell.app.data.model.Affair
import com.classbell.app.data.model.Course
import com.classbell.app.data.model.MeetingOccurrence
import com.classbell.app.data.model.MeetingStatus
import com.classbell.app.data.model.PeriodSlot
import com.classbell.app.data.model.Semester
import com.classbell.app.data.model.SubPeriodKind
import com.classbell.app.data.repository.AffairRepository
import com.classbell.app.data.repository.CourseRepository
import com.classbell.app.data.repository.ScheduleRepository
import com.classbell.app.data.repository.SettingsRepository
import com.classbell.app.service.AlarmScheduler
import com.classbell.app.service.InClassService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit

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
    val holidayName: String? = null,
    val availableCourses: List<Course> = emptyList()
)

class TodayViewModel(
    private val context: Context,
    private val scheduleRepository: ScheduleRepository,
    private val courseRepository: CourseRepository,
    private val affairRepository: AffairRepository,
    private val settingsRepository: SettingsRepository,
    private val alarmScheduler: AlarmScheduler
) : ViewModel() {

    private val _currentTime = MutableStateFlow(LocalTime.now())
    val currentTime = _currentTime.asStateFlow()

    init {
        // Live second ticker for smooth progress and countdown updates
        viewModelScope.launch {
            while (isActive) {
                _currentTime.value = LocalTime.now()
                delay(1000L)
            }
        }

        // Auto schedule alarms for today with granular period slots
        viewModelScope.launch {
            val warnMin = settingsRepository.endWarnMinutesFlow.firstOrNull() ?: 5
            combine(
                scheduleRepository.getMeetingsOnDate(LocalDate.now()),
                scheduleRepository.periodSlots
            ) { meetings, slots ->
                meetings to slots
            }.collect { (meetings, slots) ->
                for (m in meetings) {
                    alarmScheduler.scheduleClassAlarms(m, warnMin, slots)
                }
            }
        }
    }

    val uiState: StateFlow<TodayUiState> = combine(
        scheduleRepository.activeSemester,
        _currentTime,
        scheduleRepository.getMeetingsOnDate(LocalDate.now()),
        scheduleRepository.periodSlots,
        scheduleRepository.manualSession,
        affairRepository.getAffairsForDate(LocalDate.now())
    ) { args: Array<Any?> ->
        val semester = args[0] as Semester?
        val time = args[1] as LocalTime
        @Suppress("UNCHECKED_CAST")
        val rawMeetings = args[2] as List<MeetingOccurrence>
        @Suppress("UNCHECKED_CAST")
        val periodSlots = args[3] as List<PeriodSlot>
        val manualSession = args[4] as MeetingOccurrence?
        @Suppress("UNCHECKED_CAST")
        val affairs = args[5] as List<Affair>

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

        // Dynamically compute subPeriodState for each meeting with the current second tick
        val meetings = rawMeetings.map { m ->
            val sub = OccurrenceEngine.resolveSubPeriodState(m.course, periodSlots, time)
            val status = when {
                m.isHolidaySuppressed -> MeetingStatus.HOLIDAY
                time.isBefore(m.startTime) -> MeetingStatus.UPCOMING
                time.isAfter(m.endTime) || time == m.endTime -> MeetingStatus.FINISHED
                else -> MeetingStatus.IN_PROGRESS
            }
            val prog = if (sub != null && (sub.kind == SubPeriodKind.IN_CLASS || sub.kind == SubPeriodKind.BREAK)) {
                sub.progress
            } else m.progress
            val rem = if (sub != null && (sub.kind == SubPeriodKind.IN_CLASS || sub.kind == SubPeriodKind.BREAK)) {
                sub.remainingMinutes
            } else m.remainingMinutes
            m.copy(status = status, progress = prog, remainingMinutes = rem, subPeriodState = sub)
        }

        // Active meeting: manual session takes precedence if set, then scheduled active meeting
        val activeMeeting = if (manualSession != null) {
            // Recalculate progress and remaining minutes with current time
            val totalSecs = Duration.between(manualSession.startTime, manualSession.endTime).seconds.coerceAtLeast(1)
            val elapsedSecs = Duration.between(manualSession.startTime, time).seconds.coerceIn(0, totalSecs)
            val progress = (elapsedSecs.toFloat() / totalSecs.toFloat()).coerceIn(0f, 1f)
            val remainingMins = ChronoUnit.MINUTES.between(time, manualSession.endTime).coerceAtLeast(0)
            manualSession.copy(progress = progress, remainingMinutes = remainingMins)
        } else {
            meetings.firstOrNull { it.status == MeetingStatus.IN_PROGRESS }
        }

        val next = meetings.firstOrNull { it.status == MeetingStatus.UPCOMING && it.startTime.isAfter(time) }
        val holidayMeeting = meetings.firstOrNull { it.isHolidaySuppressed }

        TodayUiState(
            currentDate = today,
            currentTime = time,
            activeSemester = semester,
            weekNumber = weekNum,
            semesterPhase = phase,
            todayMeetings = meetings,
            currentMeeting = activeMeeting,
            nextMeeting = next,
            todayAffairs = affairs,
            isHolidayToday = holidayMeeting != null,
            holidayName = holidayMeeting?.holidayName
        )
    }.flatMapLatest { baseState ->
        if (baseState.activeSemester != null) {
            courseRepository.getCoursesForSemester(baseState.activeSemester.id).combine(
                MutableStateFlow(baseState)
            ) { courses, state ->
                state.copy(availableCourses = courses)
            }
        } else {
            flowOf(baseState)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TodayUiState()
    )

    /**
     * Starts tracking a scheduled course routine without accumulating entire multi-hour blocks.
     * Directly respects the university's period timetable.
     */
    fun startCourseRoutine(meeting: MeetingOccurrence) {
        scheduleRepository.endManualSession()
        InClassService.start(
            context = context,
            courseName = meeting.course.name,
            className = meeting.course.className,
            room = meeting.course.room,
            startTime = meeting.startTime.toString(),
            endTime = meeting.endTime.toString(),
            periodStart = meeting.periodStart,
            periodEnd = meeting.periodEnd
        )
    }

    fun startClassNow(course: Course, durationMinutes: Int = 45) {
        val now = LocalTime.now()
        val session = scheduleRepository.startManualSession(
            course = course,
            durationMinutes = durationMinutes,
            startTime = now,
            todayDate = LocalDate.now()
        )
        InClassService.start(
            context = context,
            courseName = session.course.name,
            className = session.course.className,
            room = session.course.room,
            startTime = session.startTime.toString(),
            endTime = session.endTime.toString(),
            periodStart = session.course.periodStart,
            periodEnd = session.course.periodEnd
        )
    }

    fun endClassNow() {
        scheduleRepository.endManualSession()
        InClassService.stop(context)
    }

    fun extendClass(extraMinutes: Int = 5) {
        scheduleRepository.extendManualSession(extraMinutes)
        InClassService.extend(context, extraMinutes)
    }

    fun toggleAffair(affair: Affair) {
        viewModelScope.launch {
            affairRepository.updateAffair(affair.copy(isDone = !affair.isDone))
        }
    }

    fun addAffair(affair: Affair) {
        viewModelScope.launch {
            val newId = affairRepository.insertAffair(affair)
            if (affair.reminder != com.classbell.app.data.model.ReminderChoice.NONE && affair.time != null) {
                alarmScheduler.scheduleAffairAlarm(affair.copy(id = newId))
            }
        }
    }

    class Factory(
        private val context: Context,
        private val scheduleRepository: ScheduleRepository,
        private val courseRepository: CourseRepository,
        private val affairRepository: AffairRepository,
        private val settingsRepository: SettingsRepository,
        private val alarmScheduler: AlarmScheduler
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TodayViewModel(
                context,
                scheduleRepository,
                courseRepository,
                affairRepository,
                settingsRepository,
                alarmScheduler
            ) as T
        }
    }
}
