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
import com.classbell.app.llm.ParsedAffair
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

data class SessionSuppression(
    val date: LocalDate = LocalDate.now(),
    val suppressedPeriodSlots: Set<Int> = emptySet(),
    val suppressedCourses: Set<Long> = emptySet(),
    val suppressMorning: Boolean = false,
    val suppressAfternoon: Boolean = false,
    val suppressFullDay: Boolean = false
) {
    fun isSuppressed(meeting: MeetingOccurrence, currentSlotOrder: Int?): Boolean {
        if (date != meeting.date) return false
        if (suppressFullDay) return true
        if (suppressMorning && meeting.startTime.isBefore(LocalTime.of(13, 0))) return true
        if (suppressAfternoon && meeting.startTime.isAfter(LocalTime.of(12, 30))) return true
        if (suppressedCourses.contains(meeting.course.id)) return true
        if (currentSlotOrder != null && suppressedPeriodSlots.contains(currentSlotOrder)) return true
        return false
    }

    val isAnySuppressed: Boolean
        get() = suppressFullDay || suppressMorning || suppressAfternoon || suppressedPeriodSlots.isNotEmpty() || suppressedCourses.isNotEmpty()
}

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
    val availableCourses: List<Course> = emptyList(),
    val todayOverrides: List<com.classbell.app.data.model.Override> = emptyList(),
    val allSemesterCourses: List<Course> = emptyList(),
    val userRole: String = SettingsRepository.ROLE_TEACHER,
    val autoTimezoneConversion: Boolean = false,
    val sessionSuppression: SessionSuppression = SessionSuppression()
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

    private val _activePreClassCourseId = MutableStateFlow<Long?>(null)
    private val _sessionSuppression = MutableStateFlow(SessionSuppression())

    val uiState: StateFlow<TodayUiState> = combine(
        scheduleRepository.activeSemester,
        _currentTime,
        scheduleRepository.getMeetingsOnDate(LocalDate.now()),
        scheduleRepository.periodSlots,
        scheduleRepository.manualSession,
        affairRepository.getAffairsBetweenDates(LocalDate.now().minusDays(1), LocalDate.now().plusDays(1)),
        _activePreClassCourseId,
        settingsRepository.userRoleFlow,
        settingsRepository.autoTimezoneConversionFlow,
        _sessionSuppression
    ) { args: Array<Any?> ->
        val semester = args[0] as Semester?
        val time = args[1] as LocalTime
        @Suppress("UNCHECKED_CAST")
        val rawMeetings = args[2] as List<MeetingOccurrence>
        @Suppress("UNCHECKED_CAST")
        val periodSlots = args[3] as List<PeriodSlot>
        val manualSession = args[4] as MeetingOccurrence?
        @Suppress("UNCHECKED_CAST")
        val affairsRange = args[5] as List<Affair>
        val preClassCourseId = args[6] as Long?
        val userRole = (args[7] as? String) ?: SettingsRepository.ROLE_TEACHER
        val autoTz = (args[8] as? Boolean) ?: false
        val rawSuppression = (args[9] as? SessionSuppression) ?: SessionSuppression()

        val today = LocalDate.now()
        val effectiveSuppression = if (rawSuppression.date == today) rawSuppression else SessionSuppression(date = today)

        val affairs = affairsRange.filter { affair ->
            val conv = com.classbell.app.core.TimeUtils.convertAffairDateTime(affair.date, affair.time, autoTz)
            conv.effectiveDate == today
        }
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
            val isSuppressed = effectiveSuppression.isSuppressed(m, sub?.periodOrder)
            val status = when {
                m.isHolidaySuppressed -> MeetingStatus.HOLIDAY
                isSuppressed -> MeetingStatus.FINISHED
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

        // Active meeting: manual session takes precedence if set, then scheduled in-progress, then pre-class routine
        val inProgressMeeting = meetings.firstOrNull { it.status == MeetingStatus.IN_PROGRESS }
        val activeMeeting = if (manualSession != null) {
            if (effectiveSuppression.suppressFullDay) null
            else {
                // Recalculate progress and remaining minutes with current time
                val totalSecs = Duration.between(manualSession.startTime, manualSession.endTime).seconds.coerceAtLeast(1)
                val elapsedSecs = Duration.between(manualSession.startTime, time).seconds.coerceIn(0, totalSecs)
                val progress = (elapsedSecs.toFloat() / totalSecs.toFloat()).coerceIn(0f, 1f)
                val remainingMins = ChronoUnit.MINUTES.between(time, manualSession.endTime).coerceAtLeast(0)
                manualSession.copy(progress = progress, remainingMinutes = remainingMins)
            }
        } else if (inProgressMeeting != null) {
            inProgressMeeting
        } else if (preClassCourseId != null) {
            val preMeeting = meetings.firstOrNull { it.course.id == preClassCourseId }
            if (preMeeting != null && !effectiveSuppression.isSuppressed(preMeeting, null) && time.isBefore(preMeeting.startTime)) {
                val sub = OccurrenceEngine.resolveSubPeriodState(preMeeting.course, periodSlots, time)
                preMeeting.copy(status = MeetingStatus.UPCOMING, subPeriodState = sub)
            } else {
                null
            }
        } else {
            null
        }

        val next = meetings.firstOrNull { it.status == MeetingStatus.UPCOMING && it.startTime.isAfter(time) && it != activeMeeting }
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
            holidayName = holidayMeeting?.holidayName,
            userRole = userRole,
            autoTimezoneConversion = autoTz,
            sessionSuppression = effectiveSuppression
        )
    }.flatMapLatest { baseState ->
        val semId = baseState.activeSemester?.id
        if (semId != null) {
            combine(
                courseRepository.getCoursesForSemester(semId),
                scheduleRepository.getOverridesForSemester(semId)
            ) { courses, overrides ->
                val dateOverrides = overrides.filter { it.date == baseState.currentDate }
                baseState.copy(
                    availableCourses = courses,
                    todayOverrides = dateOverrides,
                    allSemesterCourses = courses
                )
            }
        } else {
            flowOf(baseState)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TodayUiState()
    )

    init {
        // Auto reconcile InClassService with currently active class state
        viewModelScope.launch {
            uiState.collect { state ->
                val meeting = state.currentMeeting
                val suppression = state.sessionSuppression
                if (meeting != null && !meeting.isHolidaySuppressed) {
                    val sub = meeting.subPeriodState
                    val isSuppressed = suppression.isSuppressed(meeting, sub?.periodOrder)
                    if (!isSuppressed && sub != null && sub.kind != com.classbell.app.data.model.SubPeriodKind.FINISHED) {
                        val role = state.userRole
                        val displayClass = if (role == SettingsRepository.ROLE_STUDENT) {
                            meeting.course.teacher?.takeIf { it.isNotBlank() } ?: meeting.course.className
                        } else {
                            meeting.course.className
                        }
                        InClassService.reconcile(
                            context = context,
                            courseName = meeting.course.name,
                            className = displayClass,
                            room = meeting.course.room,
                            startTime = meeting.startTime.toString(),
                            endTime = meeting.endTime.toString(),
                            periodStart = meeting.periodStart,
                            periodEnd = meeting.periodEnd,
                            isManualSession = meeting.course.id < 0
                        )
                    } else if (isSuppressed && InClassService.isRunning) {
                        InClassService.stop(context)
                    }
                }
            }
        }
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
            val app = context.applicationContext as? com.classbell.app.ClassBellApp
            app?.database?.let { db ->
                com.classbell.app.core.CalendarSyncManager.syncIfEnabled(context, db, settingsRepository)
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
            val app = context.applicationContext as? com.classbell.app.ClassBellApp
            app?.database?.let { db ->
                com.classbell.app.core.CalendarSyncManager.syncIfEnabled(context, db, settingsRepository)
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
            val app = context.applicationContext as? com.classbell.app.ClassBellApp
            app?.database?.let { db ->
                com.classbell.app.core.CalendarSyncManager.syncIfEnabled(context, db, settingsRepository)
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
            val app = context.applicationContext as? com.classbell.app.ClassBellApp
            app?.database?.let { db ->
                com.classbell.app.core.CalendarSyncManager.syncIfEnabled(context, db, settingsRepository)
            }
        }
    }

    fun clearOverridesForDate(date: LocalDate) {
        viewModelScope.launch {
            val semId = uiState.value.activeSemester?.id ?: 1L
            scheduleRepository.clearOverridesForDate(semId, date)
            val app = context.applicationContext as? com.classbell.app.ClassBellApp
            app?.database?.let { db ->
                com.classbell.app.core.CalendarSyncManager.syncIfEnabled(context, db, settingsRepository)
            }
        }
    }

    /**
     * Starts tracking a scheduled course routine without accumulating entire multi-hour blocks.
     * Directly respects the university's period timetable.
     */
    fun startCourseRoutine(meeting: MeetingOccurrence) {
        scheduleRepository.endManualSession()
        _activePreClassCourseId.value = meeting.course.id
        val role = uiState.value.userRole
        val displayClass = if (role == SettingsRepository.ROLE_STUDENT) {
            meeting.course.teacher?.takeIf { it.isNotBlank() } ?: meeting.course.className
        } else {
            meeting.course.className
        }
        InClassService.start(
            context = context,
            courseName = meeting.course.name,
            className = displayClass,
            room = meeting.course.room,
            startTime = meeting.startTime.toString(),
            endTime = meeting.endTime.toString(),
            periodStart = meeting.periodStart,
            periodEnd = meeting.periodEnd
        )
    }

    fun startClassNow(course: Course, durationMinutes: Int = 45) {
        _activePreClassCourseId.value = null
        val now = LocalTime.now()
        val session = scheduleRepository.startManualSession(
            course = course,
            durationMinutes = durationMinutes,
            startTime = now,
            todayDate = LocalDate.now()
        )
        val role = uiState.value.userRole
        val displayClass = if (role == SettingsRepository.ROLE_STUDENT) {
            session.course.teacher?.takeIf { it.isNotBlank() } ?: session.course.className
        } else {
            session.course.className
        }
        InClassService.start(
            context = context,
            courseName = session.course.name,
            className = displayClass,
            room = session.course.room,
            startTime = session.startTime.toString(),
            endTime = session.endTime.toString(),
            periodStart = session.course.periodStart,
            periodEnd = session.course.periodEnd,
            isManualSession = true
        )
    }

    fun endClassNow() {
        dismissCurrentPeriod()
    }

    fun dismissCurrentPeriod() {
        val cur = uiState.value.currentMeeting
        val currentSlot = cur?.subPeriodState?.periodOrder
        _activePreClassCourseId.value = null
        scheduleRepository.endManualSession()
        InClassService.stop(context)

        val today = LocalDate.now()
        val curSupp = if (_sessionSuppression.value.date == today) _sessionSuppression.value else SessionSuppression(date = today)
        if (currentSlot != null) {
            _sessionSuppression.value = curSupp.copy(
                suppressedPeriodSlots = curSupp.suppressedPeriodSlots + currentSlot
            )
        } else if (cur != null) {
            _sessionSuppression.value = curSupp.copy(
                suppressedCourses = curSupp.suppressedCourses + cur.course.id
            )
        }
    }

    fun dismissCurrentHalfDay() {
        _activePreClassCourseId.value = null
        scheduleRepository.endManualSession()
        InClassService.stop(context)

        val today = LocalDate.now()
        val nowTime = LocalTime.now()
        val curSupp = if (_sessionSuppression.value.date == today) _sessionSuppression.value else SessionSuppression(date = today)
        if (nowTime.isBefore(LocalTime.of(13, 0))) {
            _sessionSuppression.value = curSupp.copy(suppressMorning = true)
        } else {
            _sessionSuppression.value = curSupp.copy(suppressAfternoon = true)
        }
    }

    fun dismissFullDay() {
        _activePreClassCourseId.value = null
        scheduleRepository.endManualSession()
        InClassService.stop(context)

        val today = LocalDate.now()
        val curSupp = if (_sessionSuppression.value.date == today) _sessionSuppression.value else SessionSuppression(date = today)
        _sessionSuppression.value = curSupp.copy(suppressFullDay = true)
    }

    fun restoreTiming() {
        _sessionSuppression.value = SessionSuppression(date = LocalDate.now())
    }

    fun extendClass(extraMinutes: Int = 5) {
        scheduleRepository.extendManualSession(extraMinutes)
        InClassService.extend(context, extraMinutes)
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
            val app = context.applicationContext as? com.classbell.app.ClassBellApp
            app?.database?.let { db ->
                com.classbell.app.core.CalendarSyncManager.syncIfEnabled(context, db, settingsRepository)
            }
        }
    }

    fun addAffair(affair: Affair) {
        viewModelScope.launch {
            val newId = affairRepository.insertAffair(affair)
            alarmScheduler.scheduleAffairAlarm(affair.copy(id = newId))
            val app = context.applicationContext as? com.classbell.app.ClassBellApp
            app?.database?.let { db ->
                com.classbell.app.core.CalendarSyncManager.syncIfEnabled(context, db, settingsRepository)
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

    fun saveShiftOverride(override: com.classbell.app.data.model.Override) {
        saveShiftOverrides(listOf(override))
    }

    fun saveShiftOverrides(overrides: List<com.classbell.app.data.model.Override>) {
        viewModelScope.launch {
            val app = context.applicationContext as? com.classbell.app.ClassBellApp
            val activeSemId = scheduleRepository.activeSemester.firstOrNull()?.id ?: 1L
            overrides.forEach { override ->
                scheduleRepository.clearOverridesForDate(activeSemId, override.date)
                scheduleRepository.insertOverride(override.copy(semesterId = activeSemId))
            }
            app?.database?.let { db ->
                com.classbell.app.core.CalendarSyncManager.syncIfEnabled(context, db, settingsRepository)
            }
        }
    }

    fun saveAffairs(affairs: List<Affair>) {
        viewModelScope.launch {
            val insertedIds = affairRepository.insertAll(affairs)
            insertedIds.forEachIndexed { index, newId ->
                val affairWithId = affairs[index].copy(id = newId)
                alarmScheduler.scheduleAffairAlarm(affairWithId)
            }
            val app = context.applicationContext as? com.classbell.app.ClassBellApp
            app?.database?.let { db ->
                com.classbell.app.core.CalendarSyncManager.syncIfEnabled(context, db, settingsRepository)
            }
        }
    }

    fun saveCourses(courses: List<Course>) {
        viewModelScope.launch {
            val activeSemId = scheduleRepository.activeSemester.firstOrNull()?.id ?: 1L
            courseRepository.insertAll(courses.map { it.copy(semesterId = activeSemId) })
            val app = context.applicationContext as? com.classbell.app.ClassBellApp
            app?.database?.let { db ->
                com.classbell.app.core.CalendarSyncManager.syncIfEnabled(context, db, settingsRepository)
            }
        }
    }

    fun saveCalendar(semester: Semester, holidays: List<com.classbell.app.data.model.Holiday>) {
        viewModelScope.launch {
            val app = context.applicationContext as? com.classbell.app.ClassBellApp
            val semId = app?.database?.semesterDao()?.insert(semester) ?: 1L
            app?.database?.holidayDao()?.insertAll(holidays.map { it.copy(semesterId = semId) })
            app?.database?.let { db ->
                com.classbell.app.core.CalendarSyncManager.syncIfEnabled(context, db, settingsRepository)
            }
        }
    }

    fun saveHolidays(holidays: List<com.classbell.app.data.model.Holiday>) {
        viewModelScope.launch {
            val activeSemId = scheduleRepository.activeSemester.firstOrNull()?.id ?: 1L
            scheduleRepository.insertHolidays(holidays.map { it.copy(semesterId = activeSemId) })
            val app = context.applicationContext as? com.classbell.app.ClassBellApp
            app?.database?.let { db ->
                com.classbell.app.core.CalendarSyncManager.syncIfEnabled(context, db, settingsRepository)
            }
        }
    }

    fun savePeriodSlots(slots: List<PeriodSlot>) {
        viewModelScope.launch {
            val app = context.applicationContext as? com.classbell.app.ClassBellApp
            app?.database?.periodSlotDao()?.deleteAll()
            app?.database?.periodSlotDao()?.insertAll(slots)
            com.classbell.app.ClassBellApp.cachedPeriodSlots = slots
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
