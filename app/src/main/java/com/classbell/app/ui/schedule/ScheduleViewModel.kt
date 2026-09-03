package com.classbell.app.ui.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.classbell.app.core.WeekCalculator
import com.classbell.app.data.model.Course
import com.classbell.app.data.model.MeetingOccurrence
import com.classbell.app.data.model.PeriodSlot
import com.classbell.app.data.model.Semester
import com.classbell.app.data.repository.CourseRepository
import com.classbell.app.data.repository.ScheduleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class ScheduleUiState(
    val activeSemester: Semester? = null,
    val selectedWeek: Int = 2,
    val currentWeekNumber: Int = 2,
    val weekDateRangeStr: String = "",
    val isTeachingWeek: Boolean = true,
    val phaseLabel: String = "教学周",
    val periodSlots: List<PeriodSlot> = emptyList(),
    val weeklyMeetings: Map<Int, List<MeetingOccurrence>> = emptyMap(),
    val selectedMeetingForDetail: MeetingOccurrence? = null,
    val isEditingCourse: Boolean = false,
    val courseToEdit: Course? = null
)

class ScheduleViewModel(
    private val scheduleRepository: ScheduleRepository,
    private val courseRepository: CourseRepository
) : ViewModel() {

    private val _selectedWeek = MutableStateFlow(2)
    val selectedWeek = _selectedWeek.asStateFlow()

    private val _selectedMeetingForDetail = MutableStateFlow<MeetingOccurrence?>(null)
    val selectedMeetingForDetail = _selectedMeetingForDetail.asStateFlow()

    private val _isEditingCourse = MutableStateFlow(false)
    val isEditingCourse = _isEditingCourse.asStateFlow()

    private val _courseToEdit = MutableStateFlow<Course?>(null)
    val courseToEdit = _courseToEdit.asStateFlow()

    init {
        viewModelScope.launch {
            scheduleRepository.activeSemester.collect { semester ->
                if (semester != null) {
                    val currentWeek = WeekCalculator.calculateWeekNumber(LocalDate.now(), semester.week1Monday)
                    _selectedWeek.value = currentWeek
                }
            }
        }
    }

    val uiState: StateFlow<ScheduleUiState> = combine(
        scheduleRepository.activeSemester,
        _selectedWeek,
        scheduleRepository.periodSlots,
        _selectedMeetingForDetail,
        _isEditingCourse,
        _courseToEdit
    ) { args: Array<Any?> ->
        val semester = args[0] as Semester?
        val week = args[1] as Int
        @Suppress("UNCHECKED_CAST")
        val periods = args[2] as List<PeriodSlot>
        val detail = args[3] as MeetingOccurrence?
        val isEditing = args[4] as Boolean
        val editCourse = args[5] as Course?

        val currentWeek = if (semester != null) {
            WeekCalculator.calculateWeekNumber(LocalDate.now(), semester.week1Monday)
        } else 1

        val dateRange = if (semester != null) {
            val (mon, sun) = WeekCalculator.getWeekDateRange(week, semester.week1Monday)
            "${mon.monthValue}/${mon.dayOfMonth} - ${sun.monthValue}/${sun.dayOfMonth}"
        } else ""

        val isTeaching = semester == null || week in semester.teachingFrom..semester.teachingTo
        val phase = if (semester != null) {
            WeekCalculator.getSemesterPhase(
                week,
                semester.teachingFrom,
                semester.teachingTo,
                semester.examFrom,
                semester.examTo,
                semester.practiceFrom,
                semester.practiceTo
            )
        } else "教学周"

        ScheduleUiState(
            activeSemester = semester,
            selectedWeek = week,
            currentWeekNumber = currentWeek,
            weekDateRangeStr = dateRange,
            isTeachingWeek = isTeaching,
            phaseLabel = phase,
            periodSlots = periods,
            selectedMeetingForDetail = detail,
            isEditingCourse = isEditing,
            courseToEdit = editCourse
        )
    }.flatMapLatest { baseState ->
        scheduleRepository.getWeeklyMeetings(baseState.selectedWeek).combine(
            MutableStateFlow(baseState)
        ) { meetingsMap, state ->
            state.copy(weeklyMeetings = meetingsMap)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ScheduleUiState()
    )

    fun selectWeek(week: Int) {
        if (week in 1..25) {
            _selectedWeek.value = week
        }
    }

    fun nextWeek() {
        if (_selectedWeek.value < 25) {
            _selectedWeek.value += 1
        }
    }

    fun previousWeek() {
        if (_selectedWeek.value > 1) {
            _selectedWeek.value -= 1
        }
    }

    fun openCourseDetail(meeting: MeetingOccurrence) {
        _selectedMeetingForDetail.value = meeting
    }

    fun closeCourseDetail() {
        _selectedMeetingForDetail.value = null
    }

    fun openAddCourse() {
        _courseToEdit.value = null
        _isEditingCourse.value = true
    }

    fun openEditCourse(course: Course) {
        _selectedMeetingForDetail.value = null
        _courseToEdit.value = course
        _isEditingCourse.value = true
    }

    fun closeEditCourse() {
        _isEditingCourse.value = false
        _courseToEdit.value = null
    }

    fun saveCourse(course: Course) {
        viewModelScope.launch {
            if (course.id == 0L) {
                courseRepository.insertCourse(course)
            } else {
                courseRepository.updateCourse(course)
            }
            closeEditCourse()
        }
    }

    fun deleteCourse(course: Course) {
        viewModelScope.launch {
            courseRepository.deleteCourse(course)
            closeCourseDetail()
        }
    }

    class Factory(
        private val scheduleRepository: ScheduleRepository,
        private val courseRepository: CourseRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ScheduleViewModel(scheduleRepository, courseRepository) as T
        }
    }
}
