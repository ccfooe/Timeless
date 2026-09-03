package com.classbell.app.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.classbell.app.data.repository.ScheduleRepository
import com.classbell.app.data.repository.TeachingStats
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class StatsViewModel(
    scheduleRepository: ScheduleRepository
) : ViewModel() {

    val teachingStats: StateFlow<TeachingStats?> = scheduleRepository.getTeachingStats()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    class Factory(
        private val scheduleRepository: ScheduleRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return StatsViewModel(scheduleRepository) as T
        }
    }
}
