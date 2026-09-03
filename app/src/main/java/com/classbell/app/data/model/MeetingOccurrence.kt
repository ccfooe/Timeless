package com.classbell.app.data.model

import java.time.LocalDate
import java.time.LocalTime

enum class MeetingStatus {
    IN_PROGRESS, // 进行中
    UPCOMING,    // 未开始
    FINISHED,    // 已结束
    HOLIDAY      // 放假停课
}

data class MeetingOccurrence(
    val course: Course,
    val date: LocalDate,
    val periodStart: Int,
    val periodEnd: Int,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val isHolidaySuppressed: Boolean = false,
    val holidayName: String? = null,
    val status: MeetingStatus = MeetingStatus.UPCOMING,
    val progress: Float = 0f,
    val remainingMinutes: Long = 0L
)
